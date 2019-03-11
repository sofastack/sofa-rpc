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
package com.alipay.sofa.rpc.test.client;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class LazyConnectTest extends ActivelyDestroyTest {

    public static ServerConfig serverConfig;

    @BeforeClass
    public static void startServer() {

        RpcRunningState.setUnitTestMode(true);
        // 只有2个线程 执行
        serverConfig = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22222)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig.export();
    }

    @Test
    public void testLazyNone() {

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("")
            .setTimeout(1000)
            .setLazy(true)
            .setRepeatedReferLimit(-1)
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int count1 = 0;
        for (int i = 0; i < 4; i++) {
            try {
                helloService.sayHello("xxx", 20 + i);
                count1++;
            } catch (Exception ignore) {
            }
        }
        Assert.assertEquals(count1, 0);
    }

    @Test
    public void testLazy() {

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(1000)
            .setLazy(true)
            .setRepeatedReferLimit(-1)
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int count1 = 0;
        for (int i = 0; i < 4; i++) {
            try {
                helloService.sayHello("xxx", 20 + i);
                count1++;
            } catch (Exception ignore) {
            }
        }
        Assert.assertEquals(count1, 4);
    }

    @Test
    public void testLazyFail() {

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22223")
            .setTimeout(1000)
            .setLazy(true)
            .setRepeatedReferLimit(-1)
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int count1 = 0;
        for (int i = 0; i < 4; i++) {
            try {
                helloService.sayHello("xxx", 20 + i);
                count1++;
            } catch (Exception ignore) {
            }
        }
        Assert.assertEquals(count1, 0);
    }

    @Test
    public void testLazyWhenMultipleThreads() {

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(1000)
            .setLazy(true)
            .setRepeatedReferLimit(-1)
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int threads = 10;
        final AtomicInteger count1 = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            try {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int j = 0; j < 10; j++) {
                            try {
                                helloService.sayHello("xxx", 20);
                                count1.incrementAndGet();
                            } catch (Exception ignore) {
                            }
                        }
                        latch.countDown();
                    }
                }, "thread" + i);
                thread.start();

            } catch (Exception ignore) {
            }
        }
        try {
            latch.await(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {

        }
        Assert.assertEquals(count1.get(), 100);
    }

    @Test
    public void testLazyFailWhenMultipleThreads() {

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22223")
            .setTimeout(1000)
            .setLazy(true)
            .setRepeatedReferLimit(-1)
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int threads = 10;
        final AtomicInteger count1 = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            try {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int j = 0; j < 10; j++) {
                            try {
                                helloService.sayHello("xxx", 20);
                                count1.incrementAndGet();
                            } catch (Exception ignore) {
                            }
                        }
                        latch.countDown();
                    }
                }, "thread" + i);
                thread.start();

            } catch (Exception ignore) {
            }
        }
        try {
            latch.await(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {

        }
        Assert.assertEquals(count1.get(), 0);
    }
}
