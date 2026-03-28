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

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for SubscribeServiceResult
 *
 * @author SOFA-RPC Team
 */
public class SubscribeServiceResultTest {

    @Test
    public void testGettersAndSetters() {
        SubscribeServiceResult result = new SubscribeServiceResult();

        result.setServiceName("com.test.Service");
        Assert.assertEquals("com.test.Service", result.getServiceName());

        result.setSuccess(true);
        Assert.assertTrue(result.isSuccess());

        result.setErrorMessage("test error");
        Assert.assertEquals("test error", result.getErrorMessage());

        List<String> datas = new ArrayList<>();
        datas.add("127.0.0.1:8080");
        result.setDatas(datas);
        Assert.assertEquals(datas, result.getDatas());
        Assert.assertEquals(1, result.getDatas().size());
    }

    @Test
    public void testToString() {
        SubscribeServiceResult result = new SubscribeServiceResult();
        result.setServiceName("com.test.Service");
        result.setSuccess(true);

        List<String> datas = new ArrayList<>();
        datas.add("127.0.0.1:8080");
        result.setDatas(datas);

        String str = result.toString();
        Assert.assertNotNull(str);
        Assert.assertTrue(str.contains("SubscribeServiceResult"));
        Assert.assertTrue(str.contains("com.test.Service"));
        Assert.assertTrue(str.contains("success=true"));
    }

    @Test
    public void testDefaultConstructor() {
        SubscribeServiceResult result = new SubscribeServiceResult();
        Assert.assertNotNull(result);
        Assert.assertNull(result.getServiceName());
        Assert.assertFalse(result.isSuccess());
        Assert.assertNull(result.getErrorMessage());
        Assert.assertNotNull(result.getDatas());
        Assert.assertTrue(result.getDatas().isEmpty());
    }
}
