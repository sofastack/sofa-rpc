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
 * Unit tests for SubscribeServiceRequest
 *
 * @author SOFA-RPC Team
 */
public class SubscribeServiceRequestTest {

    @Test
    public void testGettersAndSetters() {
        SubscribeServiceRequest request = new SubscribeServiceRequest();

        request.setServiceName("com.test.Service");
        Assert.assertEquals("com.test.Service", request.getServiceName());

        request.setProtocolType("bolt");
        Assert.assertEquals("bolt", request.getProtocolType());

        request.setGroup("SOFA_TEST");
        Assert.assertEquals("SOFA_TEST", request.getGroup());

        request.setTargetAppAddress("127.0.0.1:8080");
        Assert.assertEquals("127.0.0.1:8080", request.getTargetAppAddress());

        request.setVipEnforce(true);
        Assert.assertTrue(request.isVipEnforce());

        request.setVipOnly(false);
        Assert.assertFalse(request.isVipOnly());

        request.setLocalCloudFirst(true);
        Assert.assertTrue(request.isLocalCloudFirst());

        Map<String, String> props = new HashMap<>();
        props.put("key", "value");
        request.setProperties(props);
        Assert.assertEquals(props, request.getProperties());
    }

    @Test
    public void testToString() {
        SubscribeServiceRequest request = new SubscribeServiceRequest();
        request.setServiceName("com.test.Service");
        request.setProtocolType("bolt");
        request.setVipEnforce(true);

        String str = request.toString();
        Assert.assertNotNull(str);
        Assert.assertTrue(str.contains("SubscribeServiceRequest"));
        Assert.assertTrue(str.contains("com.test.Service"));
        Assert.assertTrue(str.contains("vipEnforce=true"));
    }

    @Test
    public void testDefaultConstructor() {
        SubscribeServiceRequest request = new SubscribeServiceRequest();
        Assert.assertNotNull(request);
        Assert.assertNull(request.getServiceName());
        Assert.assertNull(request.getProtocolType());
        Assert.assertFalse(request.isVipEnforce());
        Assert.assertFalse(request.isVipOnly());
        Assert.assertFalse(request.isLocalCloudFirst());
        Assert.assertNull(request.getGroup());
    }
}
