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

import com.alipay.sofa.rpc.common.RpcConstants;
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
    public void test() {
        ProviderInfo provider = ProviderInfo.valueOf("bolt://10.15.232.229:12222");
        Assert.assertEquals("bolt://10.15.232.229:12222", provider.toUrl());
    }

    @Test
    public void testGetWeight() {
        ProviderInfo provider = ProviderHelper
            .toProviderInfo("bolt://10.15.232.229:12222?timeout=3333&serialization=hessian2&connections=1&warmupTime=6&warmupWeight=5&appName=test-server&weight=2000");

        long warmupTime = Long.parseLong(provider.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME));
        provider.setDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT,
            Integer.parseInt(provider.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT)));
        provider.setDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME, warmupTime);
        provider.setDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME, System.currentTimeMillis() + warmupTime);
        provider.setStatus(ProviderStatus.WARMING_UP);

        Assert.assertTrue(RpcConstants.PROTOCOL_TYPE_BOLT.equals(provider.getProtocolType()));
        Assert.assertTrue(RpcConstants.SERIALIZE_HESSIAN2.equals(provider.getSerializationType()));
        Assert.assertTrue("10.15.232.229".equals(provider.getHost()));
        Assert.assertTrue(provider.getPort() == 12222);
        Assert.assertTrue("test-server".equals(provider.getAttr(ProviderInfoAttrs.ATTR_APP_NAME)));
        Assert.assertTrue("1".equals(provider.getAttr(ProviderInfoAttrs.ATTR_CONNECTIONS)));
        Assert.assertEquals("3333", provider.getStaticAttr(ProviderInfoAttrs.ATTR_TIMEOUT));
        Assert.assertEquals(5, provider.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));
        Assert.assertTrue(provider.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME) != null);
        Assert.assertEquals("5", provider.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));
        Assert.assertEquals("6", provider.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME));
        Assert.assertEquals(ProviderStatus.WARMING_UP, provider.getStatus());
        Assert.assertEquals(5, provider.getWeight());
        try {
            Thread.sleep(10);
        } catch (Exception e) {
        }
        Assert.assertTrue(provider.getWeight() == 2000);
        Assert.assertTrue(provider.getStatus() == ProviderStatus.AVAILABLE);
        Assert.assertTrue(provider.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME) == null);
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