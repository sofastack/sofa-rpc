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
package com.alipay.sofa.rpc.transport.rest;

import com.alipay.sofa.rpc.config.JAXRSProviderManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for SofaResteasyClientBuilder
 *
 * @author SOFA-RPC Team
 */
public class SofaResteasyClientBuilderTest {

    private SofaResteasyClientBuilder clientBuilder;

    @Before
    public void setUp() {
        clientBuilder = new SofaResteasyClientBuilder();
        // Clean up any existing providers
        JAXRSProviderManager.getInternalProviderClasses().clear();
        JAXRSProviderManager.getCustomProviderInstances().clear();
    }

    @After
    public void tearDown() {
        // Clean up providers after test
        JAXRSProviderManager.getInternalProviderClasses().clear();
        JAXRSProviderManager.getCustomProviderInstances().clear();
    }

    @Test
    public void testConstructor() {
        // Verify constructor works
        assert clientBuilder != null;
    }

    @Test
    public void testRegisterProviderWithNoProviders() {
        // When no providers are registered, should return builder without error
        SofaResteasyClientBuilder result = clientBuilder.registerProvider();
        assert result != null;
    }

    @Test
    public void testRegisterProviderWithInternalProviders() {
        // Register an internal provider class
        JAXRSProviderManager.registerInternalProviderClass(String.class);

        // Register providers
        SofaResteasyClientBuilder result = clientBuilder.registerProvider();
        assert result != null;
    }

    @Test
    public void testRegisterProviderWithCustomProviders() {
        // Create a custom provider instance
        Object customProvider = new Object();
        JAXRSProviderManager.registerCustomProviderInstance(customProvider);

        // Register providers
        SofaResteasyClientBuilder result = clientBuilder.registerProvider();
        assert result != null;
    }

    @Test
    public void testRegisterProviderWithBothInternalAndCustomProviders() {
        // Register both internal and custom providers
        JAXRSProviderManager.registerInternalProviderClass(String.class);
        Object customProvider = new Object();
        JAXRSProviderManager.registerCustomProviderInstance(customProvider);

        // Register providers
        SofaResteasyClientBuilder result = clientBuilder.registerProvider();
        assert result != null;
    }

    @Test
    public void testLogProviders() {
        // Test logProviders method - should not throw exception
        SofaResteasyClientBuilder result = clientBuilder.logProviders();
        assert result != null;
    }

    @Test
    public void testChainedMethodCalls() {
        // Test chaining multiple method calls
        SofaResteasyClientBuilder result = clientBuilder.registerProvider().logProviders();
        assert result != null;
    }

    @Test
    public void testRegisterProviderAfterMultipleRegistrations() {
        // Register multiple internal providers
        JAXRSProviderManager.registerInternalProviderClass(String.class);
        JAXRSProviderManager.registerInternalProviderClass(Integer.class);

        // Register multiple custom providers
        JAXRSProviderManager.registerCustomProviderInstance(new Object());
        JAXRSProviderManager.registerCustomProviderInstance(new Object());

        // Register providers - should handle multiple registrations
        SofaResteasyClientBuilder result = clientBuilder.registerProvider();
        assert result != null;
    }
}
