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
package com.alipay.sofa.rpc.registry.mesh;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.common.json.JSON;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.registry.mesh.mock.HttpMockServer;
import com.alipay.sofa.rpc.registry.mesh.model.ApplicationInfoResult;
import com.alipay.sofa.rpc.registry.mesh.model.MeshEndpoint;
import com.alipay.sofa.rpc.registry.mesh.model.PublishServiceResult;
import com.alipay.sofa.rpc.registry.mesh.model.SubscribeServiceResult;
import com.alipay.sofa.rpc.registry.mesh.model.UnPublishServiceResult;
import com.alipay.sofa.rpc.registry.mesh.model.UnSubscribeServiceResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class MeshRegistryTest extends BaseMeshTest {

    private RegistryConfig registryConfig;

    private MeshRegistry   registry;

    private HttpMockServer httpMockServer;

    @Before
    public void setUp() {
        httpMockServer = new HttpMockServer();
        httpMockServer.initSever(7654);

        ApplicationInfoResult applicationInfoResult = new ApplicationInfoResult();
        applicationInfoResult.setSuccess(true);
        httpMockServer.addMockPath(MeshEndpoint.CONFIGS, JSON.toJSONString(applicationInfoResult));

        PublishServiceResult publishServiceResult = new PublishServiceResult();
        publishServiceResult.setSuccess(true);
        httpMockServer.addMockPath(MeshEndpoint.PUBLISH, JSON.toJSONString(publishServiceResult));

        SubscribeServiceResult subscribeServiceResult = new SubscribeServiceResult();
        subscribeServiceResult.setSuccess(true);
        List<String> datas = new ArrayList<String>();
        datas.add("127.0.0.1:12200?v=4.0&p=1");
        datas.add("127.0.0.1:12201?v=4.0&p=1");
        subscribeServiceResult.setDatas(datas);
        httpMockServer.addMockPath(MeshEndpoint.SUBCRIBE, JSON.toJSONString(subscribeServiceResult));

        UnPublishServiceResult unPublishServiceResult = new UnPublishServiceResult();
        unPublishServiceResult.setSuccess(true);
        httpMockServer.addMockPath(MeshEndpoint.UN_PUBLISH, JSON.toJSONString(unPublishServiceResult));

        UnSubscribeServiceResult unSubscribeServiceResult = new UnSubscribeServiceResult();
        unSubscribeServiceResult.setSuccess(true);
        httpMockServer.addMockPath(MeshEndpoint.UN_SUBCRIBE, JSON.toJSONString(unSubscribeServiceResult));

        httpMockServer.start();
        registryConfig = new RegistryConfig()
            .setProtocol("mesh")
            .setSubscribe(true)
            .setRegister(true)
            .setAddress("http://localhost:7654");

        registry = (MeshRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();
        registry.start();
    }

    @After
    public void tearDown() {
        try {
            registry.destroy(); // destroy可能还会备份异常
            registry = null;
        } finally {
            // 清理数据
            httpMockServer.stop();
        }
    }

    @Test
    public void testOnlyPublish() throws InterruptedException {

        Field registedAppField = null;
        try {
            registedAppField = MeshRegistry.class.getDeclaredField("registedApp");
            registedAppField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        Boolean registedAppValue = null;
        // in case of effected by other case.
        try {
            registedAppValue = (Boolean) registedAppField.get(registry);
            registedAppField.set(registry, false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

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

        registry.register(provider);
        Thread.sleep(3000);

        try {
            registedAppValue = (Boolean) registedAppField.get(registry);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        LOGGER.info("final registedAppValue is " + registedAppValue);

        Assert.assertTrue(registedAppValue);
    }

    @Test
    public void testAll() throws Exception {

        int timeoutPerSub = 1000;

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

        String tag0 = MeshRegistryHelper.buildMeshKey(provider, serverConfig.getProtocol());
        String tag1 = MeshRegistryHelper.buildMeshKey(consumer, consumer.getProtocol());
        Assert.assertEquals(tag1, tag0);

        // 订阅
        MeshRegistryTest.MockProviderInfoListener providerInfoListener = new MeshRegistryTest.MockProviderInfoListener();
        consumer.setProviderInfoListener(providerInfoListener);
        List<ProviderGroup> groups = registry.subscribe(consumer);
        Assert.assertNull(groups);
        Thread.sleep(3000);
        Map<String, ProviderGroup> ps = providerInfoListener.getData();
        Assert.assertTrue(ps.toString(), ps.size() == 1);

        // 反注册
        CountDownLatch latch = new CountDownLatch(1);
        providerInfoListener.setCountDownLatch(latch);
        registry.unRegister(provider);
        latch.await(timeoutPerSub, TimeUnit.MILLISECONDS);
        //mesh 并不直接感知.
        Assert.assertTrue(ps.size() == 1);

        // 一次发2个端口的再次注册
        latch = new CountDownLatch(1);
        providerInfoListener.setCountDownLatch(latch);
        provider.getServer().add(new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12201));
        registry.register(provider);
        latch.await(timeoutPerSub * 2, TimeUnit.MILLISECONDS);

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
        MeshRegistryTest.MockProviderInfoListener providerInfoListener2 = new MeshRegistryTest.MockProviderInfoListener();
        providerInfoListener2.setCountDownLatch(latch2);
        consumer2.setProviderInfoListener(providerInfoListener2);
        List<ProviderGroup> groups2 = registry.subscribe(consumer2);
        Assert.assertNull(groups);
        Thread.sleep(3000);
        Map<String, ProviderGroup> ps2 = providerInfoListener2.getData();
        Assert.assertTrue(ps2.size() == 1);

        // 取消订阅者1
        registry.unSubscribe(consumer);

        // 批量反注册，判断订阅者2的数据
        latch = new CountDownLatch(1);
        providerInfoListener2.setCountDownLatch(latch);
        List<ProviderConfig> providerConfigList = new ArrayList<ProviderConfig>();
        providerConfigList.add(provider);
        registry.batchUnRegister(providerConfigList);

        latch.await(timeoutPerSub, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ps2.size() == 1);

        // 批量取消订阅
        List<ConsumerConfig> consumerConfigList = new ArrayList<ConsumerConfig>();
        consumerConfigList.add(consumer2);
        registry.batchUnSubscribe(consumerConfigList);

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