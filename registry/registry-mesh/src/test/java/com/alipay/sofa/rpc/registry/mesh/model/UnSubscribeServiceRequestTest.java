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

/**
 * Unit tests for UnSubscribeServiceRequest
 *
 * @author SOFA-RPC Team
 */
public class UnSubscribeServiceRequestTest {

    @Test
    public void testGettersAndSetters() {
        UnSubscribeServiceRequest request = new UnSubscribeServiceRequest();

        request.setServiceName("com.test.Service");
        Assert.assertEquals("com.test.Service", request.getServiceName());

        request.setProtocolType("bolt");
        Assert.assertEquals("bolt", request.getProtocolType());

        request.setGroup("SOFA_TEST");
        Assert.assertEquals("SOFA_TEST", request.getGroup());

        request.setTargetAppAddress("127.0.0.1:8080");
        Assert.assertEquals("127.0.0.1:8080", request.getTargetAppAddress());
    }

    @Test
    public void testToString() {
        UnSubscribeServiceRequest request = new UnSubscribeServiceRequest();
        request.setServiceName("com.test.Service");
        request.setProtocolType("bolt");

        String str = request.toString();
        Assert.assertNotNull(str);
        Assert.assertTrue(str.contains("UnSubscribeServiceRequest"));
        Assert.assertTrue(str.contains("com.test.Service"));
        Assert.assertTrue(str.contains("bolt"));
    }

    @Test
    public void testDefaultConstructor() {
        UnSubscribeServiceRequest request = new UnSubscribeServiceRequest();
        Assert.assertNotNull(request);
        Assert.assertNull(request.getServiceName());
        Assert.assertNull(request.getProtocolType());
        Assert.assertNull(request.getGroup());
        Assert.assertNull(request.getTargetAppAddress());
    }
}
