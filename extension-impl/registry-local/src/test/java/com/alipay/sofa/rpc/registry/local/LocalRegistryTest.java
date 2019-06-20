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
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.FileUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class LocalRegistryTest {

    private final static Logger   LOGGER   = LoggerFactory.getLogger(LocalRegistryTest.class);

    private static String         filePath = System.getProperty("user.home") + File.separator
                                               + "localFileTest"
                                               + new Random().nextInt(1000);

    private static String         file     = filePath + File.separator + "localRegistry.reg";

    private static RegistryConfig registryConfig;

    private static LocalRegistry  registry;

    @BeforeClass
    public static void setUp() {
        FileUtils.cleanDirectory(new File(filePath));

        registryConfig = new RegistryConfig()
            .setProtocol("local")
            //.setParameter("registry.local.scan.period", "1000")
            .setSubscribe(true)
            .setRegister(true);
        //        registryConfig.setAddress()
        //                .setConnectTimeout(5000)
        //                .setHeartbeatPeriod(60000)
        //                .setReconnectPeriod(15000)
        //                .setBatch(true)
        //                .setBatchSize(10);

        registry = (LocalRegistry) RegistryFactory.getRegistry(registryConfig);
        try {
            registry.init();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }
        registryConfig.setFile(file);
        registry.init();
        registry.start();
    }

    @AfterClass
    public static void tearDown() {
        try {
            File f = new File(file);
            Assert.assertTrue(f.exists());
            if (f.delete()) {
                Assert.assertTrue(!f.exists());
            }
            FileUtils.cleanDirectory(new File(filePath));
            registry.destroy(); // destroy可能还会备份异常
            registry = null;
        } finally {
            // 清理数据
            final boolean cleanDirectory = FileUtils.cleanDirectory(new File(filePath));
            LOGGER.info("clean result:" + cleanDirectory);
        }
    }

    public static void main(String[] args) {
        FileUtils.cleanDirectory(new File(filePath));
    }

    @Test
    public void testLoadFile() {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12200);

        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId("com.alipay.xxx.TestService")
            .setUniqueId("unique123Id")
            .setRegister(true)
            .setRegistry(registryConfig)
            .setServer(serverConfig);

        registry.register(provider);
        registry.destroy();

        // registry 关闭，但是 provider 信息保存到本地
        Assert.assertTrue(new File(file).exists());

        // 创建一个新的 localRegistry，会立即加载到缓存
        RegistryConfig newRegistryConfig = new RegistryConfig()
            .setProtocol("local")
            //.setParameter("registry.local.scan.period", "1000")
            .setSubscribe(true)
            .setFile(file)
            .setRegister(true);

        LocalRegistry newRegistry = (LocalRegistry) RegistryFactory.getRegistry(newRegistryConfig);

        newRegistry.init();
        Assert.assertFalse(newRegistry.memoryCache.isEmpty());

        // consumer 订阅时应该能立刻读到数据
        ConsumerConfig<?> consumer = new ConsumerConfig();
        consumer.setInterfaceId("com.alipay.xxx.TestService")
            .setUniqueId("unique123Id")
            .setRegistry(registryConfig)
            .setSubscribe(true);

        List<ProviderGroup> subscribe = newRegistry.subscribe(consumer);
        Assert.assertFalse(subscribe.isEmpty());
        Assert.assertFalse(subscribe.get(0).getProviderInfos().isEmpty());
    }

    @Test
    public void testAll() throws Exception {
        // test for notifyConsumer
        notifyConsumerTest();

        int timeoutPerSub = 5000;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12200);

        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId("com.alipay.xxx.TestService")
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
        consumer.setInterfaceId("com.alipay.xxx.TestService")
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);

        String tag0 = LocalRegistryHelper.buildListDataId(provider, serverConfig.getProtocol());
        String tag1 = LocalRegistryHelper.buildListDataId(consumer, consumer.getProtocol());
        Assert.assertEquals(tag1, tag0);

        String content = FileUtils.file2String(new File(file));
        Assert.assertTrue(content.startsWith(tag0));

        // 订阅
        LocalRegistryTest.MockProviderInfoListener providerInfoListener = new LocalRegistryTest.MockProviderInfoListener();
        consumer.setProviderInfoListener(providerInfoListener);
        List<ProviderGroup> groups = registry.subscribe(consumer);
        providerInfoListener.updateAllProviders(groups);
        Map<String, ProviderGroup> ps = providerInfoListener.getData();
        Assert.assertTrue(ps.size() > 0);
        Assert.assertNotNull(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertTrue(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size() == 1);

        // 反注册
        CountDownLatch latch = new CountDownLatch(1);
        providerInfoListener.setCountDownLatch(latch);
        registry.unRegister(provider);
        latch.await(timeoutPerSub, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ps.size() > 0);
        Assert.assertNotNull(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertTrue(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size() == 0);

        // 一次发2个端口的再次注册
        latch = new CountDownLatch(1);
        providerInfoListener.setCountDownLatch(latch);
        provider.getServer().add(new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12201));
        registry.register(provider);
        latch.await(timeoutPerSub * 2, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ps.size() > 0);
        Assert.assertNotNull(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertTrue(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size() == 2);

        // 重复订阅
        ConsumerConfig<?> consumer2 = new ConsumerConfig();
        consumer2.setInterfaceId("com.alipay.xxx.TestService")
            .setUniqueId("unique123Id")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);
        CountDownLatch latch2 = new CountDownLatch(1);
        LocalRegistryTest.MockProviderInfoListener providerInfoListener2 = new LocalRegistryTest.MockProviderInfoListener();
        providerInfoListener2.setCountDownLatch(latch2);
        consumer2.setProviderInfoListener(providerInfoListener2);
        List<ProviderGroup> groups2 = registry.subscribe(consumer2);
        providerInfoListener2.updateAllProviders(groups2);

        Map<String, ProviderGroup> ps2 = providerInfoListener2.getData();
        Assert.assertTrue(ps2.size() > 0);
        Assert.assertNotNull(ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertTrue(ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size() == 2);
        Assert.assertTrue(registry.memoryCache.get(tag1).size() == 2);

        // 取消订阅者1
        registry.unSubscribe(consumer);
        List<ConsumerConfig> callback = registry.notifyListeners.get(tag1);
        Assert.assertFalse(callback.contains(consumer));
        Assert.assertTrue(callback.size() == 1);

        // 批量反注册，判断订阅者2的数据
        latch = new CountDownLatch(1);
        providerInfoListener2.setCountDownLatch(latch);
        List<ProviderConfig> providerConfigList = new ArrayList<ProviderConfig>();
        providerConfigList.add(provider);
        registry.batchUnRegister(providerConfigList);

        latch.await(timeoutPerSub, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ps2.size() > 0);
        Assert.assertNotNull(ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertTrue(ps2.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size() == 0);
        Assert.assertTrue(registry.notifyListeners.size() == 1);

        // 批量取消订阅
        List<ConsumerConfig> consumerConfigList = new ArrayList<ConsumerConfig>();
        consumerConfigList.add(consumer2);
        registry.batchUnSubscribe(consumerConfigList);

        Assert.assertTrue(registry.notifyListeners.size() == 0);
    }

    public void notifyConsumerTest() {
        LocalRegistry registry = new LocalRegistry(new RegistryConfig());
        ConsumerConfig<?> consumer = new ConsumerConfig();
        consumer.setInterfaceId("test");
        LocalRegistryTest.MockProviderInfoListener providerInfoListener = new LocalRegistryTest.MockProviderInfoListener();
        consumer.setProviderInfoListener(providerInfoListener);
        registry.subscribe(consumer);
        String key = LocalRegistryHelper.buildListDataId(consumer, consumer.getProtocol());

        registry.memoryCache.put(key, new ProviderGroup());

        Map<String, ProviderGroup> newCache = new HashMap<String, ProviderGroup>();
        ProviderGroup newProviderGroup = new ProviderGroup();
        ProviderInfo providerInfo = new ProviderInfo().setHost("0.0.0.0");
        newProviderGroup.add(providerInfo);
        newCache.put(key, newProviderGroup);

        registry.notifyConsumer(newCache);

        Map<String, ProviderGroup> ps = providerInfoListener.getData();
        Assert.assertTrue(ps.size() > 0);
        Assert.assertNotNull(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertTrue(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size() == 1);
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
            for (ProviderGroup providerGroup : providerGroups) {
                ps.put(providerGroup.getName(), providerGroup);
            }
        }

        public Map<String, ProviderGroup> getData() {
            return ps;
        }
    }
}