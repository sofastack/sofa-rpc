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
package com.alipay.sofa.rpc.registry.zk;

import java.util.List;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.registry.base.BaseMultiZkTest;
import com.alipay.sofa.rpc.registry.zk.demo.ZkTestService;
import com.alipay.sofa.rpc.registry.zk.demo.ZkTestServiceImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author leizhiyuan
 */
public class ZookeeperMultiRegistryTest extends BaseMultiZkTest {

    private static RegistryConfig    registryConfig1;

    private static ZookeeperRegistry registry1;

    private static RegistryConfig    registryConfig2;

    private static ZookeeperRegistry registry2;

    @BeforeClass
    public static void setUp() {
        registryConfig1 = new RegistryConfig()
            .setProtocol(RpcConstants.REGISTRY_PROTOCOL_ZK)
            .setSubscribe(true)
            .setAddress("127.0.0.1:2181")
            .setRegister(true);

        registry1 = (ZookeeperRegistry) RegistryFactory.getRegistry(registryConfig1);
        registry1.init();
        Assert.assertTrue(registry1.start());

        registryConfig2 = new RegistryConfig()
            .setProtocol(RpcConstants.REGISTRY_PROTOCOL_ZK)
            .setSubscribe(true)
            .setAddress("127.0.0.1:3181")
            .setRegister(true);

        registry2 = (ZookeeperRegistry) RegistryFactory.getRegistry(registryConfig2);
        registry2.init();
        Assert.assertTrue(registry2.start());
    }

    @AfterClass
    public static void tearDown() {
        registry1.destroy();
        registry1 = null;

        registry2.destroy();
        registry2 = null;
    }

    /**
     * 测试Zookeeper Provider Observer
     *
     * @throws Exception
     */
    @Test
    public void testMultiRegistry() throws Exception {

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12200);

        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId(ZkTestService.class.getName())
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server1"))
            .setProxy("javassist")
            .setRegister(true)
            .setRegistry(registryConfig1)
            .setRegistry(registryConfig2)
            .setSerialization("hessian2")
            .setServer(serverConfig)
            .setWeight(222)
            .setTimeout(3000)
            .setRef(new ZkTestServiceImpl())
            .setRepeatedExportLimit(2);

        // 注册
        provider.export();

        ServerConfig serverConfig2 = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12201);
        ProviderConfig<?> provider2 = new ProviderConfig();
        provider2.setInterfaceId(ZkTestService.class.getName())
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server2"))
            .setProxy("javassist")
            .setRegister(true)
            .setRegistry(registryConfig1)
            .setRegistry(registryConfig2)
            .setSerialization("hessian2")
            .setServer(serverConfig2)
            .setWeight(222)
            .setTimeout(3000)
            .setRef(new ZkTestServiceImpl())
            .setRepeatedExportLimit(2);

        provider2.export();

        ConsumerConfig<ZkTestService> consumer = new ConsumerConfig<ZkTestService>();
        consumer.setInterfaceId(ZkTestService.class.getName())
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("hessian2")
            .setInvokeType("sync")
            .setTimeout(4444)
            .setRegistry(registryConfig1)
            .setRegistry(registryConfig2);

        //订阅一下
        consumer.refer();

        Thread.sleep(1000);

        List<ProviderGroup> providerGroups = consumer.getConsumerBootstrap().getCluster().getAddressHolder()
            .getProviderGroups();

        for (ProviderGroup providerGroup : providerGroups) {
            if (providerGroup.getName().equals(RpcConstants.ADDRESS_DEFAULT_GROUP)) {
                //内部有去重复的逻辑
                Assert.assertEquals(2, providerGroup.getProviderInfos().size());
            }
        }

    }
}