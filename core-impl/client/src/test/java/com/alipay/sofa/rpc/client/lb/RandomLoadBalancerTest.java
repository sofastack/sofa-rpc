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
import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RandomLoadBalancerTest extends BaseLoadBalancerTest {
    @Test
    public void doSelect() throws Exception {

        RandomLoadBalancer loadBalancer = new RandomLoadBalancer(null);

        Map<Integer, Integer> cnt = new HashMap<Integer, Integer>();
        int size = 20;
        int total = 100000;
        SofaRequest request = new SofaRequest();
        {
            for (int i = 0; i < size; i++) {
                cnt.put(9000 + i, 0);
            }
            List<ProviderInfo> providers = buildSameWeightProviderList(size);
            long start = System.currentTimeMillis();
            for (int i = 0; i < total; i++) {
                ProviderInfo provider = loadBalancer.doSelect(request, providers);
                int port = provider.getPort();
                cnt.put(port, cnt.get(port) + 1);
            }
            long end = System.currentTimeMillis();
            LOGGER.info("elapsed" + (end - start) + "ms");
            LOGGER.info("avg " + (end - start) * 1000 * 1000 / total + "ns");

            int avg = total / size;
            for (int i = 0; i < size; i++) {
                Assert.assertTrue(avg * 0.9 < cnt.get(9000 + i)
                    && avg * 1.1 > cnt.get(9000 + i)); // 随机偏差不会太大，应该不超过10%
            }
        }

        {
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
            LOGGER.info("elapsed" + (end - start) + "ms");
            LOGGER.info("avg " + (end - start) * 1000 * 1000 / total + "ns");

            Assert.assertTrue(cnt.get(9000) == 0);

            int count = 0;
            for (int i = 0; i < size; i++) {
                count += i;
            }
            int per = total / count;
            for (int i = 1; i < size; i++) {
                Assert.assertTrue(per * i * 0.85 < cnt.get(9000 + i)
                    && per * i * 1.15 > cnt.get(9000 + i)); // 随机偏差不会太大，应该不超过15%
            }
        }
    }
}