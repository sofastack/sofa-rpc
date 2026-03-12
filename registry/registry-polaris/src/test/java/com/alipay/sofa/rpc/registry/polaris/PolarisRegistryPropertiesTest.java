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
package com.alipay.sofa.rpc.registry.polaris;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for PolarisRegistryProperties
 *
 * @author SOFA-RPC Team
 */
public class PolarisRegistryPropertiesTest {

    @Test
    public void testDefaultConstructorWithNull() {
        PolarisRegistryProperties properties = new PolarisRegistryProperties(null);
        Assert.assertNotNull(properties);
        Assert.assertEquals("grpc", properties.getConnectorProtocol());
        Assert.assertEquals(10, properties.getHealthCheckTTL());
        Assert.assertEquals(3000, properties.getHeartbeatInterval());
        Assert.assertEquals(1, properties.getHeartbeatCoreSize());
        Assert.assertEquals(1000, properties.getLookupInterval());
    }

    @Test
    public void testDefaultConstructorWithEmptyMap() {
        PolarisRegistryProperties properties = new PolarisRegistryProperties(Collections.emptyMap());
        Assert.assertNotNull(properties);
        Assert.assertEquals("grpc", properties.getConnectorProtocol());
        Assert.assertEquals(10, properties.getHealthCheckTTL());
        Assert.assertEquals(3000, properties.getHeartbeatInterval());
        Assert.assertEquals(1, properties.getHeartbeatCoreSize());
        Assert.assertEquals(1000, properties.getLookupInterval());
    }

    @Test
    public void testCustomParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("connector.protocol", "http");
        params.put("healthCheck.ttl", "20");
        params.put("heartbeat.interval", "5000");
        params.put("heartbeat.coreSize", "4");
        params.put("lookup.interval", "2000");

        PolarisRegistryProperties properties = new PolarisRegistryProperties(params);
        Assert.assertNotNull(properties);
        Assert.assertEquals("http", properties.getConnectorProtocol());
        Assert.assertEquals(20, properties.getHealthCheckTTL());
        Assert.assertEquals(5000, properties.getHeartbeatInterval());
        Assert.assertEquals(4, properties.getHeartbeatCoreSize());
        Assert.assertEquals(2000, properties.getLookupInterval());
    }

    @Test
    public void testPartialParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("connector.protocol", "http");
        params.put("healthCheck.ttl", "20");

        PolarisRegistryProperties properties = new PolarisRegistryProperties(params);
        Assert.assertNotNull(properties);
        Assert.assertEquals("http", properties.getConnectorProtocol());
        Assert.assertEquals(20, properties.getHealthCheckTTL());
        Assert.assertEquals(3000, properties.getHeartbeatInterval());
        Assert.assertEquals(1, properties.getHeartbeatCoreSize());
        Assert.assertEquals(1000, properties.getLookupInterval());
    }
}
