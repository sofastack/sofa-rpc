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
package com.alipay.sofa.rpc.common.struct;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class MapDifferenceTest {
    @Test
    public void testDifferenceMap() {
        Map<String, List<String>> oldmap = new ConcurrentHashMap<String, List<String>>();
        oldmap.put("aaa", Arrays.asList("127.0.0.1:9090", "127.0.0.1:9091", "127.0.0.1:9092"));
        oldmap.put("bbb", Arrays.asList("127.0.0.1:9090", "127.0.0.1:9091", "127.0.0.1:9092"));
        oldmap.put("ccc", Arrays.asList("127.0.0.1:9090", "127.0.0.1:9091", "127.0.0.1:9092"));

        Map<String, List<String>> newmap = new ConcurrentHashMap<String, List<String>>();
        newmap.put("aaa", Arrays.asList("127.0.0.1:9090", "127.0.0.1:9091", "127.0.0.1:9092"));
        newmap.put("bbb", Arrays.asList("127.0.0.1:9090", "127.0.0.1:9092", "127.0.0.1:9093"));
        newmap.put("ddd", Arrays.asList("127.0.0.1:9090", "127.0.0.1:9091", "127.0.0.1:9092"));
        newmap.put("eee", Arrays.asList("127.0.0.1:9090", "127.0.0.1:9091", "127.0.0.1:9092"));
        newmap.put("fff", Arrays.asList("127.0.0.1:9090", "127.0.0.1:9091", "127.0.0.1:9092"));

        MapDifference difference = new MapDifference(oldmap, newmap);
        Assert.assertFalse(difference.areEqual());
        Map<String, List<String>> onlynew = difference.entriesOnlyOnRight();
        Assert.assertTrue(onlynew.containsKey("ddd"));

        Map<String, List<String>> onlyold = difference.entriesOnlyOnLeft();
        Assert.assertTrue(onlyold.containsKey("ccc"));

        Map<String, List<String>> same = difference.entriesInCommon();
        Assert.assertTrue(same.containsKey("aaa"));

        Map<String, ValueDifference<List<String>>> differ = difference.entriesDiffering();
        Assert.assertTrue(differ.containsKey("bbb"));

        for (Map.Entry<String, ValueDifference<List<String>>> entry : differ.entrySet()) {
            ValueDifference<List<String>> differentValue = entry.getValue();
            List<String> inold = differentValue.leftValue();
            List<String> innew = differentValue.rightValue();

            ListDifference<String> listDifference = new ListDifference<String>(inold, innew);
            List<String> adds = listDifference.getOnlyOnRight();
            List<String> removeds = listDifference.getOnlyOnLeft();
            List<String> sames = listDifference.getOnBoth();

            Assert.assertTrue(adds.size() == 1);
            Assert.assertTrue(removeds.size() == 1);
            Assert.assertTrue(sames.size() == 2);

            Assert.assertEquals(adds.get(0), "127.0.0.1:9093");
            Assert.assertEquals(removeds.get(0), "127.0.0.1:9091");
        }
    }
}