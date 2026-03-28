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
 * Unit tests for ApplicationInfoRequest
 *
 * @author SOFA-RPC Team
 */
public class ApplicationInfoRequestTest {

    @Test
    public void testGettersAndSetters() {
        ApplicationInfoRequest request = new ApplicationInfoRequest();

        request.setAppName("test-app");
        Assert.assertEquals("test-app", request.getAppName());

        request.addParamter("key1", "value1");
        // parameters map is internal, tested via toJson
    }

    @Test
    public void testToJson() {
        ApplicationInfoRequest request = new ApplicationInfoRequest();
        request.setAppName("test-app");
        request.addParamter("extra", "data");

        String json = request.toJson();
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("appName"));
        Assert.assertTrue(json.contains("test-app"));
    }

    @Test
    public void testDefaultConstructor() {
        ApplicationInfoRequest request = new ApplicationInfoRequest();
        Assert.assertNotNull(request);
        Assert.assertNull(request.getAppName());
    }
}
