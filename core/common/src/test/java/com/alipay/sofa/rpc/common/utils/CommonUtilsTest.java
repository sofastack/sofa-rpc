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
package com.alipay.sofa.rpc.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class CommonUtilsTest {

    @Test
    public void parseBoolean() {
        Assert.assertTrue(CommonUtils.parseBoolean(null, true));
        Assert.assertTrue(CommonUtils.parseBoolean("true", true));
        Assert.assertFalse(CommonUtils.parseBoolean("falSE", true));
        Assert.assertFalse(CommonUtils.parseBoolean("xxx", true));

        Assert.assertFalse(CommonUtils.parseBoolean(null, false));
        Assert.assertTrue(CommonUtils.parseBoolean("trUe", false));
        Assert.assertFalse(CommonUtils.parseBoolean("falSE", false));
        Assert.assertFalse(CommonUtils.parseBoolean("xxx", false));
    }

    @Test
    public void parseInts() {
        Assert.assertArrayEquals(new int[] { 1, 2, 3 }, CommonUtils.parseInts("1,2,3", ","));
    }

    @Test
    public void join() {
        Assert.assertEquals(CommonUtils.join(null, ","), "");
        Assert.assertEquals(CommonUtils.join(new ArrayList(), ","), "");
        List<String> s = new ArrayList<String>();
        s.add("a");
        s.add("b");
        s.add("c");
        Assert.assertEquals(CommonUtils.join(s, "1"), "a1b1c");
    }

    @Test
    public void testPutToConcurrentMap() throws Exception {
        final ConcurrentMap<String, AtomicInteger> hashMap = new ConcurrentHashMap<String, AtomicInteger>();
        final CountDownLatch latch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 10000; j++) {
                        AtomicInteger inter = CommonUtils.putToConcurrentMap(hashMap, "key", new AtomicInteger(0));
                        inter.incrementAndGet();
                    }
                    latch.countDown();
                }
            });
            thread.start();
        }
        latch.await();
        Assert.assertEquals(hashMap.get("key").get(), 30000);
    }

    @Test
    public void testIsNotEmpty() {
        List list1 = null;
        List list2 = new ArrayList();
        List list3 = new ArrayList();
        list3.add(1);

        Assert.assertTrue(CommonUtils.isEmpty(list1));
        Assert.assertTrue(CommonUtils.isEmpty(list2));
        Assert.assertFalse(CommonUtils.isEmpty(list3));

        Assert.assertFalse(CommonUtils.isNotEmpty(list1));
        Assert.assertFalse(CommonUtils.isNotEmpty(list2));
        Assert.assertTrue(CommonUtils.isNotEmpty(list3));

        Set set1 = null;
        Set set2 = new HashSet();
        Set set3 = new HashSet();
        set3.add(1);

        Assert.assertTrue(CommonUtils.isEmpty(set1));
        Assert.assertTrue(CommonUtils.isEmpty(set2));
        Assert.assertFalse(CommonUtils.isEmpty(set3));

        Assert.assertFalse(CommonUtils.isNotEmpty(set1));
        Assert.assertFalse(CommonUtils.isNotEmpty(set2));
        Assert.assertTrue(CommonUtils.isNotEmpty(set3));

        HashMap map1 = null;
        HashMap map2 = new HashMap();
        HashMap map3 = new HashMap();
        map3.put(1, 1);

        Assert.assertTrue(CommonUtils.isEmpty(map1));
        Assert.assertTrue(CommonUtils.isEmpty(map2));
        Assert.assertFalse(CommonUtils.isEmpty(map3));

        Assert.assertFalse(CommonUtils.isNotEmpty(map1));
        Assert.assertFalse(CommonUtils.isNotEmpty(map2));
        Assert.assertTrue(CommonUtils.isNotEmpty(map3));

        String[] array1 = null;
        String[] array2 = new String[0];
        String[] array3 = new String[] { "11" };

        Assert.assertTrue(CommonUtils.isEmpty(array1));
        Assert.assertTrue(CommonUtils.isEmpty(array2));
        Assert.assertFalse(CommonUtils.isEmpty(array3));

        Assert.assertFalse(CommonUtils.isNotEmpty(array1));
        Assert.assertFalse(CommonUtils.isNotEmpty(array2));
        Assert.assertTrue(CommonUtils.isNotEmpty(array3));

    }

    @Test
    public void testIsTrue() {
        Assert.assertTrue(CommonUtils.isTrue("true"));
        Assert.assertTrue(CommonUtils.isTrue("True"));
        Assert.assertFalse(CommonUtils.isTrue("111"));
        Assert.assertFalse(CommonUtils.isTrue((String) null));
        Assert.assertFalse(CommonUtils.isTrue(""));

        Assert.assertFalse(CommonUtils.isTrue((Boolean) null));
        Assert.assertTrue(CommonUtils.isTrue(Boolean.TRUE));
        Assert.assertFalse(CommonUtils.isTrue(Boolean.FALSE));
    }

    @Test
    public void testIsFalse() {
        Assert.assertTrue(CommonUtils.isFalse("false"));
        Assert.assertTrue(CommonUtils.isFalse("False"));
        Assert.assertFalse(CommonUtils.isFalse("null"));
        Assert.assertFalse(CommonUtils.isFalse(""));
        Assert.assertFalse(CommonUtils.isFalse("xxx"));

        Assert.assertFalse(CommonUtils.isFalse((Boolean) null));
        Assert.assertFalse(CommonUtils.isFalse(Boolean.TRUE));
        Assert.assertTrue(CommonUtils.isFalse(Boolean.FALSE));
    }

    @Test
    public void testParseNum() {
        Assert.assertTrue(CommonUtils.parseNum(null, 123) == 123);
        Assert.assertTrue(CommonUtils.parseNum(1234, 123) == 1234);
    }

    @Test
    public void testParseInt() {
        Assert.assertEquals(CommonUtils.parseInt("", 123), 123);
        Assert.assertEquals(CommonUtils.parseInt("xxx", 123), 123);
        Assert.assertEquals(CommonUtils.parseInt(null, 123), 123);
        Assert.assertEquals(CommonUtils.parseInt("12345", 123), 12345);
    }

    @Test
    public void testParseLong() {
        Assert.assertEquals(CommonUtils.parseLong("", 123L), 123L);
        Assert.assertEquals(CommonUtils.parseLong("xxx", 123L), 123L);
        Assert.assertEquals(CommonUtils.parseLong(null, 123L), 123L);
        Assert.assertEquals(CommonUtils.parseLong("12345", 123L), 12345L);
    }

    @Test
    public void testListEquals() {
        List left = new ArrayList();
        List right = new ArrayList();
        Assert.assertTrue(CommonUtils.listEquals(null, null));
        Assert.assertFalse(CommonUtils.listEquals(left, null));
        Assert.assertFalse(CommonUtils.listEquals(null, right));
        Assert.assertTrue(CommonUtils.listEquals(left, right));

        left.add("111");
        left.add("111");
        Assert.assertFalse(CommonUtils.listEquals(left, right));

        right.add("222");
        right.add("111");
        Assert.assertFalse(CommonUtils.listEquals(left, right));

        left.remove("111");
        left.add("222");
        Assert.assertTrue(CommonUtils.listEquals(left, right));
    }
}