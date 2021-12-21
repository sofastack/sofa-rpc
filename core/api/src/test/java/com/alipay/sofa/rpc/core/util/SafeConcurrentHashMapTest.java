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
package com.alipay.sofa.rpc.core.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class SafeConcurrentHashMapTest {
    public static final String    KEY   = "key";
    public static final String    VALUE = "value";
    private SafeConcurrentHashMap map   = new SafeConcurrentHashMap();

    @Before
    public void before() {
        map = new SafeConcurrentHashMap();
        map.put(KEY, VALUE);
    }

    @Test
    public void testPut() {
        SafeConcurrentHashMap<Object, Object> map = new SafeConcurrentHashMap<>();
        Assert.assertNull(map.put(null, null));
        Assert.assertNull(map.put(null, "value"));
        Assert.assertNull(map.put("key", null));
        Assert.assertEquals(0, map.size());
        Assert.assertNull(map.put("key", "value"));
        Assert.assertEquals(1, map.size());
    }

    @Test
    public void testPutIfAbsent() {
        SafeConcurrentHashMap<Object, Object> map = new SafeConcurrentHashMap<>();
        Assert.assertNull(map.putIfAbsent(null, null));
        Assert.assertNull(map.putIfAbsent(null, "value"));
        Assert.assertNull(map.putIfAbsent("key", null));
        Assert.assertEquals(0, map.size());
        Assert.assertNull(map.putIfAbsent("key", "value"));
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("value", map.get("key"));
        Assert.assertEquals("value", map.putIfAbsent("key", "anotherValue"));
    }

    @Test
    public void testPutAll() {
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put(null, "value");
        hashMap.put("key", null);
        hashMap.put("key1", "value");
        Assert.assertEquals(3, hashMap.size());
        SafeConcurrentHashMap<Object, Object> safeConcurrentHashMap = new SafeConcurrentHashMap<>();
        safeConcurrentHashMap.putAll(hashMap);
        Assert.assertEquals(1, safeConcurrentHashMap.size());
        Assert.assertEquals("value", safeConcurrentHashMap.get("key1"));
    }

    @Test
    public void testGet() {
        Assert.assertNull(map.get(null));
        Assert.assertEquals(VALUE, map.get(KEY));
    }

    @Test
    public void testComputeIfAbsent() {
        Assert.assertEquals(null, map.computeIfAbsent(null, key -> ""));
        Assert.assertEquals(VALUE, map.computeIfAbsent(KEY, key -> ""));
        Assert.assertEquals(VALUE, map.get(KEY));
    }

    @Test
    public void testComputeIfPresent() {
        Assert.assertEquals(VALUE, map.get(KEY));
        Assert.assertEquals(null, map.computeIfAbsent(null, key -> ""));
        Assert.assertEquals("", map.computeIfPresent(KEY, (key, value) -> ""));
        Assert.assertEquals("", map.get(KEY));
    }

    @Test
    public void testCompute() {
        Assert.assertEquals(VALUE, map.get(KEY));
        Assert.assertEquals(null, map.compute(null, (key, value) -> ""));
        Assert.assertEquals(VALUE, map.get(KEY));
    }

    @Test
    public void testMerge() {
        Assert.assertEquals(VALUE, map.get(KEY));
        try {
            map.merge(null, null, (key, value) -> "");
            Assert.fail();
        } catch (NullPointerException npe) {
            //ignore
        }
        Assert.assertNull(map.merge(null, "value", (key, value) -> ""));
        Assert.assertEquals("value1", map.merge("key1", "value1", (v1, v2) -> v1 + "" + v2));
        Assert.assertEquals(VALUE + VALUE, map.merge(KEY, VALUE, (v1, v2) -> v1 + "" + v2));
    }
}