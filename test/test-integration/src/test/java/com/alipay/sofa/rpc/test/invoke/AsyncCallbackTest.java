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
package com.alipay.sofa.rpc.test.invoke;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class AsyncCallbackTest extends ActivelyDestroyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncCallbackTest.class);

    @Test
    public void testAll() {

        ServerConfig serverConfig2 = new ServerConfig()
            .setPort(22222)
            .setDaemon(false);

        // C服务的服务端
        ProviderConfig<HelloService> CProvider = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(1000))
            .setServer(serverConfig2);
        CProvider.export();

        // B调C的客户端
        Filter filter = new TestAsyncFilter();
        ConsumerConfig<HelloService> BConsumer = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK)
            .setTimeout(50000)
            .setFilterRef(Arrays.asList(filter))
            // .setOnReturn() // 不设置 调用级别设置
            .setDirectUrl("bolt://127.0.0.1:22222");
        HelloService helloService = BConsumer.refer();

        final CountDownLatch latch = new CountDownLatch(1);
        final String[] ret = { null };

        RpcInvokeContext.getContext().setResponseCallback(new SofaResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                LOGGER.info("B get result: {}", appResponse);
                ret[0] = (String) appResponse;
                latch.countDown();
            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                LOGGER.info("B get app exception: {}", throwable);
                latch.countDown();
            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName,
                                        RequestBase request) {
                LOGGER.info("B get sofa exception: {}", sofaException);
                latch.countDown();
            }
        });

        String ret0 = helloService.sayHello("xxx", 22);
        Assert.assertNull(ret0); // 第一次返回null

        try {
            latch.await(60000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
        }

        Assert.assertNotNull(ret[0]);
        // 过滤器生效
        Assert.assertTrue(ret[0].endsWith("append by async filter"));

        RpcInvokeContext.removeContext();
    }
}
