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
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.registry.etcd.client.EtcdClient;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.KeyValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EtcdHelper {

    private final EtcdClient                   client;
    private final Gson                         gson     = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                                                            .create();
    private final Map<ConsumerConfig, Watcher> watchers = new ConcurrentHashMap<ConsumerConfig, Watcher>();

    public EtcdHelper(EtcdClient client) {
        if (client == null) {
            throw new SofaRpcRuntimeException("etcd client should be initialized");
        }
        this.client = client;
    }

    void register(ServiceInstance instance) {
        long leaseId = client.putWithLease(EtcdRegistryHelper.buildUniqueKey(instance), instance.toJson());
        client.keepAlive(leaseId);
    }

    public void deregister(ServiceInstance instance) {
        client.revokeLease(EtcdRegistryHelper.buildUniqueKey(instance));
    }

    public List<ServiceInstance> getInstances(String serviceName, String protocol, String uniqueId) {
        String keyPrefix = EtcdRegistryHelper.buildKeyPrefix(serviceName, protocol, uniqueId);
        List<KeyValue> keyValues = client.getWithPrefix(keyPrefix);
        List<ServiceInstance> serviceInstances = new ArrayList<ServiceInstance>(keyValues.size());
        for (KeyValue keyValue : keyValues) {
            serviceInstances.add(gson.fromJson(keyValue.getValue().toStringUtf8(), ServiceInstance.class));
        }
        return serviceInstances;
    }

    public void unsubscribe(ConsumerConfig consumerConfig) {
        Watcher watcher = watchers.get(consumerConfig);
        if (watcher != null && watcher.getWatchId() != null) {
            client.cancelWatch(watcher);
            watchers.remove(consumerConfig);
        }
    }

    public void startWatch(Watcher watcher) {
        watchers.put(watcher.getConfig(), watcher);
        client.startWatch(watcher.getKeyPrefix(), watcher);
    }
}
