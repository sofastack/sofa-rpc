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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProviderInfoTest {
    @Test
    public void valueOf() throws Exception {
    }

    @Test
    public void testWarmUp() throws InterruptedException {

        //effect
        long now = System.currentTimeMillis();
        String src = "bolt://10.15.233.114:12200?weight=123&warmupTime=5000&warmupWeight=300&startTime=" + now +
            "&serialization=hessian2";
        ProviderInfo providerInfo = ProviderInfo.valueOf(src);

        Assert.assertEquals(now + "", providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_START_TIME));
        Assert.assertEquals(300, providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));
        Assert.assertEquals(5000L, providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME));
        Assert.assertEquals(now + 5000, providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME));

        Assert.assertEquals(ProviderStatus.WARMING_UP, providerInfo.getStatus());
        Assert.assertEquals(300, providerInfo.getWeight());

        Thread.sleep(3000);
        Assert.assertEquals(ProviderStatus.WARMING_UP, providerInfo.getStatus());
        Assert.assertEquals(300, providerInfo.getWeight());

        Thread.sleep(2000);
        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo.getStatus());
        Assert.assertEquals(123, providerInfo.getWeight());

        //no warmupTime
        long now2 = System.currentTimeMillis();
        String src2 = "bolt://10.15.233.114:12200?weight=240&warmupWeight=600&startTime=" + now2 +
            "&serialization=hessian2";
        ProviderInfo providerInfo2 = ProviderInfo.valueOf(src2);

        Assert.assertEquals(now2 + "", providerInfo2.getStaticAttr(ProviderInfoAttrs.ATTR_START_TIME));
        Assert.assertEquals(600, providerInfo2.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));
        Assert.assertEquals(null, providerInfo2.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME));
        Assert.assertEquals(null, providerInfo2.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME));

        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo2.getStatus());
        Assert.assertEquals(240, providerInfo2.getWeight());

        Thread.sleep(3000);
        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo2.getStatus());
        Assert.assertEquals(240, providerInfo2.getWeight());

        Thread.sleep(2000);
        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo2.getStatus());
        Assert.assertEquals(240, providerInfo2.getWeight());

        //no warmupWeight
        long now3 = System.currentTimeMillis();
        String src3 = "bolt://10.15.233.114:12200?weight=360&warmupTime=5000&startTime=" + now3 +
            "&serialization=hessian2";
        ProviderInfo providerInfo3 = ProviderInfo.valueOf(src3);

        Assert.assertEquals(now3 + "", providerInfo3.getStaticAttr(ProviderInfoAttrs.ATTR_START_TIME));
        Assert.assertEquals(null, providerInfo3.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));
        Assert.assertEquals(5000L, providerInfo3.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME));
        Assert.assertEquals(null, providerInfo3.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME));

        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo3.getStatus());
        Assert.assertEquals(360, providerInfo3.getWeight());

        Thread.sleep(3000);
        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo3.getStatus());
        Assert.assertEquals(360, providerInfo3.getWeight());

        Thread.sleep(2000);
        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo3.getStatus());
        Assert.assertEquals(360, providerInfo3.getWeight());
    }

    @Test
    public void toUrl() throws Exception {
        {
            String src = "10.15.233.114:12200";
            ProviderInfo providerInfo = ProviderInfo.valueOf(src);
            Assert.assertEquals(providerInfo.getHost(), "10.15.233.114");
            Assert.assertEquals(providerInfo.getPort(), 12200);
            Assert.assertEquals(providerInfo.getPath(), StringUtils.EMPTY);
            Assert.assertEquals(providerInfo.toUrl(), providerInfo.getProtocolType() + "://" + src);
        }
        {
            String src = "10.15.233.114:12200/";
            ProviderInfo providerInfo = ProviderInfo.valueOf(src);
            Assert.assertEquals(providerInfo.getHost(), "10.15.233.114");
            Assert.assertEquals(providerInfo.getPort(), 12200);
            Assert.assertEquals(providerInfo.getPath(), StringUtils.CONTEXT_SEP);
            Assert.assertEquals(providerInfo.toUrl(), providerInfo.getProtocolType() + "://" + src);
        }
        {
            String src = "bolt://10.15.233.114:12200";
            ProviderInfo providerInfo = ProviderInfo.valueOf(src);
            Assert.assertEquals(providerInfo.getProtocolType(), "bolt");
            Assert.assertEquals(providerInfo.getHost(), "10.15.233.114");
            Assert.assertEquals(providerInfo.getPort(), 12200);
            Assert.assertEquals(providerInfo.getPath(), StringUtils.EMPTY);
            Assert.assertEquals(providerInfo.toUrl(), src);
        }
        {
            String src = "bolt://10.15.233.114:12200/";
            ProviderInfo providerInfo = ProviderInfo.valueOf(src);
            Assert.assertEquals(providerInfo.getProtocolType(), "bolt");
            Assert.assertEquals(providerInfo.getHost(), "10.15.233.114");
            Assert.assertEquals(providerInfo.getPort(), 12200);
            Assert.assertEquals(providerInfo.getPath(), StringUtils.CONTEXT_SEP);
            Assert.assertEquals(providerInfo.toUrl(), src);
        }
        {
            String src = "bolt://10.15.233.114:12200?weight=222&serialization=hessian2";
            ProviderInfo providerInfo = ProviderInfo.valueOf(src);
            Assert.assertEquals(providerInfo.getProtocolType(), "bolt");
            Assert.assertEquals(providerInfo.getHost(), "10.15.233.114");
            Assert.assertEquals(providerInfo.getPort(), 12200);
            Assert.assertEquals(providerInfo.getPath(), StringUtils.EMPTY);
            Assert.assertEquals(providerInfo.getWeight(), 222);
            Assert.assertEquals(providerInfo.getSerializationType(), "hessian2");
            Assert.assertEquals(ProviderInfo.valueOf(providerInfo.toUrl()), providerInfo);
        }
        {
            String src = "bolt://10.15.233.114:12200/?weight=222&serialization=hessian2";
            ProviderInfo providerInfo = ProviderInfo.valueOf(src);
            Assert.assertEquals(providerInfo.getProtocolType(), "bolt");
            Assert.assertEquals(providerInfo.getHost(), "10.15.233.114");
            Assert.assertEquals(providerInfo.getPort(), 12200);
            Assert.assertEquals(providerInfo.getPath(), StringUtils.CONTEXT_SEP);
            Assert.assertEquals(providerInfo.getWeight(), 222);
            Assert.assertEquals(providerInfo.getSerializationType(), "hessian2");
            Assert.assertEquals(ProviderInfo.valueOf(providerInfo.toUrl()), providerInfo);
        }
    }

    @Test
    public void testEquals() throws Exception {

        ProviderInfo p1 = new ProviderInfo();
        ProviderInfo p2 = new ProviderInfo();

        Assert.assertEquals(p1, p2);

        List<ProviderInfo> ps = new ArrayList<ProviderInfo>();
        ps.add(p1);
        ps.remove(p2);
        Assert.assertEquals(ps.size(), 0);

        p1.setHost("127.0.0.1");
        Assert.assertFalse(p1.equals(p2));
        p2.setHost("127.0.0.2");
        Assert.assertFalse(p1.equals(p2));
        p2.setHost("127.0.0.1");
        Assert.assertTrue(p1.equals(p2));

        p1.setPort(12200);
        Assert.assertFalse(p1.equals(p2));
        p2.setPort(12201);
        Assert.assertFalse(p1.equals(p2));
        p2.setPort(12200);
        Assert.assertTrue(p1.equals(p2));

        p1.setRpcVersion(4420);
        Assert.assertFalse(p1.equals(p2));
        p2.setRpcVersion(4421);
        Assert.assertFalse(p1.equals(p2));
        p2.setRpcVersion(4420);
        Assert.assertTrue(p1.equals(p2));

        p1.setProtocolType("p1");
        Assert.assertFalse(p1.equals(p2));
        p2.setProtocolType("p2");
        Assert.assertFalse(p1.equals(p2));
        p2.setProtocolType("p1");
        Assert.assertTrue(p1.equals(p2));

        p1.setSerializationType("zzz");
        Assert.assertFalse(p1.equals(p2));
        p2.setSerializationType("yyy");
        Assert.assertFalse(p1.equals(p2));
        p2.setSerializationType("zzz");
        Assert.assertTrue(p1.equals(p2));

        //        p1.setInterfaceId("com.xxx");
        //        Assert.assertFalse(p1.equals(p2));
        //        p2.setInterfaceId("com.yyy");
        //        Assert.assertFalse(p1.equals(p2));
        //        p2.setInterfaceId("com.xxx");
        //        Assert.assertTrue(p1.equals(p2));
        //
        //        p1.setUniqueId("u1");
        //        Assert.assertFalse(p1.equals(p2));
        //        p2.setUniqueId("u2");
        //        Assert.assertFalse(p1.equals(p2));
        //        p2.setUniqueId("u1");
        //        Assert.assertTrue(p1.equals(p2));

        p1.setPath("/aaa");
        Assert.assertFalse(p1.equals(p2));
        p2.setPath("/bbb");
        Assert.assertFalse(p1.equals(p2));
        p2.setPath("/aaa");
        Assert.assertTrue(p1.equals(p2));

        p1.setWeight(200);
        Assert.assertTrue(p1.equals(p2));
        p2.setWeight(300);
        Assert.assertTrue(p1.equals(p2));
        p2.setWeight(200);
        Assert.assertTrue(p1.equals(p2));

        p1.setDynamicAttr("x1", "y1");
        Assert.assertTrue(p1.equals(p2));
        p2.setDynamicAttr("x1", "y1");
        Assert.assertTrue(p1.equals(p2));
        p2.setDynamicAttr("x2", "y2");
        Assert.assertTrue(p1.equals(p2));

        p1.setStaticAttr("x1", "y1");
        Assert.assertTrue(p1.equals(p2));
        p2.setStaticAttr("x1", "y1");
        Assert.assertTrue(p1.equals(p2));
        p1.setStaticAttr("x2", "y2");
        Assert.assertTrue(p1.equals(p2));
        p2.setStaticAttr("x2", "y2");
        Assert.assertTrue(p1.equals(p2));

        ps.add(p1);
        ps.remove(p2);
        Assert.assertEquals(ps.size(), 0);
    }

    @Test
    public void testHashCode() throws Exception {
        ProviderInfo p1 = new ProviderInfo();
        ProviderInfo p2 = new ProviderInfo();

        p1.setHost("127.0.0.1");
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setHost("127.0.0.2");
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setHost("127.0.0.1");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        p1.setPort(12200);
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setPort(12201);
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setPort(12200);
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        p1.setRpcVersion(4420);
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setRpcVersion(4421);
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setRpcVersion(4420);
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        p1.setProtocolType("p1");
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setProtocolType("p2");
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setProtocolType("p1");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        p1.setSerializationType("zzz");
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setSerializationType("yyy");
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setSerializationType("zzz");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        //        p1.setInterfaceId("com.xxx");
        //        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        //        p2.setInterfaceId("com.yyy");
        //        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        //        p2.setInterfaceId("com.xxx");
        //        Assert.assertTrue(p1.hashCode() == p2.hashCode());
        //
        //        p1.setUniqueId("u1");
        //        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        //        p2.setUniqueId("u2");
        //        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        //        p2.setUniqueId("u1");
        //        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        p1.setPath("/aaa");
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setPath("/bbb");
        Assert.assertFalse(p1.hashCode() == p2.hashCode());
        p2.setPath("/aaa");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        p1.setWeight(200);
        Assert.assertTrue(p1.hashCode() == p2.hashCode());
        p2.setWeight(300);
        Assert.assertTrue(p1.hashCode() == p2.hashCode());
        p2.setWeight(200);
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        p1.setDynamicAttr("x1", "y1");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());
        p2.setDynamicAttr("x1", "y1");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());
        p2.setDynamicAttr("x2", "y2");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        p1.setStaticAttr("x1", "y1");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());
        p2.setStaticAttr("x1", "y1");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());
        p1.setStaticAttr("x2", "y2");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());
        p2.setStaticAttr("x2", "y2");
        Assert.assertTrue(p1.hashCode() == p2.hashCode());

        List<ProviderInfo> ps = new ArrayList<ProviderInfo>();
        ps.add(p1);
        ps.remove(p2);
        Assert.assertEquals(ps.size(), 0);

        Set<ProviderInfo> set = new HashSet<ProviderInfo>();
        set.add(p1);
        set.add(p2);
        Assert.assertEquals(set.size(), 1);
        set.remove(p2);
        Assert.assertEquals(set.size(), 0);

        Map<ProviderInfo, String> map = new HashMap<ProviderInfo, String>();
        map.put(p1, "xx");
        map.put(p2, "yy");
        Assert.assertEquals(map.get(p1), "yy");
        map.remove(p2);
        Assert.assertEquals(map.size(), 0);
    }
}