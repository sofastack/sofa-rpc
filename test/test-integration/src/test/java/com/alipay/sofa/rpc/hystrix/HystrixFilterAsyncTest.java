/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.hystrix;

import com.alipay.sofa.rpc.api.future.SofaResponseFuture;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class HystrixFilterAsyncTest extends ActivelyDestroyTest {

    @Test
    public void testSuccess() throws InterruptedException {
        ProviderConfig<HelloService> providerConfig = defaultServer(0, false);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = defaultClient();

        HelloService helloService = consumerConfig.refer();

        try {
            helloService.sayHello("abc", 24);
            String result = (String) SofaResponseFuture.getResponse(10000, true);
            Assert.assertEquals("hello abc from server! age: 24", result);
        } finally {
            providerConfig.unExport();
            consumerConfig.unRefer();
        }
    }

    @Test
    public void testHystrixTimeout() {
        ProviderConfig<HelloService> providerConfig = defaultServer(2000, false);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = defaultClient()
            .setTimeout(10000);

        HelloService helloService = consumerConfig.refer();

        try {
            helloService.sayHello("abc", 24);
            String result = (String) SofaResponseFuture.getResponse(10000, true);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcException);
            Assert.assertTrue(e.getCause() instanceof HystrixRuntimeException);
        } finally {
            providerConfig.unExport();
            consumerConfig.unRefer();
        }
    }

    @Test
    public void testHystrixFallback() throws InterruptedException {
        ProviderConfig<HelloService> providerConfig = defaultServer(2000, false);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = defaultClient()
            .setTimeout(10000);

        SofaHystrixConfig.registerFallback(consumerConfig, new HelloServiceFallback());

        HelloService helloService = consumerConfig.refer();

        try {
            helloService.sayHello("abc", 24);
            String result = (String) SofaResponseFuture.getResponse(10000, true);
            Assert.assertEquals("fallback abc from server! age: 24", result);
        } finally {
            providerConfig.unExport();
            consumerConfig.unRefer();
        }
    }

    @Test
    public void testHystrixFallbackFactory() throws InterruptedException {
        ProviderConfig<HelloService> providerConfig = defaultServer(2000, false);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = defaultClient()
            .setTimeout(10000);

        SofaHystrixConfig.registerFallbackFactory(consumerConfig, new HelloServiceFallbackFactory());

        HelloService helloService = consumerConfig.refer();

        try {
            helloService.sayHello("abc", 24);
            String result = (String) SofaResponseFuture.getResponse(10000, true);
            Assert.assertEquals(
                "fallback abc from server! age: 24, error: com.netflix.hystrix.exception.HystrixTimeoutException",
                result);
        } finally {
            providerConfig.unExport();
            consumerConfig.unRefer();
        }
    }

    @Test
    public void testHystrixCircuitBreakerFallback() throws InterruptedException {
        // 强制开启熔断
        SetterFactory setterFactory = new SetterFactory() {

            private DefaultSetterFactory defaultSetterFactory = new DefaultSetterFactory();

            @Override
            public HystrixCommand.Setter createSetter(FilterInvoker invoker, SofaRequest request) {
                return defaultSetterFactory.createSetter(invoker, request);
            }

            @Override
            public HystrixObservableCommand.Setter createObservableSetter(FilterInvoker invoker, SofaRequest request) {
                String groupKey = invoker.getConfig().getInterfaceId();
                String commandKey = request.getMethodName() + "_invoke_failed";
                return HystrixObservableCommand.Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                    .andCommandPropertiesDefaults(
                        HystrixCommandProperties.defaultSetter().withCircuitBreakerForceOpen(true));
            }
        };

        ProviderConfig<HelloService> providerConfig = defaultServer(0, true);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = defaultClient()
            .setTimeout(10000);

        SofaHystrixConfig.registerFallbackFactory(consumerConfig, new HelloServiceFallbackFactory());
        SofaHystrixConfig.registerSetterFactory(consumerConfig, setterFactory);

        HelloService helloService = consumerConfig.refer();

        try {
            for (int i = 0; i < 20; i++) {
                helloService.sayHello("abc", 24);
                String result = (String) SofaResponseFuture.getResponse(10000, true);
                Assert.assertEquals(
                    "fallback abc from server! age: 24, error: java.lang.RuntimeException",
                    result);
            }
            // 熔断时服务端不应该接收到任何请求
            Assert.assertEquals(((InvokeFailedHelloService) providerConfig.getRef()).getExecuteCount(), 0);
        } finally {
            providerConfig.unExport();
            consumerConfig.unRefer();
        }
    }

    private ProviderConfig<HelloService> defaultServer(int sleep, boolean error) {
        HelloService helloService = error ?
            new InvokeFailedHelloService() :
            new HelloServiceImpl(sleep);

        ServerConfig serverConfig = new ServerConfig()
            .setPort(22222)
            .setDaemon(false);

        return new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(helloService)
            .setServer(serverConfig);
    }

    private ConsumerConfig<HelloService> defaultClient() {
        return new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE)
            .setParameter(HystrixConstants.SOFA_HYSTRIX_ENABLED, String.valueOf(true));
    }
}
