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
package com.alipay.sofa.rpc.bootstrap.rest;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.ext.Extension;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for RestProviderBootstrap
 *
 * @author SOFA-RPC Team
 */
public class RestProviderBootstrapTest {

    @Test
    public void testProviderBootstrapCreation() {
        ProviderConfig<Object> providerConfig = new ProviderConfig<>();
        providerConfig.setInterfaceId("com.test.RestService");

        RestProviderBootstrap<Object> bootstrap = new RestProviderBootstrap<>(providerConfig);

        Assert.assertNotNull(bootstrap);
        Assert.assertEquals("com.test.RestService", bootstrap.getProviderConfig().getInterfaceId());
    }

    @Test
    public void testProviderBootstrapWithNullConfig() {
        RestProviderBootstrap<Object> bootstrap = new RestProviderBootstrap<>(null);
        Assert.assertNotNull(bootstrap);
    }

    @Test
    public void testExtensionAnnotation() {
        Extension extension = RestProviderBootstrap.class.getAnnotation(Extension.class);
        Assert.assertNotNull(extension);
        Assert.assertEquals("rest", extension.value());
    }
}
