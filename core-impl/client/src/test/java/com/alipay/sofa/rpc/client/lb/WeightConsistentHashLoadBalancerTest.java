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
package com.alipay.sofa.rpc.client.lb;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 景竹 2018/8/13 since 5.5.0
 */
public class WeightConsistentHashLoadBalancerTest extends BaseLoadBalancerTest {

    /**
     * 测试相同参数会落到相同的节点
     *
     * @throws Exception
     */
    @Test
    public void doSelect() throws Exception {

        WeightConsistentHashLoadBalancer loadBalancer = new WeightConsistentHashLoadBalancer(null);

        Map<Integer, Integer> cnt = new HashMap<Integer, Integer>(40);
        int size = 20;
        int total = 100000;
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(ConsistentHashLoadBalancerTest.class.getName());
        request.setMethod(ConsistentHashLoadBalancerTest.class.getMethod("doSelect"));
        for (int i = 0; i < size; i++) {
            cnt.put(9000 + i, 0);
        }
        List<ProviderInfo> providers = buildDiffWeightProviderList(size);
        long start = System.currentTimeMillis();
        for (int i = 0; i < total; i++) {
            ProviderInfo provider = loadBalancer.doSelect(request, providers);
            int port = provider.getPort();
            cnt.put(port, cnt.get(port) + 1);
        }
        long end = System.currentTimeMillis();
        System.out.println("elapsed" + (end - start) + "ms");
        System.out.println("avg " + (end - start) * 1000 * 1000 / total + "ns");

        int count = 0;
        for (int i = 0; i < size; i++) {
            if (cnt.get(9000 + i) > 0) {
                count++;
            }
        }
        // 应该落在同一台机器上
        Assert.assertTrue(count == 1);

    }

    /**
     * 测试根据权重数据的分布符合比例
     *
     * @throws Exception
     */
    @Test
    public void testWeight() throws Exception {
        WeightConsistentHashLoadBalancer loadBalancer = new WeightConsistentHashLoadBalancer(null);
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(ConsistentHashLoadBalancerTest.class.getName());
        request.setMethod(ConsistentHashLoadBalancerTest.class.getMethod("doSelect"));
        int size = 20;
        int total = 100000;
        List<ProviderInfo> providers = buildDiffWeightProviderList(size);
        Map<Integer, Integer> map = new HashMap(total * 2);
        for (int i = 0; i < total; i++) {
            request.setMethodArgs(new Object[] { "method" + i });
            ProviderInfo provider = loadBalancer.doSelect(request, providers);
            Integer key = provider.getPort();
            if (map.containsKey(key)) {
                int count = map.get(key);
                map.put(key, ++count);
            } else {
                map.put(key, 0);
            }
        }
        Set<Map.Entry<Integer, Integer>> set = map.entrySet();
        Iterator<Map.Entry<Integer, Integer>> iterator = set.iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int port = entry.getKey() - 9000;
            //最大误差不超过10%
            Assert.assertTrue(entry.getValue() > 500 * port * 0.90);
            Assert.assertTrue(entry.getValue() < 500 * port * 1.10);
        }
    }

}
