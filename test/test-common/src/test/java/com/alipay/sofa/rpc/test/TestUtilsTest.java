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
package com.alipay.sofa.rpc.test;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;

/**
 * Unit tests for TestUtils
 *
 * @author SOFA-RPC Team
 */
public class TestUtilsTest {

    @Test
    public void testDelayGetSuccess() throws Exception {
        Callable<String> callable = () -> "expected";
        String result = TestUtils.delayGet(callable, "expected", 10, 3);
        Assert.assertEquals("expected", result);
    }

    @Test
    public void testDelayGetWithWait() throws Exception {
        final boolean[] called = {false};
        Callable<String> callable = () -> {
            called[0] = true;
            return "success";
        };
        String result = TestUtils.delayGet(callable, "success", 10, 5);
        Assert.assertEquals("success", result);
        Assert.assertTrue(called[0]);
    }

    @Test
    public void testDelayGetTimeout() throws Exception {
        Callable<String> callable = () -> "different";
        String result = TestUtils.delayGet(callable, "expected", 10, 2);
        Assert.assertEquals("different", result);
    }

    @Test
    public void testDelayGetException() throws Exception {
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };
        String result = TestUtils.delayGet(callable, "expected", 10, 2);
        Assert.assertNull(result);
    }

    @Test
    public void testDelayGetNullExpected() throws Exception {
        Callable<String> callable = () -> null;
        String result = TestUtils.delayGet(callable, null, 10, 3);
        Assert.assertNull(result);
    }

    @Test
    public void testClassInitialization() {
        Assert.assertNotNull(TestUtils.class);
    }
}
