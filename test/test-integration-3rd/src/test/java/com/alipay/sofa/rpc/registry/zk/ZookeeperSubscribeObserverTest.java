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

import com.alipay.sofa.rpc.client.AddressHolder;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.registry.base.BaseZkTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import com.alipay.sofa.rpc.test.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ZookeeperSubscribeObserverTest extends BaseZkTest {

    @Test
    public void testAll() throws Exception {
        RegistryConfig registryConfig = new RegistryConfig().setProtocol(RpcConstants.REGISTRY_PROTOCOL_ZK)
            .setAddress("127.0.0.1:2181");
        ZookeeperRegistry registry = (ZookeeperRegistry) RegistryFactory
            .getRegistry(registryConfig);
        registry.start();

        ServerConfig serverConfig = new ServerConfig().setPort(22222)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName()).setRef(new HelloServiceImpl(22222))
            .setServer(serverConfig).setRegistry(registryConfig)
            .setParameter(ProviderInfoAttrs.ATTR_WARMUP_TIME, "2000")
            .setParameter(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT, "100").setUniqueId("uniqueIdA")
            .setRepeatedExportLimit(-1).setWeight(0);

        ServerConfig serverConfig2 = new ServerConfig().setPort(22111)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName()).setRef(new HelloServiceImpl(22111))
            .setServer(serverConfig2).setRegistry(registryConfig)
            .setUniqueId("uniqueIdB").setRepeatedExportLimit(-1).setWeight(0);

        providerConfig.export();
        providerConfig2.export();

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName()).setRegistry(registryConfig)
            .setTimeout(3333).setUniqueId("uniqueIdA").setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        HelloService helloService = consumerConfig.refer();

        AddressHolder addressHolder = consumerConfig.getConsumerBootstrap().getCluster()
            .getAddressHolder();
        Assert.assertTrue(addressHolder.getAllProviderSize() == 1);

        ServerConfig serverConfig3 = new ServerConfig().setPort(22133)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<HelloService> providerConfig3 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName()).setRef(new HelloServiceImpl(22133))
            .setServer(serverConfig3).setRegistry(registryConfig).setRepeatedExportLimit(-1)
            .setUniqueId("uniqueIdB").setRepeatedExportLimit(-1).setWeight(0);

        providerConfig3.export();

        Assert.assertTrue(delayGetSize(addressHolder, 1, 100) == 1);

        ServerConfig serverConfig4 = new ServerConfig().setPort(22244)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        ProviderConfig<HelloService> providerConfig4 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName()).setRef(new HelloServiceImpl(22244))
            .setServer(serverConfig4).setRegistry(registryConfig).setUniqueId("uniqueIdA")
            .setRepeatedExportLimit(-1).setWeight(0);

        providerConfig4.export();

        Assert.assertTrue(delayGetSize(addressHolder, 1, 100) == 2);
    }

    int delayGetSize(final AddressHolder addressHolder, int expect, int n50ms) {
        return TestUtils.delayGet(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return addressHolder.getAllProviderSize();
            }
        }, expect, 70, n50ms);
    }
}
