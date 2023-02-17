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
package com.alipay.sofa.rpc.registry.local;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.struct.ScheduledService;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class DomainRegistryTest {

    private DomainRegistry domainRegistry;

    @Before
    public void before() {
        domainRegistry = new DomainRegistry(new RegistryConfig());
    }

    @After
    public void after() {
        domainRegistry.destroy();
    }

    @Test
    public void testInit() {
        DomainRegistry domainRegistry = new DomainRegistry(new RegistryConfig());
        assertNull(domainRegistry.scheduledExecutorService);
        domainRegistry.init();
        assertTrue(domainRegistry.scheduledExecutorService.isStarted());
    }

    @Test
    public void testInitOnce() {
        DomainRegistry domainRegistry = new DomainRegistry(new RegistryConfig());
        domainRegistry.init();
        ScheduledService origin = domainRegistry.scheduledExecutorService;
        domainRegistry.init();
        assertSame(origin, domainRegistry.scheduledExecutorService);
    }

    @Test
    public void testDirectUrl2IpUrl() {
        ProviderInfo providerInfo = ProviderHelper.toProviderInfo("bolt://alipay.com:12200");
        List<ProviderInfo> providerInfos = domainRegistry.directUrl2IpUrl(providerInfo, null);
        assertTrue(providerInfos.size() > 0);
        String host = providerInfos.get(0).getHost();
        assertNotEquals("alipay.com", host);
        assertFalse(DomainRegistryHelper.isDomain(host));

        ProviderInfo notExist = ProviderHelper.toProviderInfo("bolt://notexist:12200");
        providerInfos = domainRegistry.directUrl2IpUrl(notExist, null);
        assertEquals(1, providerInfos.size());
        host = providerInfos.get(0).getHost();
        assertEquals("notexist", host);

        ProviderInfo ipProviderInfo = ProviderHelper.toProviderInfo("bolt://127.0.0.1:12200");
        providerInfos = domainRegistry.directUrl2IpUrl(ipProviderInfo, null);
        assertEquals(1, providerInfos.size());
        host = providerInfos.get(0).getHost();
        assertEquals("127.0.0.1", host);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRegister() {
        domainRegistry.register(new ProviderConfig<>());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnRegister() {
        domainRegistry.unRegister(new ProviderConfig<>());
    }

    @Test
    public void testSubscribe() {
        ConsumerConfig<Object> consumerConfig = new ConsumerConfig<>();
        String directUrl = "bolt://alipay.com";
        consumerConfig.setDirectUrl(directUrl);
        List<ProviderGroup> providerGroups = domainRegistry.subscribe(consumerConfig);
        assertTrue(domainRegistry.notifyListeners.containsKey(directUrl));
        assertSame(consumerConfig, domainRegistry.notifyListeners.get(directUrl).get(0));

        assertEquals(1, providerGroups.size());
        ProviderGroup providerGroup = providerGroups.get(0);
        assertEquals(RpcConstants.ADDRESS_DIRECT_GROUP, providerGroup.getName());


        ConsumerConfig<Object> notDirect = new ConsumerConfig<>();
        notDirect.setDirectUrl("");
        domainRegistry.subscribe(notDirect);
        assertFalse(domainRegistry.notifyListeners.containsKey(""));

        ConsumerConfig<Object> ipConfig = new ConsumerConfig<>();
        String ip = "bolt://127.0.0.1";
        ipConfig.setDirectUrl(ip);
        List<ProviderGroup> ipProviderGroup = domainRegistry.subscribe(ipConfig);
        assertTrue(domainRegistry.notifyListeners.containsKey(directUrl));
        assertSame(consumerConfig, domainRegistry.notifyListeners.get(directUrl).get(0));

        assertEquals(1, ipProviderGroup.size());
        ProviderGroup ipGroup = ipProviderGroup.get(0);
        assertEquals(RpcConstants.ADDRESS_DIRECT_GROUP, ipGroup.getName());
        assertEquals(1, ipGroup.getProviderInfos().size());
        assertEquals("127.0.0.1", ipGroup.getProviderInfos().get(0).getHost());
    }

    @Test
    public void testUnSubscribe() {
        ConsumerConfig<Object> consumerConfig = new ConsumerConfig<>();
        String directUrl = "bolt://alipay.com";
        consumerConfig.setDirectUrl(directUrl);
        List<ProviderGroup> providerGroups = domainRegistry.subscribe(consumerConfig);
        assertTrue(domainRegistry.notifyListeners.containsKey(directUrl));
        assertSame(consumerConfig, domainRegistry.notifyListeners.get(directUrl).get(0));
        assertEquals(1, domainRegistry.notifyListeners.get(directUrl).size());

        domainRegistry.unSubscribe(consumerConfig);
        assertTrue(domainRegistry.notifyListeners.containsKey(directUrl));
        assertEquals(0, domainRegistry.notifyListeners.get(directUrl).size());
    }

    @Test
    public void testUnSubscribe2() {
        ConsumerConfig<Object> consumerConfig1 = new ConsumerConfig<>();
        String directUrl = "bolt://alipay.com";
        consumerConfig1.setDirectUrl(directUrl);

        ConsumerConfig<Object> consumerConfig2 = new ConsumerConfig<>();
        consumerConfig2.setDirectUrl(directUrl);

        domainRegistry.subscribe(consumerConfig1);
        domainRegistry.subscribe(consumerConfig2);

        assertTrue(domainRegistry.notifyListeners.containsKey(directUrl));
        assertTrue(domainRegistry.notifyListeners.get(directUrl).contains(consumerConfig1));
        assertTrue(domainRegistry.notifyListeners.get(directUrl).contains(consumerConfig2));
        assertEquals(2, domainRegistry.notifyListeners.get(directUrl).size());

        domainRegistry.unSubscribe(consumerConfig1);
        assertTrue(domainRegistry.notifyListeners.containsKey(directUrl));
        assertEquals(1, domainRegistry.notifyListeners.get(directUrl).size());

        domainRegistry.unSubscribe(consumerConfig2);
        assertEquals(0, domainRegistry.notifyListeners.get(directUrl).size());
    }

    @Test
    public void testBatchUnSubscribe() {
        ConsumerConfig<Object> config1 = new ConsumerConfig<>();
        String direct1 = "2";
        config1.setDirectUrl(direct1);

        ConsumerConfig<Object> config2 = new ConsumerConfig<>();
        String direct2 = "1";
        config2.setDirectUrl(direct2);

        domainRegistry.subscribe(config1);
        domainRegistry.subscribe(config2);

        assertTrue(domainRegistry.notifyListeners.containsKey(direct1));
        assertEquals(1, domainRegistry.notifyListeners.get(direct1).size());
        assertTrue(domainRegistry.notifyListeners.containsKey(direct2));
        assertEquals(1, domainRegistry.notifyListeners.get(direct2).size());


        List<ConsumerConfig> consumerConfigs = Arrays.asList(config1, config2);
        domainRegistry.batchUnSubscribe(consumerConfigs);
        assertTrue(domainRegistry.notifyListeners.containsKey(direct1));
        assertEquals(0, domainRegistry.notifyListeners.get(direct1).size());
        assertTrue(domainRegistry.notifyListeners.containsKey(direct2));
        assertEquals(0, domainRegistry.notifyListeners.get(direct2).size());

    }

    @Test
    public void testDestroy() {
        domainRegistry.init();
        ConsumerConfig<Object> config1 = new ConsumerConfig<>();
        String direct1 = "2";
        config1.setDirectUrl(direct1);
        domainRegistry.subscribe(config1);

        assertTrue(domainRegistry.scheduledExecutorService.isStarted());
        assertTrue(domainRegistry.notifyListeners.containsKey(direct1));
        domainRegistry.destroy();
        assertFalse(domainRegistry.scheduledExecutorService.isStarted());
        assertEquals(0, domainRegistry.notifyListeners.size());
    }

    @Test
    public void testRefreshDomain() {
        ConsumerConfig<Object> consumerConfig = new ConsumerConfig<>();
        String directUrl1 = "bolt://alipay.com";
        String directUrl2 = "bolt://taobao.com";
        String directUrl = directUrl1 + ";" + directUrl2;
        consumerConfig.setDirectUrl(directUrl);
        domainRegistry.subscribe(consumerConfig);

        assertTrue(domainRegistry.notifyListeners.containsKey(directUrl));
        domainRegistry.refreshDomain();

        assertTrue(domainRegistry.domainCache.containsKey(directUrl1));
        assertTrue(domainRegistry.domainCache.containsKey(directUrl2));
    }

    @Test
    public void testNotifyListener() {
        ConsumerConfig<Object> consumerConfig = new ConsumerConfig<>();
        String directUrl1 = "bolt://alipay.com";
        String directUrl2 = "bolt://taobao.com";
        String directUrl = directUrl1 + ";" + directUrl2;
        consumerConfig.setDirectUrl(directUrl);
        MockProviderInfoListener mockProviderInfoListener = new MockProviderInfoListener();
        consumerConfig.setProviderInfoListener(mockProviderInfoListener);


        domainRegistry.subscribe(consumerConfig);

        assertTrue(domainRegistry.notifyListeners.containsKey(directUrl));
        domainRegistry.refreshDomain();

        assertTrue(domainRegistry.domainCache.containsKey(directUrl1));
        assertTrue(domainRegistry.domainCache.containsKey(directUrl2));

        domainRegistry.notifyListener();

        Map<String, List<ProviderInfo>> data = mockProviderInfoListener.getData();
        assertEquals(1, data.size());
        assertTrue(data.containsKey(RpcConstants.ADDRESS_DIRECT_GROUP));
        List<ProviderInfo> providerInfo = data.get(RpcConstants.ADDRESS_DIRECT_GROUP);
        assertTrue(providerInfo.size() > 1);
    }

    @Test
    public void testResolveDomain() {
        String mockKey = "mock";
        List<ProviderInfo> value = new ArrayList<>();
        domainRegistry.domainCache.put(mockKey, value);
        assertSame(value, domainRegistry.resolveDomain(mockKey));

        String local = "127.0.0.1";
        List<ProviderInfo> actual = domainRegistry.resolveDomain(local);
        assertEquals(1, actual.size());
        assertEquals(local, actual.get(0).getHost());
    }

    private static class MockProviderInfoListener implements ProviderInfoListener {

        ConcurrentHashMap<String, List<ProviderInfo>> ps = new ConcurrentHashMap();

        private CountDownLatch                        countDownLatch;

        public void setCountDownLatch(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void addProvider(ProviderGroup providerGroup) {
            ps.put(providerGroup.getName(), providerGroup.getProviderInfos());
        }

        @Override
        public void removeProvider(ProviderGroup providerGroup) {
            ps.remove(providerGroup.getName());
        }

        @Override
        public void updateProviders(ProviderGroup providerGroup) {
            ps.clear();
            ps.put(providerGroup.getName(), providerGroup.getProviderInfos());
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

        @Override
        public void updateAllProviders(List<ProviderGroup> providerGroups) {
            ps.clear();
            for (ProviderGroup providerGroup : providerGroups) {
                ps.put(providerGroup.getName(), providerGroup.getProviderInfos());
            }
        }

        public Map<String, List<ProviderInfo>> getData() {
            return ps;
        }
    }
}