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

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.ClientTransport;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zdyu 2022/2/10
 */
public class AbstractClusterTest {

    private static AbstractCluster abstractCluster;

    @BeforeClass
    public static void beforeClass() {
        ConsumerConfig consumerConfig = new ConsumerConfig().setProtocol("test")
            .setBootstrap("test");
        ConsumerBootstrap consumerBootstrap = new ConsumerBootstrap(consumerConfig) {
            @Override
            public Object refer() {
                return null;
            }

            @Override
            public void unRefer() {

            }

            @Override
            public Object getProxyIns() {
                return null;
            }

            @Override
            public Cluster getCluster() {
                return null;
            }

            @Override
            public List<ProviderGroup> subscribe() {
                return null;
            }

            @Override
            public boolean isSubscribed() {
                return false;
            }
        };
        abstractCluster = new AbstractCluster(consumerBootstrap) {
            @Override
            protected SofaResponse doInvoke(SofaRequest msg) throws SofaRpcException {
                return null;
            }
        };
    }

    @Test
    public void testDestroyWithDestroyHook() {
        List<String> hookActionResult = new ArrayList<>(2);
        Destroyable.DestroyHook destroyHook = new Destroyable.DestroyHook() {
            @Override
            public void preDestroy() {
                hookActionResult.add("preDestroy");
            }

            @Override
            public void postDestroy() {
                hookActionResult.add("postDestroy");
            }
        };
        abstractCluster.destroy(destroyHook);
        Assert.assertEquals(2, hookActionResult.size());
        Assert.assertEquals("preDestroy", hookActionResult.get(0));
        Assert.assertEquals("postDestroy", hookActionResult.get(1));
    }

    @Test
    public void testResolveTimeout() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method resolveTimeoutMethod = AbstractCluster.class.getDeclaredMethod("resolveTimeout", SofaRequest.class,
            ConsumerConfig.class, ProviderInfo.class);
        resolveTimeoutMethod.setAccessible(true);

        SofaRequest sofaRequest = new SofaRequest();
        ConsumerConfig consumerConfig = new ConsumerConfig();
        ProviderInfo providerInfo = new ProviderInfo();
        Integer defaultTimeout = (Integer) resolveTimeoutMethod.invoke(abstractCluster, sofaRequest, consumerConfig,
            providerInfo);
        Assert.assertTrue(defaultTimeout == 3000);

        providerInfo.setStaticAttr(ProviderInfoAttrs.ATTR_TIMEOUT, "5000");
        Integer providerTimeout = (Integer) resolveTimeoutMethod.invoke(abstractCluster, sofaRequest, consumerConfig,
            providerInfo);
        Assert.assertTrue(providerTimeout == 5000);

        consumerConfig.setTimeout(2000);
        Integer consumerTimeout = (Integer) resolveTimeoutMethod.invoke(abstractCluster, sofaRequest, consumerConfig,
            providerInfo);
        Assert.assertTrue(consumerTimeout == 2000);

