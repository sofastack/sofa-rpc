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
import com.alipay.sofa.rpc.common.SystemInfo;
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
public class LocalPreferenceLoadBalancerTest extends BaseLoadBalancerTest {
    @Test
    public void doSelect() throws Exception {

        LocalPreferenceLoadBalancer loadBalancer = new LocalPreferenceLoadBalancer(null);

        Map<Integer, Integer> cnt = new HashMap<Integer, Integer>();
        int size = 20;
        int total = 100000;
        SofaRequest request = new SofaRequest();
        {
            for (int i = 0; i < size; i++) {
                cnt.put(9000 + i, 0);
            }
            List<ProviderInfo> providers = buildSameWeightProviderList(size);
            int localps = 5;
            for (int i = 0; i < localps; i++) {
                ProviderInfo localProvider = new ProviderInfo();
                localProvider.setHost(SystemInfo.getLocalHost());
                localProvider.setPort(22000 + i);
                providers.add(localProvider);
                cnt.put(22000 + i, 0);
            }

            long start = System.currentTimeMillis();
            for (int i = 0; i < total; i++) {
                ProviderInfo provider = loadBalancer.doSelect(request, providers);
                int port = provider.getPort();
                cnt.put(port, cnt.get(port) + 1);
            }
            long end = System.currentTimeMillis();
            LOGGER.info("elapsed" + (end - start) + "ms");
            LOGGER.info("avg " + (end - start) * 1000 * 1000 / total + "ns");

            for (int i = 0; i < size; i++) {
                Assert.assertTrue(cnt.get(9000 + i) == 0);
            }
            int avg = total / localps;
            for (int i = 0; i < localps; i++) {
                Assert.assertTrue(avg * 0.9 < cnt.get(22000 + i)
                    && avg * 1.1 > cnt.get(22000 + i)); // 随机偏差不会太大，应该不超过10%
            }
        }

        {
            for (int i = 0; i < size; i++) {
                cnt.put(9000 + i, 0);
            }
            List<ProviderInfo> providers = buildDiffWeightProviderList(size);

            int localps = 5;
            for (int i = 0; i < localps; i++) {
                ProviderInfo localProvider = new ProviderInfo();
                localProvider.setHost(SystemInfo.getLocalHost());
                localProvider.setPort(22000 + i);
                localProvider.setWeight(i * 100);
                providers.add(localProvider);
                cnt.put(22000 + i, 0);
            }

            long start = System.currentTimeMillis();
            for (int i = 0; i < total; i++) {
                ProviderInfo provider = loadBalancer.doSelect(request, providers);
                int port = provider.getPort();
                cnt.put(port, cnt.get(port) + 1);
            }
            long end = System.currentTimeMillis();
            LOGGER.info("elapsed" + (end - start) + "ms");
            LOGGER.info("avg " + (end - start) * 1000 * 1000 / total + "ns");

            for (int i = 0; i < size; i++) {
                Assert.assertTrue(cnt.get(9000 + i) == 0);
            }

            int count = 0;
            for (int i = 0; i < localps; i++) {
                count += i;
            }
            int per = total / count;
            Assert.assertTrue(cnt.get(22000) == 0);
            for (int i = 1; i < localps; i++) {
                Assert.assertTrue(per * i * 0.9 < cnt.get(22000 + i)
                    && per * i * 1.1 > cnt.get(22000 + i)); // 随机偏差不会太大，应该不超过10%
            }
        }
    }

}