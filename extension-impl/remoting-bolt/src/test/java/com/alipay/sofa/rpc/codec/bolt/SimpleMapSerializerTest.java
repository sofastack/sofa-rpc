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
package com.alipay.sofa.rpc.codec.bolt;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SimpleMapSerializerTest {
    @Test
    public void encode() throws Exception {
        SimpleMapSerializer simpleMapSerializer = new SimpleMapSerializer();
        Map<String, String> map = null;
        byte[] bs = simpleMapSerializer.encode(map);
        Assert.assertEquals(bs, null);

        map = new HashMap<String, String>();
        bs = simpleMapSerializer.encode(map);
        Assert.assertEquals(bs, null);

        map.put("1", "2");
        map.put("", "x");
        map.put("a", "");
        map.put("b", null);
        bs = simpleMapSerializer.encode(map);
        Assert.assertEquals(37, bs.length);

        Map<String, String> map1 = simpleMapSerializer.decode(bs);
        Assert.assertNotNull(map1);
        Assert.assertEquals(4, map1.size());
        Assert.assertEquals("2", map1.get("1"));
        Assert.assertEquals("x", map1.get(""));
        Assert.assertEquals("", map1.get("a"));
        Assert.assertEquals("", map1.get("b"));

        map1 = simpleMapSerializer.decode(null);
        Assert.assertNotNull(map1);
        Assert.assertEquals(0, map1.size());

        map1 = simpleMapSerializer.decode(new byte[0]);
        Assert.assertNotNull(map1);
        Assert.assertEquals(0, map1.size());
    }

    @Test
    public void decode() throws Exception {
    }

    @Test
    public void writeString() throws Exception {
    }

    @Test
    public void readString() throws Exception {
    }

    @Test
    public void readInt() throws Exception {
    }

    @Test
    public void testUTF8() throws Exception {
        SimpleMapSerializer mapSerializer = new SimpleMapSerializer();
        String s = "test";
        // utf-8 和 gbk  英文是一样的
        Assert.assertArrayEquals(s.getBytes("UTF-8"), s.getBytes("GBK"));

        Map<String, String> map = new HashMap<String, String>();
        map.put("11", "22");
        map.put("222", "333");
        byte[] bs = mapSerializer.encode(map);
        Map newmap = mapSerializer.decode(bs);
        Assert.assertEquals(map, newmap);

        // 支持中文
        map.put("弄啥呢", "咋弄呢？");
        bs = mapSerializer.encode(map);
        newmap = mapSerializer.decode(bs);
        Assert.assertEquals(map, newmap);
    }

}