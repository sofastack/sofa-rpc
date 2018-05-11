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
package com.alipay.sofa.rpc.server.bolt;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RejectedTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {

        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(0).setPort(22222)
            .setQueues(0).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(1000))
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setTimeout(3000)
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setRegister(false);

        final HelloService helloService = consumerConfig.refer();

        final AtomicInteger success = new AtomicInteger();
        final AtomicInteger failure = new AtomicInteger();

        int times = 3;
        final CountDownLatch latch = new CountDownLatch(times);
        for (int i = 0; i < times; i++) {
            Thread thread1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        helloService.sayHello("xxx", 22);
                        success.incrementAndGet();
                    } catch (Exception e) {
                        if (e instanceof SofaRpcException) {
                            Assert.assertEquals(((SofaRpcException) e).getErrorType(), RpcErrorType.SERVER_BUSY);
                        }
                        failure.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            }, "T1");
            thread1.start();
        }

        try {
            latch.await(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
        }
        Assert.assertEquals(success.get(), 2);
        Assert.assertEquals(failure.get(), 1);

    }
}
