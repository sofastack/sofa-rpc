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
package com.alipay.sofa.rpc.registry.sofa;

import com.alipay.sofa.registry.client.api.ConfigDataObserver;
import com.alipay.sofa.registry.client.api.Configurator;
import com.alipay.sofa.registry.client.api.Subscriber;
import com.alipay.sofa.registry.client.api.SubscriberDataObserver;
import com.alipay.sofa.registry.client.api.model.ConfigData;
import com.alipay.sofa.registry.client.api.model.UserData;
import com.alipay.sofa.registry.client.provider.DefaultConfigData;
import com.alipay.sofa.registry.client.provider.DefaultUserData;
import com.alipay.sofa.registry.core.model.ScopeEnum;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhanggeng on 2017/7/15.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
public class SofaRegistrySubscribeCallbackTest {
    @Test
    public void mergeProviderInfo() throws Exception {
        SofaRegistrySubscribeCallback callback = new SofaRegistrySubscribeCallback();
        // null + null
        List<ProviderInfo> result = callback.mergeProviderInfo(null, null);
        Assert.assertTrue(CommonUtils.isEmpty(result));
        // 空 + null
        List<String> listData = new ArrayList<String>();
        result = callback.mergeProviderInfo(listData, null);
        Assert.assertTrue(CommonUtils.isEmpty(result));
        // null + 空
        List<String> attrData = new ArrayList<String>();
        result = callback.mergeProviderInfo(null, attrData);
        Assert.assertTrue(CommonUtils.isEmpty(result));
        // 空+ 空
        result = callback.mergeProviderInfo(listData, attrData);
        Assert.assertTrue(CommonUtils.isEmpty(result));
        // 空+非空
        attrData.add("override://127.0.0.1?weight=200");
        result = callback.mergeProviderInfo(listData, attrData);
        Assert.assertTrue(CommonUtils.isEmpty(result));
        // 非空+空
        attrData.clear();
        listData.add("127.0.0.1:22000?weight=100");
        listData.add("127.0.0.1:22001?weight=100");
        result = callback.mergeProviderInfo(listData, attrData);
        Assert.assertTrue(result.size() == 2);
        for (ProviderInfo providerInfo : result) {
            if (providerInfo.getPort() == 22000) {
                Assert.assertTrue(providerInfo.getWeight() == 100);
            } else if (providerInfo.getPort() == 22001) {
                Assert.assertTrue(providerInfo.getWeight() == 100);
            }
        }
        // 覆盖未命中
        attrData.add("override://127.0.0.1:22005?weight=200");
        attrData.add("override://127.0.0.1:22004?disabled=true&weight=200");
        result = callback.mergeProviderInfo(listData, attrData);
        Assert.assertTrue(result.size() == 2);
        for (ProviderInfo providerInfo : result) {
            if (providerInfo.getPort() == 22000) {
                Assert.assertTrue(providerInfo.getWeight() == 100);
            } else if (providerInfo.getPort() == 22001) {
                Assert.assertTrue(providerInfo.getWeight() == 100);
            }
        }
        // 覆盖
        attrData.add("override://127.0.0.1:22000?weight=200");
        attrData.add("override://127.0.0.1:22001?disabled=true&weight=200");
        result = callback.mergeProviderInfo(listData, attrData);
        Assert.assertTrue(result.size() == 1);
        for (ProviderInfo providerInfo : result) {
            if (providerInfo.getPort() == 22000) {
                Assert.assertTrue(providerInfo.getWeight() == 200);
            }
        }
    }

