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

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.registry.zk.base.BaseZkTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ZookeeperRegistryTest extends BaseZkTest {

    private static final String      TEST_SERVICE_NAME = "com.alipay.xxx.ZookeeperTestService";

    private static RegistryConfig    registryConfig;

    private static ZookeeperRegistry registry;

    @BeforeClass
    public static void setUp() {
        registryConfig = new RegistryConfig()
            .setProtocol(RpcConstants.REGISTRY_PROTOCOL_ZK)
            .setSubscribe(true)
            .setAddress("127.0.0.1:2181")
            .setRegister(true);

        registry = (ZookeeperRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();
        Assert.assertTrue(registry.start());
    }

    @AfterClass
    public static void tearDown() {
        registry.destroy();
        registry = null;
    }

    /**
     * 测试Zookeeper Provider Observer
     *
     * @throws Exception
     */
    @Test
    public void testProviderObserver() throws Exception {

        int timeoutPerSub = 2000;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12200);

        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId(TEST_SERVICE_NAME)
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setRegister(true)
            .setRegistry(registryConfig)
            .setSerialization("hessian2")
            .setServer(serverConfig)
            .setWeight(222)
            .setTimeout(3000);

        // 注册
        registry.register(provider);

        ConsumerConfig<?> consumer = new ConsumerConfig();
        consumer.setInterfaceId(TEST_SERVICE_NAME)
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);

        // 订阅
        CountDownLatch latch = new CountDownLatch(1);
        MockProviderInfoListener providerInfoListener = new MockProviderInfoListener();
        providerInfoListener.setCountDownLatch(latch);
        consumer.setProviderInfoListener(providerInfoListener);
        List<ProviderGroup> all = registry.subscribe(consumer);
        providerInfoListener.updateAllProviders(all);
        Map<String, ProviderInfo> ps = providerInfoListener.getData();
        Assert.assertEquals("after register: 1", 1, ps.size());

        // 订阅 错误的uniqueId
        ConsumerConfig<?> consumerNoUniqueId = new ConsumerConfig();
        consumerNoUniqueId.setInterfaceId(TEST_SERVICE_NAME)
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);
        latch = new CountDownLatch(1);
        MockProviderInfoListener providerInfoListener3 = new MockProviderInfoListener();
        providerInfoListener3.setCountDownLatch(latch);
        consumerNoUniqueId.setProviderInfoListener(providerInfoListener3);
        all = registry.subscribe(consumerNoUniqueId);
        providerInfoListener3.updateAllProviders(all);
        Map<String, ProviderInfo> ps3 = providerInfoListener3.getData();
        Assert.assertEquals("wrong uniqueId: 0", 0, ps3.size());

        // 反注册
        latch = new CountDownLatch(1);
        providerInfoListener.setCountDownLatch(latch);
        registry.unRegister(provider);
        latch.await(timeoutPerSub, TimeUnit.MILLISECONDS);
        Assert.assertEquals("after unregister: 0", 0, ps.size());

        // 一次发2个端口的再次注册
        latch = new CountDownLatch(2);
        providerInfoListener.setCountDownLatch(latch);
        provider.getServer().add(new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12201));
        registry.register(provider);
        latch.await(timeoutPerSub * 2, TimeUnit.MILLISECONDS);
        Assert.assertEquals("after register two servers: 2", 2, ps.size());

        // 重复订阅
        ConsumerConfig<?> consumer2 = new ConsumerConfig();
        consumer2.setInterfaceId(TEST_SERVICE_NAME)
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);
        CountDownLatch latch2 = new CountDownLatch(1);
        MockProviderInfoListener providerInfoListener2 = new MockProviderInfoListener();
        providerInfoListener2.setCountDownLatch(latch2);
        consumer2.setProviderInfoListener(providerInfoListener2);
        providerInfoListener2.updateAllProviders(registry.subscribe(consumer2));
        latch2.await(timeoutPerSub, TimeUnit.MILLISECONDS);

        Map<String, ProviderInfo> ps2 = providerInfoListener2.getData();
        Assert.assertEquals("after register duplicate: 2", 2, ps2.size());

        // 取消订阅者1
        registry.unSubscribe(consumer);

        // 批量反注册，判断订阅者2的数据
        latch = new CountDownLatch(2);
        providerInfoListener2.setCountDownLatch(latch);
        List<ProviderConfig> providerConfigList = new ArrayList<ProviderConfig>();
        providerConfigList.add(provider);
        registry.batchUnRegister(providerConfigList);

        latch.await(timeoutPerSub * 2, TimeUnit.MILLISECONDS);
        Assert.assertEquals("after unregister: 0", 0, ps2.size());

        // 批量取消订阅
        List<ConsumerConfig> consumerConfigList = new ArrayList<ConsumerConfig>();
        consumerConfigList.add(consumer2);
        registry.batchUnSubscribe(consumerConfigList);
    }

    /**
     * 测试Zookeeper Config Observer
     *
     * @throws Exception
     */
    @Test
    public void testConfigObserver() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12200);

        ProviderConfig<?> providerConfig = new ProviderConfig();
        providerConfig.setInterfaceId(TEST_SERVICE_NAME)
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setRegister(true)
            .setRegistry(registryConfig)
            .setSerialization("hessian2")
            .setServer(serverConfig)
            .setWeight(222)
            .setTimeout(3000);

        // 注册Provider Config
        registry.register(providerConfig);

        // 订阅Provider Config
        CountDownLatch latch = new CountDownLatch(1);
        MockConfigListener configListener = new MockConfigListener();
        configListener.setCountDownLatch(latch);
        registry.subscribeConfig(providerConfig, configListener);
        configListener.attrUpdated(Collections.singletonMap("timeout", "2000"));
        Map<String, String> configData = configListener.getData();
        Assert.assertEquals(1, configData.size());
        configListener.attrUpdated(Collections.singletonMap("uniqueId", "unique234Id"));
        configData = configListener.getData();
        Assert.assertEquals(2, configData.size());

        ConsumerConfig<?> consumerConfig = new ConsumerConfig();
        consumerConfig.setInterfaceId(TEST_SERVICE_NAME)
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);

        // 订阅Consumer Config
        latch = new CountDownLatch(1);
        configListener = new MockConfigListener();
        configListener.setCountDownLatch(latch);
        registry.subscribeConfig(consumerConfig, configListener);
        configListener.attrUpdated(Collections.singletonMap(RpcConstants.CONFIG_KEY_TIMEOUT, "3333"));
        configData = configListener.getData();
        Assert.assertEquals(1, configData.size());
        configListener.attrUpdated(Collections.singletonMap("uniqueId", "unique234Id"));
        configData = configListener.getData();
        Assert.assertEquals(2, configData.size());

        latch.await(2000, TimeUnit.MILLISECONDS);
        Assert.assertEquals(2, configData.size());

        registry.unRegister(providerConfig);
    }

    /**
     * 测试Zookeeper Override Observer
     *
     * @throws Exception
     */
    @Test
    public void testOverrideObserver() throws InterruptedException {
        ConsumerConfig<?> consumerConfig = new ConsumerConfig();
        consumerConfig.setInterfaceId(TEST_SERVICE_NAME)
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);

        // 订阅Consumer Config
        CountDownLatch latch = new CountDownLatch(1);
        MockConfigListener configListener = new MockConfigListener();
        configListener.setCountDownLatch(latch);
        registry.subscribeOverride(consumerConfig, configListener);
        Map<String, String> attributes = new ConcurrentHashMap<String, String>();
        attributes.put(RpcConstants.CONFIG_KEY_TIMEOUT, "3333");
        attributes.put(RpcConstants.CONFIG_KEY_APP_NAME, "test-server");
        attributes.put(RpcConstants.CONFIG_KEY_SERIALIZATION, "java");
        configListener.attrUpdated(attributes);
        Map<String, String> configData = configListener.getData();
        Assert.assertEquals(3, configData.size());

        consumerConfig.setInterfaceId(TEST_SERVICE_NAME)
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server1"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(5555);
        configListener = new MockConfigListener();
        configListener.setCountDownLatch(latch);
        registry.subscribeOverride(consumerConfig, configListener);
        attributes.put(RpcConstants.CONFIG_KEY_TIMEOUT, "4444");
        attributes.put(RpcConstants.CONFIG_KEY_APP_NAME, "test-server2");
        configListener.attrUpdated(attributes);
        configData = configListener.getData();
        Assert.assertEquals(3, configData.size());

        latch.await(2000, TimeUnit.MILLISECONDS);
        Assert.assertEquals(3, configData.size());
    }

    private static class MockProviderInfoListener implements ProviderInfoListener {

        ConcurrentMap<String, ProviderInfo> ps = new ConcurrentHashMap<String, ProviderInfo>();

        private CountDownLatch              countDownLatch;

        public void setCountDownLatch(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void addProvider(ProviderGroup providerGroup) {
            for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                ps.put(providerInfo.getHost() + ":" + providerInfo.getPort(), providerInfo);
            }
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

        @Override
        public void removeProvider(ProviderGroup providerGroup) {
            for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                ps.remove(providerInfo.getHost() + ":" + providerInfo.getPort());
            }
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

        @Override
        public void updateProviders(ProviderGroup providerGroup) {
            ps.clear();
            for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                ps.put(providerInfo.getHost() + ":" + providerInfo.getPort(), providerInfo);
            }
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

        @Override
        public void updateAllProviders(List<ProviderGroup> providerGroups) {
            ps.clear();
            for (ProviderGroup providerGroup : providerGroups) {
                for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                    ps.put(providerInfo.getHost() + ":" + providerInfo.getPort(), providerInfo);
                }
            }
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

        public Map<String, ProviderInfo> getData() {
            return ps;
        }
    }

    private static class MockConfigListener implements ConfigListener {

        ConcurrentMap<String, String> concurrentHashMap = new ConcurrentHashMap<String, String>();

        private CountDownLatch        countDownLatch;

        public void setCountDownLatch(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void configChanged(Map newValue) {
        }

        @Override
        public void attrUpdated(Map newValue) {
            for (Object property : newValue.keySet()) {
                concurrentHashMap.put(StringUtils.toString(property), StringUtils.toString(newValue.get(property)));
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                }
            }
        }

        public Map<String, String> getData() {
            return concurrentHashMap;
        }
    }

}