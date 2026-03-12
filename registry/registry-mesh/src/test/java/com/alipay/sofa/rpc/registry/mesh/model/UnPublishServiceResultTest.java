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
 * Unit tests for UnPublishServiceResult
 *
 * @author SOFA-RPC Team
 */
public class UnPublishServiceResultTest {

    @Test
    public void testGettersAndSetters() {
        UnPublishServiceResult result = new UnPublishServiceResult();

        result.setSuccess(true);
        Assert.assertTrue(result.isSuccess());

        result.setErrorMessage("test error");
        Assert.assertEquals("test error", result.getErrorMessage());
    }

    @Test
    public void testToString() {
        UnPublishServiceResult result = new UnPublishServiceResult();
        result.setSuccess(true);

        String str = result.toString();
        Assert.assertNotNull(str);
        Assert.assertTrue(str.contains("UnPublishServiceResult"));
        Assert.assertTrue(str.contains("success=true"));
    }

    @Test
    public void testDefaultConstructor() {
        UnPublishServiceResult result = new UnPublishServiceResult();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isSuccess());
        Assert.assertNull(result.getErrorMessage());
    }
}
