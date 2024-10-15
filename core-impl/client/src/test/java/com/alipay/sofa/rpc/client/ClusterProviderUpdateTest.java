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

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.ClientTransport;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author junyuan
 * @version ClusterProviderUpdateTest.java, v 0.1 2024-10-11 11:04 junyuan Exp $
 */
public class ClusterProviderUpdateTest {
    private static final AbstractCluster cluster;

    static {
        ConsumerBootstrap bootstrap = new ConsumerBootstrap(new ConsumerConfig()) {
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

        cluster = new AbstractCluster(bootstrap) {
            @Override
            protected SofaResponse doInvoke(SofaRequest msg) throws SofaRpcException {
                return null;
            }
        };

        cluster.addressHolder =  new SingleGroupAddressHolder(null);
        cluster.connectionHolder = new TestUseConnectionHolder(null);
    }

    @Test
    public void testUpdateProvider() {
        String groupName = "testUpdateProvider-Group";
        List<ProviderInfo> providerList = Arrays.asList(
                ProviderHelper.toProviderInfo("127.0.0.1:12200"),
                ProviderHelper.toProviderInfo("127.0.0.1:12201"),
                ProviderHelper.toProviderInfo("127.0.0.1:12202"));
        ProviderGroup g = new ProviderGroup(groupName, providerList);
        cluster.updateProviders(g);

        Assert.assertEquals(cluster.currentProviderList().size(), providerList.size());

        cluster.updateProviders(new ProviderGroup(groupName, null));

        Assert.assertTrue(cluster.getAddressHolder().getProviderGroup(groupName).isEmpty());
        Assert.assertEquals( 1, ((TestUseConnectionHolder)cluster.connectionHolder).calledCloseAllClientTransports.get());
    }


    private static class TestUseConnectionHolder extends ConnectionHolder {
        Set<ProviderInfo> connections = new HashSet<>();

        AtomicInteger calledCloseAllClientTransports = new AtomicInteger();

        /**
         * 构造函数
         *
         * @param consumerBootstrap 服务消费者配置
         */
        protected TestUseConnectionHolder(ConsumerBootstrap consumerBootstrap) {
            super(consumerBootstrap);
        }

        @Override
        public void closeAllClientTransports(DestroyHook destroyHook) {
            calledCloseAllClientTransports.getAndIncrement();
        }

        @Override
        public ConcurrentMap<ProviderInfo, ClientTransport> getAvailableConnections() {
            return null;
        }

        @Override
        public List<ProviderInfo> getAvailableProviders() {
            return null;
        }

        @Override
        public ClientTransport getAvailableClientTransport(ProviderInfo providerInfo) {
            return null;
        }

        @Override
        public boolean isAvailableEmpty() {
            return false;
        }

        @Override
        public Collection<ProviderInfo> currentProviderList() {
            return null;
        }

        @Override
        public void setUnavailable(ProviderInfo providerInfo, ClientTransport transport) {

        }

        @Override
        public void addProvider(ProviderGroup providerGroup) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public void removeProvider(ProviderGroup providerGroup) {
            throw new UnsupportedOperationException("not implemented");

        }

        @Override
        public void updateProviders(ProviderGroup providerGroup) {
            for (ProviderInfo i : providerGroup.getProviderInfos()) {
                connections.add(i);
            }
        }

        @Override
        public void updateAllProviders(List<ProviderGroup> providerGroups) {

        }

        @Override
        public void destroy() {

        }

        @Override
        public void destroy(DestroyHook hook) {

        }

        @Override
        public void init() {

        }
    }
}