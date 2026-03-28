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
package com.alipay.sofa.rpc.server.rest;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for SofaResourceFactory
 *
 * @author SOFA-RPC Team
 */
public class SofaResourceFactoryTest {

    @Test
    public void testConstructorAndGetters() {
        // Create a mock resource object
        Object resource = new Object();

        // Create ApplicationConfig and ProviderConfig
        ApplicationConfig appConfig = new ApplicationConfig();
        appConfig.setAppName("test-app");

        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setInterfaceId("com.example.TestService");
        providerConfig.setUniqueId("test-unique-id");
        providerConfig.setApplication(appConfig);

        // Create SofaResourceFactory
        SofaResourceFactory factory = new SofaResourceFactory(providerConfig, resource);

        // Verify constructor and getters
        assertNotNull(factory);
        assertEquals(providerConfig, factory.getProviderConfig());
        assertEquals("test-app", factory.getAppName());
    }

    @Test
    public void testGetServiceName() {
        // Create ApplicationConfig and ProviderConfig
        ApplicationConfig appConfig = new ApplicationConfig();
        appConfig.setAppName("test-app");

        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setInterfaceId("com.example.TestService");
        providerConfig.setUniqueId("test-unique-id");
        providerConfig.setApplication(appConfig);

        // Create SofaResourceFactory
        SofaResourceFactory factory = new SofaResourceFactory(providerConfig, new Object());

        // Verify service name is computed correctly
        String serviceName = factory.getServiceName();
        assertNotNull(serviceName);
        // Service name should contain interfaceId and uniqueId (version defaults to empty)
        assertEquals("com.example.TestService:test-unique-id", serviceName);
    }

    @Test
    public void testGetAppNameWhenNull() {
        // Create ProviderConfig without app name
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setInterfaceId("com.example.TestService");

        // Create SofaResourceFactory
        SofaResourceFactory factory = new SofaResourceFactory(providerConfig, new Object());

        // Verify app name is null
        assertNull(factory.getAppName());
    }

    @Test
    public void testGetServiceNameWithoutUniqueId() {
        // Create ApplicationConfig and ProviderConfig without uniqueId
        ApplicationConfig appConfig = new ApplicationConfig();
        appConfig.setAppName("test-app");

        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setInterfaceId("com.example.TestService");
        providerConfig.setApplication(appConfig);

        // Create SofaResourceFactory
        SofaResourceFactory factory = new SofaResourceFactory(providerConfig, new Object());

        // Verify service name without uniqueId
        String serviceName = factory.getServiceName();
        assertNotNull(serviceName);
        assertEquals("com.example.TestService", serviceName);
    }

    @Test
    public void testServiceNameCaching() {
        // Create ApplicationConfig and ProviderConfig
        ApplicationConfig appConfig = new ApplicationConfig();
        appConfig.setAppName("cached-app");

        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setInterfaceId("com.example.TestService");
        providerConfig.setUniqueId("cached-id");
        providerConfig.setApplication(appConfig);

        // Create SofaResourceFactory
        SofaResourceFactory factory = new SofaResourceFactory(providerConfig, new Object());

        // Get service name multiple times to verify caching
        String serviceName1 = factory.getServiceName();
        String serviceName2 = factory.getServiceName();

        // Both calls should return the same cached value
        assertEquals(serviceName1, serviceName2);
        assertEquals("com.example.TestService:cached-id", serviceName1);
    }
}
