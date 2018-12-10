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
package com.alipay.sofa.rpc.registry.etcd;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import java.util.concurrent.Callable;

public class Watcher implements Callable {

    private ConsumerConfig       consumerConfig;
    private EtcdProviderObserver observer;
    private EtcdHelper           etcdHelper;
    private Long                 watchId;

    public Watcher(EtcdHelper etcdHelper, ConsumerConfig consumerConfig, EtcdProviderObserver observer) {
        this.consumerConfig = consumerConfig;
        this.observer = observer;
        this.etcdHelper = etcdHelper;
    }

    @Override
    public Object call() throws Exception {
        System.out.println("called----");
        observer.updateProviders(consumerConfig,
            etcdHelper.getInstances(consumerConfig.getAppName(), consumerConfig.getProtocol()));
        return null;
    }

    String getKeyPrefix() {
        return EtcdRegistryHelper.buildKeyPrefix(consumerConfig.getAppName(), consumerConfig.getProtocol());
    }

    public ConsumerConfig getConfig() {
        return this.consumerConfig;
    }

    public void setWatchId(Long watchId) {
        this.watchId = watchId;
    }

    public Long getWatchId() {
        return this.watchId;
    }

    @Override
    public String toString() {
        return "Watcher{" +
            "consumerConfig=" + consumerConfig +
            ", watchId=" + watchId +
            '}';
    }
}
