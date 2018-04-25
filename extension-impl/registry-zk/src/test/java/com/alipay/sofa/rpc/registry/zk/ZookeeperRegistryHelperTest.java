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
package com.alipay.sofa.rpc.registry.zk;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.client.ProviderStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liengen</a>
 * @version $Id: ZookeeperRegistryHelperTest.java, v 0.1 2018年04月25日 下午7:09 LiWei.Liengen Exp $
 */
public class ZookeeperRegistryHelperTest {

    @Test
    public void testWarmup() throws UnsupportedEncodingException, InterruptedException {

        long now = System.currentTimeMillis();

        ProviderInfo providerInfo = new ProviderInfo()
            .setWeight(200)
            .setStaticAttr(ProviderInfoAttrs.ATTR_WEIGHT, "200")
            .setStaticAttr(ProviderInfoAttrs.ATTR_START_TIME, String.valueOf(now))
            .setStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME, String.valueOf(30))
            .setStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT, String.valueOf(700));

        ZookeeperRegistryHelper.processWarmUpWeight(providerInfo);

        Assert.assertEquals("200", providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WEIGHT));
        Assert.assertEquals(now + "", providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_START_TIME));
        Assert.assertEquals(null, providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME));
        Assert.assertEquals(null, providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));

        Assert.assertEquals(now + 30, providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME));
        Assert.assertEquals(700, providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));
        Assert.assertEquals(ProviderStatus.WARMING_UP, providerInfo.getStatus());
        Assert.assertEquals(700, providerInfo.getWeight());

        Thread.sleep(50);

        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo.getStatus());
        Assert.assertEquals(200, providerInfo.getWeight());
    }

    @Test
    public void testNoWarmupTime() throws InterruptedException {
        long now = System.currentTimeMillis();

        ProviderInfo providerInfo = new ProviderInfo()
            .setWeight(300)
            .setStaticAttr(ProviderInfoAttrs.ATTR_WEIGHT, "300")
            .setStaticAttr(ProviderInfoAttrs.ATTR_START_TIME, String.valueOf(now))
            .setStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT, String.valueOf(800));

        ZookeeperRegistryHelper.processWarmUpWeight(providerInfo);

        Assert.assertEquals("300", providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WEIGHT));
        Assert.assertEquals(now + "", providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_START_TIME));
        Assert.assertEquals(null, providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME));
        Assert.assertEquals(null, providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));

        Assert.assertEquals(null, providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME));
        Assert.assertEquals(null, providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));
        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo.getStatus());
        Assert.assertEquals(300, providerInfo.getWeight());

        Thread.sleep(30);

        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo.getStatus());
        Assert.assertEquals(300, providerInfo.getWeight());
    }

    @Test
    public void testNoWarmupWeight() throws InterruptedException {
        long now = System.currentTimeMillis();

        ProviderInfo providerInfo = new ProviderInfo()
            .setWeight(600)
            .setStaticAttr(ProviderInfoAttrs.ATTR_WEIGHT, "600")
            .setStaticAttr(ProviderInfoAttrs.ATTR_START_TIME, String.valueOf(now))
            .setStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME, String.valueOf(30));

        ZookeeperRegistryHelper.processWarmUpWeight(providerInfo);

        Assert.assertEquals("600", providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WEIGHT));
        Assert.assertEquals(now + "", providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_START_TIME));
        Assert.assertEquals(null, providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME));
        Assert.assertEquals(null, providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));

        Assert.assertEquals(null, providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME));
        Assert.assertEquals(null, providerInfo.getDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT));
        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo.getStatus());
        Assert.assertEquals(600, providerInfo.getWeight());

        Thread.sleep(30);

        Assert.assertEquals(ProviderStatus.AVAILABLE, providerInfo.getStatus());
        Assert.assertEquals(600, providerInfo.getWeight());
    }
}