        sofaRequest.setTimeout(1000);
        Integer invokeTimeout = (Integer) resolveTimeoutMethod.invoke(abstractCluster, sofaRequest, consumerConfig,
            providerInfo);
        Assert.assertTrue(invokeTimeout == 1000);

    }

    /**
     * 验证 updateAllProviders 中 connectionHolder 先于 addressHolder 被调用
     * 修复 Issue #1490: AddressHolder 和 ConnectionHolder 更新顺序问题
     */
    @Test
    public void testUpdateAllProvidersCallOrder() throws Exception {
        // 创建一个新的 AbstractCluster 实例，使用 Mock 的 AddressHolder 和 ConnectionHolder
        ConsumerConfig consumerConfig = new ConsumerConfig().setProtocol("test").setBootstrap("test");
        ConsumerBootstrap consumerBootstrap = new ConsumerBootstrap(consumerConfig) {
            @Override
            public Object refer() {
                return null;
            }

            @Override
            public void unRefer() {
            }

            @Override
            public Object getProxyIns() {
                return null;
            }

            @Override
            public Cluster getCluster() {
                return null;
            }

            @Override
            public List<ProviderGroup> subscribe() {
                return null;
            }

            @Override
            public boolean isSubscribed() {
                return false;
            }
        };

        final AtomicInteger addressHolderCallCount = new AtomicInteger(0);
        final AtomicInteger connectionHolderCallCount = new AtomicInteger(0);
        final AtomicInteger lastCallOrder = new AtomicInteger(0);

        AddressHolder mockAddressHolder = new AddressHolder(consumerBootstrap) {
            @Override
            public List<ProviderInfo> getProviderInfos(String groupName) {
                return new ArrayList<>();
            }

            @Override
            public ProviderGroup getProviderGroup(String groupName) {
                return new ProviderGroup();
            }

            @Override
            public List<ProviderGroup> getProviderGroups() {
                return new ArrayList<>();
            }

            @Override
            public int getAllProviderSize() {
                return 0;
            }

            @Override
            public void addProvider(ProviderGroup providerGroup) {
            }

            @Override
            public void removeProvider(ProviderGroup providerGroup) {
            }

            @Override
            public void updateProviders(ProviderGroup providerGroup) {
            }

            @Override
            public void updateAllProviders(List<ProviderGroup> providerGroups) {
                addressHolderCallCount.incrementAndGet();
                lastCallOrder.set(2); // 标记 addressHolder 被调用
            }
        };

        ConnectionHolder mockConnectionHolder = new ConnectionHolder(consumerBootstrap) {
            @Override
            public void init() {
            }

            @Override
            public void destroy() {
            }

            @Override
            public void destroy(Destroyable.DestroyHook destroyHook) {
            }

            @Override
            public void closeAllClientTransports(Destroyable.DestroyHook destroyHook) {
            }

            @Override
            public ConcurrentMap<ProviderInfo, ClientTransport> getAvailableConnections() {
                return new ConcurrentHashMap<>();
            }

            @Override
            public List<ProviderInfo> getAvailableProviders() {
                return new ArrayList<>();
            }

            @Override
            public ClientTransport getAvailableClientTransport(ProviderInfo providerInfo) {
                return null;
            }

            @Override
            public boolean isAvailableEmpty() {
                return true;
            }

            @Override
            public Collection<ProviderInfo> currentProviderList() {
                return new ArrayList<>();
            }

            @Override
            public void setUnavailable(ProviderInfo providerInfo, ClientTransport transport) {
            }

            @Override
            public void addProvider(ProviderGroup providerGroup) {
            }

            @Override
            public void removeProvider(ProviderGroup providerGroup) {
            }

            @Override
            public void updateProviders(ProviderGroup providerGroup) {
            }

            @Override
            public void updateAllProviders(List<ProviderGroup> providerGroups) {
                connectionHolderCallCount.incrementAndGet();
                lastCallOrder.set(1); // 标记 connectionHolder 被调用
            }
        };

        AbstractCluster cluster = new AbstractCluster(consumerBootstrap) {
            @Override
            protected SofaResponse doInvoke(SofaRequest msg) throws SofaRpcException {
                return null;
            }
        };

        // 通过反射设置 Mock 对象
        try {
            java.lang.reflect.Field addressHolderField = AbstractCluster.class.getDeclaredField("addressHolder");
            addressHolderField.setAccessible(true);
            addressHolderField.set(cluster, mockAddressHolder);

            java.lang.reflect.Field connectionHolderField = AbstractCluster.class.getDeclaredField("connectionHolder");
            connectionHolderField.setAccessible(true);
            connectionHolderField.set(cluster, mockConnectionHolder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set mock objects", e);
        }

        // 准备测试数据
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.setHost("127.0.0.1");
        providerInfo.setPort(8080);
        providerInfo.setPath("/test");
        providerInfo.setProtocolType("bolt");

        ProviderGroup providerGroup = new ProviderGroup();
        providerGroup.add(providerInfo);

        List<ProviderGroup> providerGroups = new ArrayList<>();
        providerGroups.add(providerGroup);

        // 调用 updateAllProviders
        cluster.updateAllProviders(providerGroups);

        // 验证调用顺序：connectionHolder 应该先于 addressHolder 被调用
        Assert.assertEquals("connectionHolder.updateAllProviders should be called once", 1, connectionHolderCallCount.get());
        Assert.assertEquals("addressHolder.updateAllProviders should be called once", 1, addressHolderCallCount.get());
        Assert.assertEquals("connectionHolder should be called before addressHolder", 2, lastCallOrder.get());
    }
}