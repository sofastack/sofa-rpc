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
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class FailFastClusterTest extends ActivelyDestroyTest {
    @Test
    public void testSingleServer() {

        // 只有2个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22222)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl() {
                AtomicInteger cnt = new AtomicInteger();

                @Override
                public String sayHello(String name, int age) {
                    if (cnt.getAndIncrement() % 3 != 0) {
                        try {
                            Thread.sleep(2000);
                        } catch (Exception ignore) {
                        }
                    }
                    LOGGER.info("xxxxxxxxxxxxxxxxx" + age);
                    return "hello " + name + " from server! age: " + age;
                }
            })
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig.export();

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(1000)
            .setCluster("failfast")
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int count1 = 0;
        for (int i = 0; i < 5; i++) {
            try {
                helloService.sayHello("xxx", 20 + i);
                count1++;
            } catch (Exception ignore) {
            }
        }
        Assert.assertEquals(count1, 2);

        ConsumerConfig<HelloService> consumerConfig2 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(1000)
            .setCluster("failfast")
            .setRegister(false);
        final HelloService helloService2 = consumerConfig2.refer();
        int count2 = 0;
        for (int i = 0; i < 5; i++) {
            try {
                helloService2.sayHello("xxx", 22);
                count2++;
            } catch (Exception ignore) {
            }
        }
        Assert.assertEquals(count2, 2);
    }

    @Test
    public void testMultiServer() {

        // 发布一个服务，每个请求要执行2秒
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22223)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(2000))
            .setServer(serverConfig)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig.export();

        // 再发布一个服务，不等待
        ServerConfig serverConfig2 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22224)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig2)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig2.export();

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22223;bolt://127.0.0.1:22224")
            .setTimeout(1000)
            .setCluster("failover")
            .setLoadBalancer("roundRobin") // 轮询
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int count2 = 0;
        for (int i = 0; i < 5; i++) {
            try {
                helloService.sayHello("xxx", 22);
                count2++;
            } catch (Exception ignore) {
            }
        }
        Assert.assertTrue(count2 < 5); // 总有失败的
    }

    @After
    public void stopServer() {
        RpcRuntimeContext.destroy();
    }

}