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
package com.alipay.sofa.rpc.test.rpcctx;

import com.alipay.sofa.rpc.api.context.RpcContextManager;
import com.alipay.sofa.rpc.api.context.RpcReferenceContext;
import com.alipay.sofa.rpc.api.context.RpcServiceContext;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RpcContextTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setPort(22222)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        CtxHelloServiceImpl helloServiceImpl = new CtxHelloServiceImpl();
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setRef(helloServiceImpl)
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig.export();

        {
            ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setApplication(new ApplicationConfig().setAppName("test-client"))
                .setDirectUrl("bolt://127.0.0.1:22222?appName=test-server")
                .setTimeout(30000)
                .setRegister(false);
            final HelloService helloService = consumerConfig.refer();

            String str = helloService.sayHello("xxx", 123);

            RpcServiceContext serviceContext = RpcContextManager.currentServiceContext(false);
            RpcReferenceContext referenceContext = RpcContextManager.lastReferenceContext(false);
            Assert.assertNull(serviceContext);
            Assert.assertNotNull(referenceContext);
            serviceContext = helloServiceImpl.serviceContext;
            Assert.assertNotNull(serviceContext);

            Assert.assertEquals(serviceContext.getCallerAppName(), "test-client");
            Assert.assertEquals(referenceContext.getTargetAppName(), "test-server");
            Assert.assertNotNull(referenceContext.getClientIP());
            Assert.assertTrue(referenceContext.getClientPort() > 0);
        }
        {
            final CountDownLatch latch = new CountDownLatch(1);
            final String[] ret = { null };
            ConsumerConfig<HelloService> consumerConfig2 = new ConsumerConfig<HelloService>()
                .setInterfaceId(HelloService.class.getName())
                .setApplication(new ApplicationConfig().setAppName("test-client"))
                .setDirectUrl("bolt://127.0.0.1:22222?appName=test-server")
                .setTimeout(2000)
                .setInvokeType("callback")
                .setOnReturn(new SofaResponseCallback() {
                    @Override
                    public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                        ret[0] = (String) appResponse;
                        latch.countDown();
                    }

                    @Override
                    public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                        latch.countDown();
                    }

                    @Override
                    public void onSofaException(SofaRpcException sofaException, String methodName,
                                                RequestBase request) {
                        latch.countDown();
                    }
                })
                .setRegister(false);
            final HelloService helloServiceCallback = consumerConfig2.refer();

            String ret0 = helloServiceCallback.sayHello("xxx", 22);
            Assert.assertNull(ret0); // 第一次返回null

            RpcServiceContext serviceContext = RpcContextManager.currentServiceContext(false);
            RpcReferenceContext referenceContext = RpcContextManager.lastReferenceContext(false);
            Assert.assertNull(serviceContext);
            Assert.assertNotNull(referenceContext);
            serviceContext = helloServiceImpl.serviceContext;
            Assert.assertNotNull(serviceContext);

            Assert.assertEquals(serviceContext.getCallerAppName(), "test-client");
            Assert.assertEquals(referenceContext.getTargetAppName(), "test-server");
            Assert.assertNotNull(referenceContext.getClientIP());
            Assert.assertTrue(referenceContext.getClientPort() > 0);

            try {
                latch.await(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignore) {
            }
            Assert.assertNotNull(ret[0]);

        }
    }
}
