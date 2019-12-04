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

import com.alipay.sofa.rpc.client.Cluster;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class FailoverClusterTest extends ActivelyDestroyTest {

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
            .setCluster("failover")
            .setTimeout(1000)
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
        Assert.assertEquals(count1, 2);

        ConsumerConfig<HelloService> consumerConfig2 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(1000)
            .setCluster("failover")
            .setRetries(2) // 失败后自动重试2次
            .setRegister(false);
        final HelloService helloService2 = consumerConfig2.refer();
        int count2 = 0;
        for (int i = 0; i < 4; i++) {
            try {
                helloService2.sayHello("xxx", 22);
                count2++;
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
        Assert.assertEquals(4, count2);

        Cluster cluster = consumerConfig2.getConsumerBootstrap().getCluster();
        Assert.assertTrue(cluster.isAvailable());
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
            .setRetries(1) // 失败后重试一次
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int count2 = 0;
        for (int i = 0; i < 4; i++) {
            try {
                helloService.sayHello("xxx", 22);
                count2++;
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
        Assert.assertEquals(4, count2);
    }

    @Test
    public void testPinpoint() {

        // 发布一个服务，每个请求要执行2秒
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22225)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl("55"))
            .setServer(serverConfig)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig.export();

        // 再发布一个服务，不等待
        ServerConfig serverConfig2 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22226)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl("66"))
            .setServer(serverConfig2)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig2.export();

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22225;bolt://127.0.0.1:22226")
            .setTimeout(1000)
            .setCluster("failover")
            .setLoadBalancer("random")
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int count2 = 0;
        for (int i = 0; i < 10; i++) {
            try {
                RpcInvokeContext.getContext().setTargetURL("127.0.0.1:22225");
                Assert.assertEquals("55", helloService.sayHello("xxx", 22));
                count2++;
            } catch (Exception ignore) {
            }
        }
        Assert.assertEquals(count2, 10);

        count2 = 0;
        for (int i = 0; i < 10; i++) {
            try {
                RpcInvokeContext.getContext().setTargetURL("127.0.0.1:22226");
                Assert.assertEquals("66", helloService.sayHello("xxx", 22));
                count2++;
            } catch (Exception ignore) {
            }
        }
        Assert.assertEquals(count2, 10);

        boolean error = false;
        try {
            RpcInvokeContext.getContext().setTargetURL("127.0.0.1:22227");
            Assert.assertEquals("66", helloService.sayHello("xxx", 22));
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void testStick() {

        // 发布一个服务，每个请求要执行2秒
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22227)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl("77"))
            .setServer(serverConfig)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig.export();

        // 再发布一个服务，不等待
        ServerConfig serverConfig2 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22228)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl("88"))
            .setServer(serverConfig2)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig2.export();

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22227;bolt://127.0.0.1:22228")
            .setTimeout(1000)
            .setCluster("failover")
            .setLoadBalancer("random")
            .setSticky(true)
            .setRegister(false);
        final HelloService helloService = consumerConfig.refer();

        int count2 = 0;
        String result = helloService.sayHello("xxx", 22);
        for (int i = 0; i < 10; i++) {
            try {
                Assert.assertEquals(result, helloService.sayHello("xxx", 22));
                count2++;
            } catch (Exception ignore) {
            }
        }
        Assert.assertEquals(count2, 10);

        String nextResult;
        if ("77".equals(result)) {
            serverConfig.destroy();
            nextResult = "88";
        } else {
            serverConfig2.destroy();
            nextResult = "77";
        }
        try {
            Assert.assertEquals(nextResult, helloService.sayHello("xxx", 22));
            count2++;
        } catch (Exception e) {
        }
        Assert.assertEquals(count2, 10);

    }

    @Test
    public void testRpcDirectInvokeFromContext() {

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(13900);

        ProviderConfig<HelloService> provider = new ProviderConfig();
        provider.setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl("x-demo-invoke"))
            .setApplication(new ApplicationConfig().setAppName("x-test-server"))
            .setProxy("javassist")
            .setSerialization("hessian2")
            .setServer(serverConfig)
            .setTimeout(3000);

        provider.export();

        ConsumerConfig<HelloService> consumer = new ConsumerConfig();
        consumer.setInterfaceId(HelloService.class.getName())
            .setApplication(new ApplicationConfig().setAppName("x-test-client"))
            .setProxy("javassist");

        HelloService proxy = consumer.refer();

        for (int i = 0; i < 3; i++) {
            RpcInvokeContext.getContext().setTargetURL("127.0.0.1:13900");
            Assert.assertEquals("x-demo-invoke", proxy.sayHello("x-demo-invoke", 1));
        }

        provider.unExport();
        consumer.unRefer();
    }

    @Test
    public void testRpcDirectInvokeFromContextWithAvailableProviders() {

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(13900);

        ProviderConfig<HelloService> provider = new ProviderConfig();
        provider.setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl("x-demo-invoke"))
            .setApplication(new ApplicationConfig().setAppName("x-test-server"))
            .setProxy("javassist")
            .setSerialization("hessian2")
            .setServer(serverConfig)
            .setTimeout(3000);

        provider.export();

        ConsumerConfig<HelloService> consumer = new ConsumerConfig();
        consumer.setInterfaceId(HelloService.class.getName())
            .setApplication(new ApplicationConfig().setAppName("x-test-client"))
            // 模拟有可用服务
            .setDirectUrl("bolt://127.0.0.1:65534")
            .setProxy("javassist");

        HelloService proxy = consumer.refer();

        for (int i = 0; i < 3; i++) {
            RpcInvokeContext.getContext().setTargetURL("127.0.0.1:13900");
            Assert.assertEquals("x-demo-invoke", proxy.sayHello("x-demo-invoke", 1));
        }

        provider.unExport();
        consumer.unRefer();
    }

    @Test(expected = SofaRouteException.class)
    public void testRpcDirectInvokeFromContextNotAllowed() {

        boolean prev = RpcConfigs.getBooleanValue(RpcOptions.RPC_CREATE_CONN_WHEN_ABSENT);

        // disable create connection from context
        RpcConfigs.putValue(RpcOptions.RPC_CREATE_CONN_WHEN_ABSENT, false);

        try {
            ServerConfig serverConfig = new ServerConfig()
                .setProtocol("bolt")
                .setHost("0.0.0.0")
                .setPort(13900);

            ProviderConfig<HelloService> provider = new ProviderConfig();
            provider.setInterfaceId(HelloService.class.getName())
                .setRef(new HelloServiceImpl("x-demo-invoke"))
                .setApplication(new ApplicationConfig().setAppName("x-test-server"))
                .setProxy("javassist")
                .setSerialization("hessian2")
                .setServer(serverConfig)
                .setTimeout(3000);

            provider.export();

            ConsumerConfig<HelloService> consumer = new ConsumerConfig();
            consumer.setInterfaceId(HelloService.class.getName())
                .setApplication(new ApplicationConfig().setAppName("x-test-client"))
                .setProxy("javassist");

            HelloService proxy = consumer.refer();

            RpcInvokeContext.getContext().setTargetURL("127.0.0.1:13900");
            proxy.sayHello("x-demo-invoke", 1);

            provider.unExport();
            consumer.unRefer();
        } finally {
            RpcConfigs.putValue(RpcOptions.RPC_CREATE_CONN_WHEN_ABSENT, prev);
        }
    }

    @After
    public void stopServer() {
        RpcRuntimeContext.destroy();
    }
}
