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
package com.alipay.sofa.rpc.client.router;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.client.AddressHolder;
import com.alipay.sofa.rpc.client.Cluster;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for MeshRouter
 *
 * @author SOFA-RPC Team
 */
public class MeshRouterTest {

    @Test
    public void testExtensionAnnotation() {
        Assert.assertNotNull(
            MeshRouter.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class));

        com.alipay.sofa.rpc.ext.Extension extension =
                MeshRouter.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class);
        Assert.assertEquals("mesh", extension.value());
        Assert.assertEquals(-19000, extension.order());
    }

    @Test
    public void testAutoActiveAnnotation() {
        Assert.assertNotNull(
            MeshRouter.class.getAnnotation(com.alipay.sofa.rpc.filter.AutoActive.class));
    }

    @Test
    public void testConstantValue() {
        Assert.assertEquals("MESH", MeshRouter.RPC_MESH_ROUTER);
    }

    @Test
    public void testNeedToLoadWithMeshRegistry() {
        MeshRouter router = new MeshRouter();

        ConsumerBootstrap mockBootstrap = Mockito.mock(ConsumerBootstrap.class);
        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setInterfaceId("com.test.Service");

        List<RegistryConfig> registryConfigs = new ArrayList<>();
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setProtocol("mesh");
        registryConfigs.add(registryConfig);
        consumerConfig.setRegistry(registryConfigs);

        Mockito.when(mockBootstrap.getConsumerConfig()).thenReturn(consumerConfig);

        boolean result = router.needToLoad(mockBootstrap);
        Assert.assertTrue(result);
    }

    @Test
    public void testNeedToLoadWithDirectUrl() {
        MeshRouter router = new MeshRouter();

        ConsumerBootstrap mockBootstrap = Mockito.mock(ConsumerBootstrap.class);
        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setInterfaceId("com.test.Service");
        consumerConfig.setDirectUrl("127.0.0.1:8080");

        List<RegistryConfig> registryConfigs = new ArrayList<>();
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setProtocol("mesh");
        registryConfigs.add(registryConfig);
        consumerConfig.setRegistry(registryConfigs);

        Mockito.when(mockBootstrap.getConsumerConfig()).thenReturn(consumerConfig);

        boolean result = router.needToLoad(mockBootstrap);
        Assert.assertFalse(result);
    }

    @Test
    public void testNeedToLoadWithoutMeshRegistry() {
        MeshRouter router = new MeshRouter();

        ConsumerBootstrap mockBootstrap = Mockito.mock(ConsumerBootstrap.class);
        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setInterfaceId("com.test.Service");

        List<RegistryConfig> registryConfigs = new ArrayList<>();
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setProtocol("zookeeper");
        registryConfigs.add(registryConfig);
        consumerConfig.setRegistry(registryConfigs);

        Mockito.when(mockBootstrap.getConsumerConfig()).thenReturn(consumerConfig);

        boolean result = router.needToLoad(mockBootstrap);
        Assert.assertFalse(result);
    }

    @Test
    public void testInit() {
        MeshRouter router = new MeshRouter();
        ConsumerBootstrap mockBootstrap = Mockito.mock(ConsumerBootstrap.class);

        router.init(mockBootstrap);
        Assert.assertNotNull(router.consumerBootstrap);
    }

    @Test
    public void testRoute() {
        MeshRouter router = new MeshRouter();

        ConsumerBootstrap mockBootstrap = Mockito.mock(ConsumerBootstrap.class);
        Cluster mockCluster = Mockito.mock(Cluster.class);
        AddressHolder mockAddressHolder = Mockito.mock(AddressHolder.class);

        List<ProviderInfo> existingProviders = new ArrayList<>();
        ProviderInfo provider1 = new ProviderInfo();
        provider1.setHost("127.0.0.1");
        provider1.setPort(8080);
        existingProviders.add(provider1);

        Mockito.when(mockBootstrap.getCluster()).thenReturn(mockCluster);
        Mockito.when(mockCluster.getAddressHolder()).thenReturn(mockAddressHolder);
        Mockito.when(mockAddressHolder.getProviderInfos(RpcConstants.ADDRESS_DEFAULT_GROUP))
            .thenReturn(existingProviders);

        router.init(mockBootstrap);

        SofaRequest mockRequest = Mockito.mock(SofaRequest.class);
        List<ProviderInfo> inputProviders = new ArrayList<>();
        ProviderInfo provider2 = new ProviderInfo();
        provider2.setHost("127.0.0.2");
        provider2.setPort(8081);
        inputProviders.add(provider2);

        List<ProviderInfo> result = router.route(mockRequest, inputProviders);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void testRouteWithNullInput() {
        MeshRouter router = new MeshRouter();

        ConsumerBootstrap mockBootstrap = Mockito.mock(ConsumerBootstrap.class);
        Cluster mockCluster = Mockito.mock(Cluster.class);
        AddressHolder mockAddressHolder = Mockito.mock(AddressHolder.class);

        List<ProviderInfo> existingProviders = new ArrayList<>();
        ProviderInfo provider1 = new ProviderInfo();
        provider1.setHost("127.0.0.1");
        provider1.setPort(8080);
        existingProviders.add(provider1);

        Mockito.when(mockBootstrap.getCluster()).thenReturn(mockCluster);
        Mockito.when(mockCluster.getAddressHolder()).thenReturn(mockAddressHolder);
        Mockito.when(mockAddressHolder.getProviderInfos(RpcConstants.ADDRESS_DEFAULT_GROUP))
            .thenReturn(existingProviders);

        router.init(mockBootstrap);

        SofaRequest mockRequest = Mockito.mock(SofaRequest.class);

        List<ProviderInfo> result = router.route(mockRequest, null);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testRouteWithNullAddressHolder() {
        MeshRouter router = new MeshRouter();

        ConsumerBootstrap mockBootstrap = Mockito.mock(ConsumerBootstrap.class);
        Cluster mockCluster = Mockito.mock(Cluster.class);

        Mockito.when(mockBootstrap.getCluster()).thenReturn(mockCluster);
        Mockito.when(mockCluster.getAddressHolder()).thenReturn(null);

        router.init(mockBootstrap);

        SofaRequest mockRequest = Mockito.mock(SofaRequest.class);
        List<ProviderInfo> inputProviders = new ArrayList<>();
        ProviderInfo provider1 = new ProviderInfo();
        provider1.setHost("127.0.0.1");
        inputProviders.add(provider1);

        List<ProviderInfo> result = router.route(mockRequest, inputProviders);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
    }
}
