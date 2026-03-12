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
package com.alipay.sofa.rpc.registry.mesh.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for ProviderMetaInfo
 *
 * @author SOFA-RPC Team
 */
public class ProviderMetaInfoTest {

    @Test
    public void testGettersAndSetters() {
        ProviderMetaInfo info = new ProviderMetaInfo();

        info.setProtocol("bolt");
        Assert.assertEquals("bolt", info.getProtocol());

        info.setVersion("4.0");
        Assert.assertEquals("4.0", info.getVersion());

        info.setSerializeType("hessian2");
        Assert.assertEquals("hessian2", info.getSerializeType());

        info.setAppName("test-app");
        Assert.assertEquals("test-app", info.getAppName());

        Map<String, String> props = new HashMap<>();
        props.put("key", "value");
        info.setProperties(props);
        Assert.assertEquals(props, info.getProperties());
    }

    @Test
    public void testToString() {
        ProviderMetaInfo info = new ProviderMetaInfo();
        info.setProtocol("bolt");
        info.setVersion("4.0");
        info.setAppName("test-app");

        String str = info.toString();
        Assert.assertNotNull(str);
        Assert.assertTrue(str.contains("ProviderMetaInfo"));
        Assert.assertTrue(str.contains("bolt"));
        Assert.assertTrue(str.contains("4.0"));
        Assert.assertTrue(str.contains("test-app"));
    }

    @Test
    public void testDefaultConstructor() {
        ProviderMetaInfo info = new ProviderMetaInfo();
        Assert.assertNotNull(info);
        Assert.assertNull(info.getProtocol());
        Assert.assertNull(info.getVersion());
        Assert.assertNull(info.getSerializeType());
        Assert.assertNull(info.getAppName());
        Assert.assertNull(info.getProperties());
    }
}
