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
package com.alipay.sofa.rpc.test.bootstrap.bolt;

import com.alipay.sofa.rpc.client.Cluster;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.proxy.jdk.JDKProxy;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltConsumerBootstrapTest extends ActivelyDestroyTest {
    @Test
    public void refer() throws Exception {

        ConsumerConfig<HelloService> consumerConfig0 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22221")
            .setTimeout(3000);
        consumerConfig0.refer();

        ConsumerConfig<HelloService> consumerConfig1 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22221")
            .setTimeout(3000);
        consumerConfig1.refer();

        ConsumerConfig<HelloService> consumerConfig2 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22221")
            .setTimeout(3000);
        consumerConfig2.refer();

        ConsumerConfig<HelloService> consumerConfig3 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22221")
            .setTimeout(3000);
        try {
            consumerConfig3.refer();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }

        ConsumerConfig<HelloService> consumerConfig4 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22221")
            .setRepeatedReferLimit(4)
            .setTimeout(3000);
        consumerConfig4.refer();

        ConsumerConfig<HelloService> consumerConfig5 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22221")
            .setRepeatedReferLimit(4)
            .setTimeout(3000);
        try {
            consumerConfig5.refer();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }
    }

    @Test
    public void testSubscribe() throws Exception {
        RegistryConfig registryConfig = new RegistryConfig().setProtocol("mocktest");
        RegistryConfig registryConfig2 = new RegistryConfig().setProtocol("mocktestslow");

        ConsumerConfig<HelloService> consumerConfig3 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("mock")
            .setRegistry(Arrays.asList(registryConfig, registryConfig2))
            .setTimeout(3000).setAddressWait(-1);
        long start = System.currentTimeMillis();
        consumerConfig3.refer();
        long end = System.currentTimeMillis();
        System.out.println("elapsed time " + (end - start) + "ms");
        Assert.assertTrue((end - start) > 2000 && (end - start) < 4000);
        Assert.assertTrue(consumerConfig3.getConsumerBootstrap().isSubscribed());
        Assert.assertTrue(consumerConfig3.getConsumerBootstrap().getCluster()
            .getAddressHolder().getAllProviderSize() == 0);

        // 发布一个服务，每个请求要执行2秒
        ServerConfig serverConfig0 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22222)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig0 = new ProviderConfig<HelloService>()
            .setId("p-0")
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("mock")
            .setRef(new HelloServiceImpl(2000))
            .setServer(serverConfig0)
            .setRepeatedExportLimit(5)
            .setRegistry(registryConfig);
        providerConfig0.export();

        // 发布一个服务，每个请求要执行2秒
        ServerConfig serverConfig1 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22223)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig1 = new ProviderConfig<HelloService>()
            .setId("p-1")
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("mock")
            .setRef(new HelloServiceImpl(2000))
            .setServer(serverConfig1)
            .setRepeatedExportLimit(5)
            .setRegistry(registryConfig2);
        providerConfig1.export();

        ConsumerConfig<HelloService> consumerConfig0 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("mock")
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(3000);
        consumerConfig0.refer();
        Assert.assertTrue(consumerConfig0.getConsumerBootstrap().isSubscribed());
        consumerConfig0.refer();

        consumerConfig0.unRefer();
        consumerConfig0.unRefer();

        ConsumerConfig<HelloService> consumerConfig1 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("mock")
            .setRegistry(Arrays.asList(registryConfig, registryConfig2))
            .setTimeout(3000).setAddressWait(-1);
        start = System.currentTimeMillis();
        consumerConfig1.refer();
        end = System.currentTimeMillis();
        System.out.println("elapsed time " + (end - start) + "ms");
        Assert.assertTrue((end - start) > 2000 && (end - start) < 4000);
        Assert.assertTrue(consumerConfig1.getConsumerBootstrap().isSubscribed());
        Assert.assertTrue(consumerConfig1.getConsumerBootstrap().getCluster()
            .getAddressHolder().getAllProviderSize() > 0);

        Assert.assertTrue(consumerConfig3.getConsumerBootstrap().getCluster()
            .getAddressHolder().getAllProviderSize() > 0);

        ConsumerConfig<HelloService> consumerConfig2 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("mock")
            .setRegistry(Arrays.asList(registryConfig, registryConfig2))
            .setTimeout(3000).setAddressWait(1000);
        start = System.currentTimeMillis();
        consumerConfig2.refer();
        end = System.currentTimeMillis();
        System.out.println("elapsed time " + (end - start) + "ms");
        Assert.assertTrue((end - start) > 1000 && (end - start) < 3000);
        Assert.assertFalse(consumerConfig2.getConsumerBootstrap().isSubscribed());

        try {
            Thread.sleep(1500);
        } catch (Exception e) {
        }
        Assert.assertTrue(consumerConfig2.getConsumerBootstrap().getCluster()
            .getAddressHolder().getAllProviderSize() > 0);
    }

    @Test
    public void testAttrUpdate() {
        // 发布一个服务
        ServerConfig serverConfig0 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22224)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig0 = new ProviderConfig<HelloService>()
            .setId("p-0")
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("attr")
            .setRef(new HelloServiceImpl(1000))
            .setServer(serverConfig0)
            .setRepeatedExportLimit(5);
        providerConfig0.export();

        ConsumerConfig<HelloService> consumerConfig0 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("attr")
            .setProxy("jdk")
            .setDirectUrl("bolt://127.0.0.1:22224")
            .setTimeout(500);
        HelloService proxy = consumerConfig0.refer();
        Invoker invoker = JDKProxy.parseInvoker(proxy);
        Cluster cluster = consumerConfig0.getConsumerBootstrap().getCluster();
        boolean error = false;
        try {
            proxy.sayHello("11", 11);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            error = true;
        }
        Assert.assertTrue(error);

        // wrong key
        error = false;
        try {
            consumerConfig0.getConfigListener().attrUpdated(Collections.singletonMap("loadbalance", "asdasd"));
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);
        // wrong value
        error = false;
        try {
            consumerConfig0.getConfigListener().attrUpdated(Collections.singletonMap("loadBalancer", "asdasd"));
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);

        // 动态加大超时时间
        consumerConfig0.getConfigListener().attrUpdated(Collections.singletonMap("timeout", "2000"));
        Invoker invoker2 = JDKProxy.parseInvoker(proxy);
        Cluster cluster2 = consumerConfig0.getConsumerBootstrap().getCluster();
        error = false;
        try {
            proxy.sayHello("11", 11);
        } catch (Exception e) {
            e.printStackTrace();
            error = true;
        }
        Assert.assertFalse(error);
        Assert.assertTrue(invoker == invoker2);
        Assert.assertTrue(cluster != cluster2);

        // 切到一个没有的分组
        consumerConfig0.getConfigListener().attrUpdated(Collections.singletonMap("uniqueId", "attr2"));
        error = false;
        try {
            proxy.sayHello("11", 11);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        // 切到一个有的分组
        consumerConfig0.getConfigListener().attrUpdated(Collections.singletonMap("uniqueId", "attr"));
        error = false;
        try {
            proxy.sayHello("11", 11);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);
    }
}