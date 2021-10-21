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
package com.alipay.sofa.rpc.registry.nacos;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.config.*;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.registry.nacos.base.BaseNacosTest;
import com.alipay.sofa.rpc.server.Server;
import com.alipay.sofa.rpc.server.ServerFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * When nacos registers the service, test the VirtualHost and VirtualPort of serverconfig
 *
 * @author <a href=mailto:916108538@qq.com>ZhengweiHou</a>
 */
public class NacosRegistryVirtualHostAndVirtualPortTest extends BaseNacosTest {

    private static RegistryConfig registryConfig;

    private NacosRegistry         registry;

    private ServerConfig          serverConfig;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        registryConfig = new RegistryConfig()
            .setProtocol("nacos")
            .setSubscribe(true)

            .setAddress("127.0.0.1:" + nacosProcess.getServerPort() + "/public")
            .setRegister(true);

        registry = (NacosRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();
        Assert.assertTrue(registry.start());
    }

    /**
     * Tear down.
     */
    @After
    public void tearDown() {
        registry.destroy();
        registry = null;
        serverConfig.destroy();
    }

    /**
     * 测试NacosRegistry 对 serverconfig中 VirtualHost 和 VirtualPort 参数的支持情况
     *
     * @throws Exception the exception
     */
    @Test
    public void testVirtualHostAndVirtualPort() throws Exception {
        int timeoutPerSub = 2000;

        // 模拟的场景 client -> proxy:127.7.7.7:8888 -> netty:0.0.0.0:12200
        String virtualHost="127.7.7.7";
        int    virtualPort=8888;
        serverConfig = new ServerConfig()
            .setProtocol("bolt")
            .setHost("0.0.0.0")
            .setPort(12200)
            .setAdaptivePort(false) // Turn off adaptive port
            .setVirtualHost(virtualHost)
            .setVirtualPort(virtualPort);

        // Verify the influence of virtualHost and virtualPort on the parameters when creating rpcserver
        Method m_resolveServerConfig = ServerFactory.class.getDeclaredMethod("resolveServerConfig", ServerConfig.class);
        m_resolveServerConfig.setAccessible(true);
        m_resolveServerConfig.invoke(new ServerFactory(),serverConfig);
        Assert.assertNotEquals("boundhost should not be equal to virtualHost", serverConfig.getBoundHost(), virtualHost);
        Assert.assertEquals("boundPort should be oriPort",serverConfig.getPort(),12200);


        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId("com.alipay.xxx.NacosTestService")
            .setApplication(new ApplicationConfig().setAppName("test-server"))
            .setUniqueId("nacos-test")
            .setProxy("javassist")
            .setRegister(true)
            .setRegistry(registryConfig)
            .setServer(serverConfig);

        // 注册
        registry.register(provider);
        Thread.sleep(1000);

        ConsumerConfig<?> consumer = new ConsumerConfig();
        consumer.setInterfaceId("com.alipay.xxx.NacosTestService")
            .setApplication(new ApplicationConfig().setAppName("test-consumer"))
            .setUniqueId("nacos-test")
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
        Map.Entry<String, ProviderInfo> psEntry = (Map.Entry) ps.entrySet().toArray()[0];
        ProviderInfo pri = psEntry.getValue();
        Assert.assertEquals("The provider's key should consist of virtualHost and virtualPort",psEntry.getKey(),virtualHost + ":" + virtualPort);
        Assert.assertEquals("The provider's host should be virtualHost",virtualHost,pri.getHost());
        Assert.assertEquals("The provider's port should be virtualPort",virtualPort,pri.getPort());
    }

    private static class MockProviderInfoListener implements ProviderInfoListener {

        /**
         * The Ps.
         */
        ConcurrentMap<String, ProviderInfo> ps = new ConcurrentHashMap<String, ProviderInfo>();

        private CountDownLatch              countDownLatch;

        /**
         * Sets count down latch.
         *
         * @param countDownLatch the count down latch
         */
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

        /**
         * Gets data.
         *
         * @return the data
         */
        public Map<String, ProviderInfo> getData() {
            return ps;
        }
    }

}