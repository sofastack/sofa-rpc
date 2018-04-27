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
public class RoundRobinLoadBalancerTest extends BaseLoadBalancerTest {
    @Test
    public void doSelect() throws Exception {

        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(null);

        Map<Integer, Integer> cnt = new HashMap<Integer, Integer>();
        int size = 20;
        int total = 190000;
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
            System.out.println("elapsed" + (end - start) + "ms");
            System.out.println("avg " + (end - start) * 1000 * 1000 / total + "ns");

            int avg = total / size;
            for (int i = 0; i < size; i++) {
                Assert.assertTrue(avg == cnt.get(9000 + i));
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
            System.out.println("elapsed" + (end - start) + "ms");
            System.out.println("avg " + (end - start) * 1000 * 1000 / total + "ns");

            // 忽略了权重
            int avg = total / size;
            for (int i = 0; i < size; i++) {
                Assert.assertTrue(avg == cnt.get(9000 + i));
            }
        }

    }

    public static void main(String[] args) {
        int x, y, temp;
        int a[] = { 12, 60, 160, 64, 80 };
        temp = a[0];
        for (int i = 0; i < a.length - 1; i++) {
            x = temp;
            y = a[i + 1];
            temp = gcd(x, y);
        }
        System.out.println(temp);
    }

    public static int gcd(int a, int b) {
        int temp;
        while (b != 0) {
            temp = a % b;
            a = b;
            b = temp;
            System.out.println("gcd(" + a + ", " + b + ")=");
        }
        System.out.println(a);
        return a;
    }
}