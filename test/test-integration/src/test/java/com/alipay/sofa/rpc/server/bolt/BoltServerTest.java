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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
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
public class BoltServerTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {
        badClose();
        RpcRuntimeContext.destroy();
        wellClose();
    }

    public void badClose() {
        // 只有2个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22222)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(1000))
            .setServer(serverConfig)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(30000)
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int times = 5;
        final CountDownLatch latch = new CountDownLatch(times);
        final AtomicInteger count = new AtomicInteger();

        // 瞬间发起5个请求，那么服务端肯定在排队
        for (int i = 0; i < times; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        LOGGER.info(helloService.sayHello("xxx", 22));
                        count.incrementAndGet();
                    } catch (Exception e) {
                        LOGGER.info(e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            }, "thread" + i);
            thread.start();
            LOGGER.info("send " + i);
            try {
                Thread.sleep(100);
            } catch (Exception ignore) {
            }
        }

        // 然后马上关闭服务端，此时应该5个请求都还没执行完
        try {
            serverConfig.destroy();
        } catch (Exception e) {
        }

        // 应该执行了0个请求
        Assert.assertTrue(count.get() == 0);
    }

    public void wellClose() {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(22222)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(1000))
            .setServer(serverConfig)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(30000)
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int times = 5;
        final CountDownLatch latch = new CountDownLatch(times);
        final AtomicInteger count = new AtomicInteger();

        // 瞬间发起5个请求，那么服务端肯定在排队
        for (int i = 0; i < times; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        LOGGER.info(helloService.sayHello("xxx", 22));
                        count.incrementAndGet();
                    } catch (Exception e) {
                        // TODO
                    } finally {
                        latch.countDown();
                    }
                }
            }, "thread" + i);
            thread.start();
            LOGGER.info("send " + i);
            try {
                Thread.sleep(100);
            } catch (Exception ignore) {
            }
        }

        // 然后马上关闭服务端，此时应该5个请求都还没执行完
        try {
            serverConfig.destroy();
        } catch (Exception e) {
        }

        // 应该执行了5个请求
        try {
            // 服务端完成最后一个请求，但是由于睡了一定时间，所以这样等待最后一个客户端响应返回
            latch.await(1500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        Assert.assertTrue(count.get() == times);

    }
}