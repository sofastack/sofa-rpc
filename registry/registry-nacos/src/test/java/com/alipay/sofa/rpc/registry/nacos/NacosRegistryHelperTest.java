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
package com.alipay.sofa.rpc.registry.nacos;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author zhuoyu.sjw
 * @version $Id: NacosRegistryHelperTest.java, v 0.1 2018-12-07 19:22 zhuoyu.sjw Exp $$
 */
public class NacosRegistryHelperTest {

    @Test
    public void convertProviderToInstances() {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12200);

        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId("com.alipay.xxx.TestService")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setUniqueId("nacos-test")
            .setProxy("javassist")
            .setRegister(true)
            .setSerialization("hessian2")
            .setServer(serverConfig)
            .setWeight(222)
            .setTimeout(3000);

        List<Instance> instances = NacosRegistryHelper.convertProviderToInstances(provider);
        assertNotNull(instances);
        assertEquals(1, instances.size());

        Instance instance = instances.get(0);
        assertNotNull(instance);
        assertEquals(NacosRegistryHelper.DEFAULT_CLUSTER, instance.getClusterName());
        assertEquals(serverConfig.getPort(), instance.getPort());
        assertEquals(serverConfig.getProtocol(), instance.getMetadata().get(RpcConstants.CONFIG_KEY_PROTOCOL));
        assertEquals(provider.getSerialization(), instance.getMetadata().get(RpcConstants.CONFIG_KEY_SERIALIZATION));
        assertEquals(provider.getUniqueId(), instance.getMetadata().get(RpcConstants.CONFIG_KEY_UNIQUEID));
        assertEquals(provider.getWeight(),
            Integer.parseInt(instance.getMetadata().get(RpcConstants.CONFIG_KEY_WEIGHT)));
        assertEquals(provider.getTimeout(),
            Integer.parseInt(instance.getMetadata().get(RpcConstants.CONFIG_KEY_TIMEOUT)));
        assertEquals(provider.getSerialization(), instance.getMetadata().get(RpcConstants.CONFIG_KEY_SERIALIZATION));
        assertEquals(provider.getAppName(), instance.getMetadata().get(RpcConstants.CONFIG_KEY_APP_NAME));
        assertEquals("com.alipay.xxx.TestService:nacos-test:DEFAULT", instance.getServiceName());
    }

    @Test
    public void convertInstancesToProviders() {
        Instance instance = new Instance();
        instance.setClusterName(NacosRegistryHelper.DEFAULT_CLUSTER);
        instance.setIp("1.1.1.1");
        instance.setPort(12200);
        instance.setServiceName("com.alipay.xxx.TestService");

        List<ProviderInfo> providerInfos = NacosRegistryHelper
            .convertInstancesToProviders(Lists.newArrayList(instance));
        assertNotNull(providerInfos);
        assertEquals(1, providerInfos.size());

        ProviderInfo providerInfo = providerInfos.get(0);
        assertNotNull(providerInfo);
        assertEquals(instance.getIp(), providerInfo.getHost());
        assertEquals(instance.getPort(), providerInfo.getPort());

        assertEquals(RpcConfigs.getStringValue(RpcOptions.DEFAULT_PROTOCOL), providerInfo.getProtocolType());

        Map<String, String> metaData = Maps.newHashMap();
        metaData.put(RpcConstants.CONFIG_KEY_PROTOCOL, RpcConstants.PROTOCOL_TYPE_REST);
        instance.setMetadata(metaData);

        providerInfos = NacosRegistryHelper.convertInstancesToProviders(Lists.newArrayList(instance));
        providerInfo = providerInfos.get(0);
        assertEquals(RpcConstants.PROTOCOL_TYPE_REST, providerInfo.getProtocolType());
    }

    @Test
    public void buildServiceName() {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12200);

        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId("com.alipay.xxx.TestService")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setUniqueId("nacos-test")
            .setProxy("javassist")
            .setRegister(true)
            .setSerialization("hessian2")
            .setServer(serverConfig)
            .setWeight(222)
            .setTimeout(3000);
        String serviceName = NacosRegistryHelper.buildServiceName(provider, RpcConstants.PROTOCOL_TYPE_BOLT);
        assertEquals(serviceName, "com.alipay.xxx.TestService:nacos-test:DEFAULT");

        serviceName = NacosRegistryHelper.buildServiceName(provider, RpcConstants.PROTOCOL_TYPE_TR);
        assertEquals(serviceName, "com.alipay.xxx.TestService:nacos-test:DEFAULT");

        serviceName = NacosRegistryHelper.buildServiceName(provider, RpcConstants.PROTOCOL_TYPE_TRIPLE);
        assertEquals(serviceName, "com.alipay.xxx.TestService:nacos-test:" + RpcConstants.PROTOCOL_TYPE_TRIPLE);

        serviceName = NacosRegistryHelper.buildServiceName(provider, RpcConstants.PROTOCOL_TYPE_REST);
        assertEquals(serviceName, "com.alipay.xxx.TestService:nacos-test:" + RpcConstants.PROTOCOL_TYPE_REST);
    }
}