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

import com.alipay.sofa.registry.server.test.TestRegistryMain;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhanggeng on 2017/7/14.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
public class SofaRegistryTest {

    private static RegistryConfig    registryConfig;

    private static SofaRegistry      registry;

    private static ConsumerConfig<?> consumer1;
    private static ConsumerConfig<?> consumer2;

    private static ServerConfig      serverConfig1;
    private static ServerConfig      serverConfig2;

    private static ProviderConfig<?> provider;

    private static TestRegistryMain  registryMain;

    @BeforeClass
    public static void beforeClass() {
        registryMain = new TestRegistryMain();
        try {
            registryMain.startRegistry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setUp() {

        registryConfig = new RegistryConfig()
            .setProtocol("sofa")
            .setSubscribe(true)
            .setRegister(true)
            .setAddress("127.0.0.1:9603");

        registry = (SofaRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();
        Assert.assertTrue(registry.start());
    }

    @After
    public void tearDown() {
        registry.destroy();
        registry = null;

        if (consumer1 != null) {
            consumer1.unRefer();
        }
        if (consumer2 != null) {
            consumer2.unRefer();
        }
        if (serverConfig1 != null) {
            serverConfig1.destroy();
        }
        if (serverConfig2 != null) {
            serverConfig2.destroy();
        }
        if (provider != null) {
            provider.unExport();
        }
    }

    @AfterClass
    public static void afterClass() {
        try {
            registryMain.stopRegistry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAll() throws Exception {

        int timeoutPerSub = 5000;

        serverConfig1 = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12200);

        provider = new ProviderConfig();
        provider.setInterfaceId("com.alipay.xxx.TestService")
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setRegister(true)
            .setRegistry(registryConfig)
            .setSerialization("hessian2")
            .setServer(serverConfig1)
            .setWeight(222)
            .setTimeout(3000);

        // 注册
        registry.register(provider);

        consumer1 = new ConsumerConfig();
        consumer1.setInterfaceId("com.alipay.xxx.TestService")
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);

        String tag0 = SofaRegistryHelper.buildListDataId(provider, serverConfig1.getProtocol());
        String tag1 = SofaRegistryHelper.buildListDataId(consumer1, consumer1.getProtocol());
        Assert.assertEquals(tag1, tag0);

        // 订阅
        CountDownLatch latch = new CountDownLatch(2);
        MockProviderInfoListener providerInfoListener = new MockProviderInfoListener();
        providerInfoListener.setCountDownLatch(latch);
        consumer1.setProviderInfoListener(providerInfoListener);
        registry.subscribe(consumer1);
        latch.await(timeoutPerSub, TimeUnit.MILLISECONDS);
        Map<String, ProviderGroup> ps = providerInfoListener.getData();

        Assert.assertTrue(ps.size() > 0);
        Assert.assertNotNull(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertTrue(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size() > 0);

        // 反注册
        latch = new CountDownLatch(1);
        providerInfoListener.setCountDownLatch(latch);
        registry.unRegister(provider);
        latch.await(timeoutPerSub, TimeUnit.MILLISECONDS);
        Assert.assertEquals(ps.toString(), 1, ps.size());
        Assert.assertEquals(0, ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size());

        // 一次发2个端口的再次注册
        latch = new CountDownLatch(2);
        providerInfoListener.setCountDownLatch(latch);
        serverConfig2 = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12201);
        provider.getServer().add(serverConfig2);
        registry.register(provider);
        latch.await(timeoutPerSub * 2, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ps.size() > 0);
        Assert.assertNotNull(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertEquals(2, ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size());

        // 重复订阅
        consumer2 = new ConsumerConfig();
        consumer2.setInterfaceId("com.alipay.xxx.TestService")
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
        registry.subscribe(consumer2);
        latch2.await(timeoutPerSub * 2, TimeUnit.MILLISECONDS);

        Map<String, ProviderGroup> ps2 = providerInfoListener2.getData();
        Assert.assertTrue(ps2.size() > 0);
        Assert.assertNotNull(ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertEquals(2, ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size());
        Assert.assertEquals(1, registry.subscribers.size());
        Assert.assertEquals(1, registry.configurators.size());
        // 取消订阅者1
        registry.unSubscribe(consumer1);
        SofaRegistrySubscribeCallback callback = (SofaRegistrySubscribeCallback) registry.subscribers.get(tag1)
            .getDataObserver();
        Assert.assertFalse(callback.providerInfoListeners.contains(consumer1));
        Assert.assertEquals(1, registry.subscribers.size());
        Assert.assertEquals(1, registry.configurators.size());

        // 批量反注册，判断订阅者2的数据
        latch = new CountDownLatch(2);
        providerInfoListener2.setCountDownLatch(latch);
        List<ProviderConfig> providerConfigList = new ArrayList<ProviderConfig>();
        providerConfigList.add(provider);
        registry.batchUnRegister(providerConfigList);

        latch.await(timeoutPerSub * 2, TimeUnit.MILLISECONDS);
        Assert.assertEquals(ps2.toString(), 1, ps2.size());
        Assert.assertEquals(0, ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size());

        Assert.assertEquals(1, registry.subscribers.size()); // 1个服务 订阅服务列表和服务配置 2个dataId
        Assert.assertEquals(1, registry.configurators.size());

        // 批量取消订阅
        List<ConsumerConfig> consumerConfigList = new ArrayList<ConsumerConfig>();
        consumerConfigList.add(consumer2);
        registry.batchUnSubscribe(consumerConfigList);

        Assert.assertEquals(0, registry.subscribers.size());
        Assert.assertEquals(0, registry.configurators.size());
    }

    @Test
    public void testSubTwice() throws Exception {
        int timeoutPerSub = 5000;

        consumer1 = new ConsumerConfig();
        consumer1.setInterfaceId("com.alipay.xxx.TestService")
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);

        // 订阅
        CountDownLatch latch = new CountDownLatch(2);
        MockProviderInfoListener providerInfoListener = new MockProviderInfoListener();
        providerInfoListener.setCountDownLatch(latch);
        consumer1.setProviderInfoListener(providerInfoListener);
        registry.subscribe(consumer1);
        latch.await(timeoutPerSub, TimeUnit.MILLISECONDS);
        Map<String, ProviderGroup> ps = providerInfoListener.getData();

        Assert.assertTrue(ps.size() > 0);
        Assert.assertNotNull(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertEquals(0, ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size());

        // 重复订阅
        consumer2 = new ConsumerConfig();
        consumer2.setInterfaceId("com.alipay.xxx.TestService")
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
        registry.subscribe(consumer2);
        latch2.await(timeoutPerSub, TimeUnit.MILLISECONDS);

        Map<String, ProviderGroup> ps2 = providerInfoListener2.getData();
        Assert.assertTrue(ps2.size() > 0);
        Assert.assertNotNull(ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertEquals(0, ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size());
        Assert.assertEquals(1, registry.subscribers.size());
        Assert.assertEquals(1, registry.configurators.size());

        latch.await(timeoutPerSub * 2, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ps2.size() > 0);
        Assert.assertNotNull(ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertEquals(0, ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size());
        Assert.assertEquals(1, registry.subscribers.size()); // 1个服务 订阅服务列表和服务配置 2个dataId
        Assert.assertEquals(1, registry.configurators.size());

    }

    private static class MockProviderInfoListener implements ProviderInfoListener {

        Map<String, ProviderGroup> ps = new HashMap<String, ProviderGroup>();

        private CountDownLatch     countDownLatch;

        public void setCountDownLatch(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void addProvider(ProviderGroup providerGroup) {

        }

        @Override
        public void removeProvider(ProviderGroup providerGroup) {

        }

        @Override
        public void updateProviders(ProviderGroup providerGroup) {

            ps.put(providerGroup.getName(), providerGroup);
            if (countDownLatch != null) {
                countDownLatch.countDown();
                countDownLatch = null;
            }
        }

        @Override
        public void updateAllProviders(List<ProviderGroup> providerGroups) {
            ps.clear();

            if (providerGroups == null || providerGroups.size() == 0) {
            } else {
                for (ProviderGroup p : providerGroups) {
                    ps.put(p.getName(), p);
                }

            }
        }

        public Map<String, ProviderGroup> getData() {
            return ps;
        }
    }
}