    @Test
    public void handleData() throws Exception {

        Subscriber listSub = new MockSubscribe(5);
        Configurator attrSub = new MockConfigurator(2);

        final AtomicInteger ps = new AtomicInteger(0);
        ProviderInfoListener listener = new ProviderInfoListener() {
            @Override
            public void addProvider(ProviderGroup providerGroup) {
                ps.addAndGet(providerGroup.size());
            }

            @Override
            public void removeProvider(ProviderGroup providerGroup) {
                ps.addAndGet(-providerGroup.size());
            }

            @Override
            public void updateProviders(ProviderGroup providerGroup) {
                ps.set(providerGroup.size());
            }

            @Override
            public void updateAllProviders(List<ProviderGroup> providerGroups) {
                ps.set(0);
                for (ProviderGroup providerGroup : providerGroups) {
                    ps.addAndGet(providerGroup.size());
                }
            }
        };

        SofaRegistrySubscribeCallback callback = new SofaRegistrySubscribeCallback();
        callback.addProviderInfoListener("xxxxx", new ConsumerConfig(), listener);
        Assert.assertTrue((!callback.flag[0].get()) && (!callback.flag[1].get()));

        callback.handleData("xxxxx", buildConfigPs(2));
        try {
            Thread.sleep(200);
        } finally {
        }
        Assert.assertTrue(callback.flag[1].get());
        Assert.assertTrue(ps.get() == 0);

        callback.handleData("xxxxx", buildPs(5));
        try {
            Thread.sleep(200);
        } finally {
        }
        Assert.assertTrue(callback.flag == null);
        //default+localZone
        Assert.assertEquals(ps.get(), 5 + 5);

        callback = new SofaRegistrySubscribeCallback();
        ps.set(0);
        callback.addProviderInfoListener("yyyyy", new ConsumerConfig(), listener);
        callback.handleData("yyyyy", buildPs(5));
        try {
            Thread.sleep(200);
        } finally {
        }
        callback.handleData("yyyyy", buildConfigPs(2));
        try {
            Thread.sleep(200);
        } finally {
        }
        Assert.assertTrue(callback.flag == null);
        Assert.assertEquals(ps.get(), 5 + 5);

    }

    private UserData buildPs(final int num) {

        UserData userData = new UserData() {
            @Override
            public Map<String, List<String>> getZoneData() {

                Map<String, List<String>> map = new HashMap<String, List<String>>();
                List<String> providerInfos = new ArrayList<String>();
                for (int i = 0; i < num; i++) {
                    providerInfos.add("127.0.0.1:" + (20000 + i));
                }
                map.put("localZone", providerInfos);

                return map;
            }

            @Override
            public String getLocalZone() {
                return "localZone";
            }
        };

        return userData;
    }

    /**
     * build config data
     * @param num
     * @return
     */
    private ConfigData buildConfigPs(final int num) {

        ConfigData userData = new ConfigData() {
            @Override
            public String getData() {
                List<String> providerInfos = new ArrayList<String>();
                for (int i = 0; i < num; i++) {
                    providerInfos.add("127.0.0.1:" + (20000 + i));
                }

                return CommonUtils.join(providerInfos, "#");
            }
        };

        return userData;
    }

    private static class MockSubscribe implements Subscriber {
        List<String> providerInfos = new ArrayList<String>();

        private MockSubscribe(int s) {
            for (int i = 0; i < s; i++) {
                providerInfos.add("127.0.0.1:" + (20000 + i));
            }
        }

        @Override
        public SubscriberDataObserver getDataObserver() {
            return null;
        }

        @Override
        public void setDataObserver(SubscriberDataObserver subscriberDataObserver) {

        }

        @Override
        public UserData peekData() {

            UserData data = new DefaultUserData();
            ((DefaultUserData) data).setLocalZone("zone");
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            map.put("zone", providerInfos);
            ((DefaultUserData) data).setZoneData(map);
            return data;
        }

        @Override
        public ScopeEnum getScopeEnum() {
            return null;
        }

        @Override
        public void reset() {

        }

        @Override
        public boolean isRegistered() {
            return false;
        }

        @Override
        public void unregister() {

        }

        @Override
        public String getDataId() {
            return null;
        }

        @Override
        public String getGroup() {
            return null;
        }

        @Override
        public String getRegistId() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    }

    private static class MockConfigurator implements Configurator {

        List<String> providerInfos = new ArrayList<String>();

        private MockConfigurator(int s) {
            for (int i = 0; i < s; i++) {
                providerInfos.add("127.0.0.1:" + (20000 + i));
            }
        }

        @Override
        public ConfigDataObserver getDataObserver() {
            return null;
        }

        @Override
        public void setDataObserver(ConfigDataObserver configDataObserver) {

        }

        @Override
        public ConfigData peekData() {
            String data = CommonUtils.join(providerInfos, "#");

            final DefaultConfigData defaultConfigData = new DefaultConfigData(data);
            return defaultConfigData;
        }

        @Override
        public void reset() {

        }

        @Override
        public boolean isRegistered() {
            return false;
        }

        @Override
        public void unregister() {

        }

        @Override
        public String getDataId() {
            return null;
        }

        @Override
        public String getGroup() {
            return null;
        }

        @Override
        public String getRegistId() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    }
}