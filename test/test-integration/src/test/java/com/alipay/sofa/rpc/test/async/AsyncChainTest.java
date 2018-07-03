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
package com.alipay.sofa.rpc.test.async;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class AsyncChainTest extends ActivelyDestroyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncChainTest.class);

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
        ConsumerConfig<HelloService> BConsumer = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK)
            // .setOnReturn() // 不设置 调用级别设置
            .setTimeout(3000)
            .setDirectUrl("bolt://127.0.0.1:22222");
        HelloService helloService = BConsumer.refer();

        // B服务的服务端
        ServerConfig serverConfig3 = new ServerConfig()
            .setPort(22223)
            .setDaemon(false);
        ProviderConfig<AsyncHelloService> BProvider = new ProviderConfig<AsyncHelloService>()
            .setInterfaceId(AsyncHelloService.class.getName())
            .setRef(new AsyncHelloServiceImpl(helloService))
            .setServer(serverConfig3);
        BProvider.export();

        // A调B的客户端
        ConsumerConfig<AsyncHelloService> AConsumer = new ConsumerConfig<AsyncHelloService>()
            .setInterfaceId(AsyncHelloService.class.getName())
            .setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK)
            .setTimeout(3000)
            .setDirectUrl("bolt://127.0.0.1:22223");
        AsyncHelloService asyncHelloService = AConsumer.refer();

        final CountDownLatch[] latch = new CountDownLatch[1];
        latch[0] = new CountDownLatch(1);
        final Object[] ret = new Object[1];

        // 链路异步化调用--正常
        RpcInvokeContext.getContext().setResponseCallback(buildCallback(ret, latch));
        String ret0 = asyncHelloService.sayHello("xxx", 22);
        Assert.assertNull(ret0); // 第一次返回null
        try {
            latch[0].await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
        }
        Assert.assertTrue(ret[0] instanceof String);

        // 链路异步化调用--业务异常
        ret[0] = null;
        latch[0] = new CountDownLatch(1);
        RpcInvokeContext.getContext().setResponseCallback(buildCallback(ret, latch));
        ret0 = asyncHelloService.appException("xxx");
        Assert.assertNull(ret0); // 第一次返回null
        try {
            latch[0].await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
        }
        Assert.assertTrue(ret[0] instanceof RuntimeException);

        // 链路异步化调用--rpc异常
        ret[0] = null;
        latch[0] = new CountDownLatch(1);
        RpcInvokeContext.getContext().setResponseCallback(buildCallback(ret, latch));
        ret0 = asyncHelloService.rpcException("xxx");
        Assert.assertNull(ret0); // 第一次返回null
        try {
            latch[0].await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
        }
        Assert.assertTrue(ret[0] instanceof SofaRpcException);
        Assert.assertTrue(((SofaRpcException) ret[0]).getMessage().contains("bbb"));

        // 非链路异步化调用--普通
        ConsumerConfig<AsyncHelloService> AConsumer2 = new ConsumerConfig<AsyncHelloService>()
            .setInterfaceId(AsyncHelloService.class.getName())
            .setTimeout(3000)
            .setDirectUrl("bolt://127.0.0.1:22223");
        AsyncHelloService syncHelloService = AConsumer2.refer();

        String s2 = syncHelloService.sayHello("yyy", 22);
        Assert.assertNotNull(s2);
    }

    private SofaResponseCallback buildCallback(final Object[] ret, final CountDownLatch[] latch) {
        return new SofaResponseCallback() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                LOGGER.info("A get result: {}", appResponse);
                ret[0] = appResponse;
                latch[0].countDown();
            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                LOGGER.info("A get app exception: ", throwable);
                ret[0] = throwable;
                latch[0].countDown();
            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName,
                                        RequestBase request) {
                LOGGER.info("A get sofa exception: ", sofaException);
                ret[0] = sofaException;
                latch[0].countDown();
            }
        };
    }
}
