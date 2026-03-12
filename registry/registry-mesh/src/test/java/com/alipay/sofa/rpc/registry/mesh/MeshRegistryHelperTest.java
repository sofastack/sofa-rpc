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
package com.alipay.sofa.rpc.registry.mesh;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for MeshRegistryHelper
 *
 * @author SOFA-RPC Team
 */
public class MeshRegistryHelperTest {

    @Test
    public void testConvertProviderToProviderInfo() {
        ServerConfig server = new ServerConfig()
            .setProtocol("bolt")
            .setHost("127.0.0.1")
            .setPort(8080)
            .setContextPath("/api");

        ProviderConfig provider = new ProviderConfig();
        provider.setInterfaceId("com.test.Service")
            .setApplication(new ApplicationConfig().setAppName("test-app"))
            .setParameter("weight", "100")
            .setSerialization("hessian2");

        ProviderInfo result = MeshRegistryHelper.convertProviderToProviderInfo(provider, server);

        Assert.assertNotNull(result);
        Assert.assertEquals(8080, result.getPort());
        Assert.assertEquals(100, result.getWeight());
        Assert.assertEquals("hessian2", result.getSerializationType());
        Assert.assertEquals("bolt", result.getProtocolType());
        Assert.assertTrue(result.getPath().contains("/api"));
        Assert.assertNotNull(result.getHost());
    }

    @Test
    public void testConvertProviderToProviderInfoWithAnyHost() {
        ServerConfig server = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(8080);

        ProviderConfig provider = new ProviderConfig();
        provider.setInterfaceId("com.test.Service")
            .setApplication(new ApplicationConfig().setAppName("test-app"))
            .setParameter("weight", "100")
            .setSerialization("java");

        ProviderInfo result = MeshRegistryHelper.convertProviderToProviderInfo(provider, server);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getHost());
        Assert.assertNotEquals("0.0.0.0", result.getHost());
    }

    @Test
    public void testBuildMeshKey() {
        ServerConfig server = new ServerConfig()
            .setProtocol("bolt")
            .setHost("127.0.0.1")
            .setPort(8080);

        ProviderConfig provider = new ProviderConfig();
        provider.setInterfaceId("com.test.Service")
            .setApplication(new ApplicationConfig().setAppName("test-app"))
            .setUniqueId("v1");

        String key = MeshRegistryHelper.buildMeshKey(provider, server.getProtocol());

        Assert.assertNotNull(key);
        Assert.assertTrue(key.contains("com.test.Service"));
    }
}
