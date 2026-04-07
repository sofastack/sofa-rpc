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
package com.alipay.sofa.rpc.transport.triple.http;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DefaultHttpHeaders}.
 */
public class DefaultHttpHeadersTest {

    private DefaultHttpHeaders headers;

    @Before
    public void setUp() {
        headers = new DefaultHttpHeaders();
    }

    @Test
    public void testSetAndGet() {
        headers.set("Content-Type", "application/json");
        Assert.assertEquals("application/json", headers.get("Content-Type"));
    }

    @Test
    public void testSetCaseInsensitive() {
        headers.set("Content-Type", "application/json");
        Assert.assertEquals("application/json", headers.get("content-type"));
        Assert.assertEquals("application/json", headers.get("CONTENT-TYPE"));
    }

    @Test
    public void testGetNotFound() {
        Assert.assertNull(headers.get("Not-Found"));
    }

    @Test
    public void testGetWithDefault() {
        Assert.assertEquals("default", headers.get("Not-Found", "default"));
    }

    @Test
    public void testAddMultipleValues() {
        headers.add("Accept", "application/json");
        headers.add("Accept", "text/plain");

        Iterable<String> values = headers.getAll("Accept");
        int count = 0;
        for (String value : values) {
            count++;
        }
        Assert.assertEquals(2, count);
    }

    @Test
    public void testRemove() {
        headers.set("Content-Type", "application/json");
        headers.remove("Content-Type");
        Assert.assertNull(headers.get("Content-Type"));
    }

    @Test
    public void testContains() {
        headers.set("Content-Type", "application/json");
        Assert.assertTrue(headers.contains("Content-Type"));
        Assert.assertTrue(headers.contains("content-type"));
        Assert.assertFalse(headers.contains("Not-Found"));
    }

    @Test
    public void testIsEmpty() {
        Assert.assertTrue(headers.isEmpty());

        headers.set("Content-Type", "application/json");
        Assert.assertFalse(headers.isEmpty());
    }

    @Test
    public void testSize() {
        Assert.assertEquals(0, headers.size());

        headers.set("Content-Type", "application/json");
        Assert.assertEquals(1, headers.size());

        headers.set("Accept", "text/plain");
        Assert.assertEquals(2, headers.size());
    }

    @Test
    public void testClear() {
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "text/plain");

        headers.clear();

        Assert.assertEquals(0, headers.size());
        Assert.assertTrue(headers.isEmpty());
    }

    @Test
    public void testIterator() {
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "text/plain");

        int count = 0;
        for (java.util.Map.Entry<String, String> entry : headers) {
            Assert.assertNotNull(entry.getKey());
            Assert.assertNotNull(entry.getValue());
            count++;
        }
        Assert.assertEquals(2, count);
    }

    @Test
    public void testSetNullName() {
        headers.set(null, "value");
        Assert.assertNull(headers.get(null));
    }

    @Test
    public void testSetNullValue() {
        headers.set("Content-Type", null);
        Assert.assertNull(headers.get("Content-Type"));
    }

    @Test
    public void testToString() {
        headers.set("Content-Type", "application/json");
        String str = headers.toString();
        Assert.assertTrue(str.contains("DefaultHttpHeaders"));
        Assert.assertTrue(str.contains("content-type"));
        Assert.assertTrue(str.contains("application/json"));
    }
}