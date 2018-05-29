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
package com.alipay.sofa.rpc.metrics.lookout;

import com.alipay.lookout.api.Id;
import com.alipay.lookout.api.Lookout;
import com.alipay.sofa.rpc.config.ServerConfig;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
public class RpcLookoutId {

    private volatile Id                         consumerId;
    private final Object                        consumerIdLock  = new Object();

    private volatile Id                         providerId;
    private final Object                        providerIdLock  = new Object();

    private final ConcurrentHashMap<String, Id> serverConfigIds = new ConcurrentHashMap<String, Id>();

    /**
     * create consumerId
     *
     * @return consumerId
     */
    public Id getConsumerId() {

        if (consumerId == null) {
            synchronized (consumerIdLock) {
                if (consumerId == null) {
                    consumerId = Lookout.registry().createId("rpc.consumer.service.stats");
                }
            }
        }

        return consumerId;
    }

    /**
     * Create ProviderId
     *
     * @return ProviderId
     */
    public Id getProviderId() {

        if (providerId == null) {
            synchronized (providerIdLock) {
                if (providerId == null) {
                    providerId = Lookout.registry().createId("rpc.provider.service.stats");
                }
            }
        }

        return providerId;
    }

    public synchronized Id getServerThreadConfigId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.config";
        return getId(key);
    }

    public Id getServerThreadPoolActiveCountId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.active.count";
        return getId(key);
    }

    public Id getServerThreadPoolIdleCountId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.idle.count";
        return getId(key);
    }

    public Id getServerThreadPoolQueueSizeId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.queue.size";
        return getId(key);
    }

    private Id getId(String key) {
        Id lookoutId = serverConfigIds.get(key);
        if (lookoutId == null) {
            synchronized (RpcLookout.class) {
                lookoutId = serverConfigIds.get(key);
                if (lookoutId == null) {
                    lookoutId = Lookout.registry().createId(key);
                    serverConfigIds.put(key, lookoutId);
                }
            }
        }
        return lookoutId;
    }

    public Id removeServerThreadConfigId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.config";
        return serverConfigIds.remove(key);
    }

    public Id removeServerThreadPoolActiveCountId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.active.count";
        return serverConfigIds.remove(key);
    }

    public Id removeServerThreadPoolIdleCountId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.idle.count";
        return serverConfigIds.remove(key);
    }

    public Id removeServerThreadPoolQueueSizeId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.queue.size";
        return serverConfigIds.remove(key);
    }
}