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

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class HystrixFilterSyncTest extends ActivelyDestroyTest {

    @Test
    public void testSuccess() {
        ProviderConfig<HelloService> providerConfig = defaultServer(0);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = defaultClient();

        HelloService helloService = consumerConfig.refer();

        try {
            String result = helloService.sayHello("abc", 24);
            Assert.assertEquals("hello abc from server! age: 24", result);
        } finally {
            providerConfig.unExport();
            consumerConfig.unRefer();
        }
    }

    @Test
    public void testHystrixTimeout() {
        ProviderConfig<HelloService> providerConfig = defaultServer(2000);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = defaultClient()
            .setTimeout(10000);

        HelloService helloService = consumerConfig.refer();

        try {
            helloService.sayHello("abc", 24);
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
    public void testHystrixFallback() {
        ProviderConfig<HelloService> providerConfig = defaultServer(2000);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = defaultClient()
            .setTimeout(10000)
            .setFilterRef(Collections.<Filter> singletonList(new HystrixFilter()));

        SofaHystrixConfig.registerFallback(consumerConfig, new HelloServiceFallback());

        HelloService helloService = consumerConfig.refer();

        try {
            String result = helloService.sayHello("abc", 24);
            Assert.assertEquals("fallback abc from server! age: 24", result);
        } finally {
            providerConfig.unExport();
            consumerConfig.unRefer();
        }
    }

    @Test
    public void testHystrixFallbackFactory() {
        ProviderConfig<HelloService> providerConfig = defaultServer(2000);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = defaultClient()
            .setTimeout(10000);

        SofaHystrixConfig.registerFallbackFactory(consumerConfig, new HelloServiceFallbackFactory());

        HelloService helloService = consumerConfig.refer();

        try {
            String result = helloService.sayHello("abc", 24);
            Assert.assertEquals(
                "fallback abc from server! age: 24, error: com.netflix.hystrix.exception.HystrixTimeoutException",
                result);
        } finally {
            providerConfig.unExport();
            consumerConfig.unRefer();
        }
    }

    private ProviderConfig<HelloService> defaultServer(int sleep) {
        ServerConfig serverConfig = new ServerConfig()
            .setPort(22222)
            .setDaemon(false);

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(sleep))
            .setServer(serverConfig);

        return providerConfig;
    }

    private ConsumerConfig<HelloService> defaultClient() {
        return new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setParameter(HystrixConstants.SOFA_HYSTRIX_ENABLED, String.valueOf(true));
    }
}
