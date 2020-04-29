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
package com.alipay.sofa.rpc.bootstrap;

import com.alipay.sofa.rpc.client.Cluster;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;

import java.util.List;

/**
 * 集群服务端地址监听器
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ClusterProviderInfoListener implements ProviderInfoListener {

    /**
     * Cluster of client
     */
    private final Cluster cluster;

    public ClusterProviderInfoListener(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public void addProvider(ProviderGroup group) {
        if (cluster != null) {
            boolean originalState = cluster.isAvailable();
            cluster.addProvider(group);
            cluster.checkStateChange(originalState);
        }
    }

    @Override
    public void removeProvider(ProviderGroup group) {
        if (cluster != null) {
            boolean originalState = cluster.isAvailable();
            cluster.removeProvider(group);
            cluster.checkStateChange(originalState);
        }
    }

    @Override
    public void updateProviders(ProviderGroup group) {
        if (cluster != null) {
            boolean originalState = cluster.isAvailable();
            cluster.updateProviders(group);
            cluster.checkStateChange(originalState);
        }
    }

    @Override
    public void updateAllProviders(List<ProviderGroup> groups) {
        if (cluster != null) {
            boolean originalState = cluster.isAvailable();
            cluster.updateAllProviders(groups);
            cluster.checkStateChange(originalState);
        }
    }
}
