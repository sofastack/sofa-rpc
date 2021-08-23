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
import com.alipay.lookout.api.Metric;
import com.alipay.sofa.rpc.config.ServerConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
public class RpcLookoutId {

    private final ConcurrentMap<String, Id> consumerIds          = new ConcurrentHashMap<String, Id>();

    private final ConcurrentMap<String, Id> providerIds          = new ConcurrentHashMap<String, Id>();

    private final ConcurrentMap<String, Id> serverConfigIds      = new ConcurrentHashMap<String, Id>();

    private volatile Id                     consumerConfigId;
    private final Object                    consumerConfigIdLock = new Object();

    private volatile Id                     providerConfigId;
    private final Object                    providerConfigIdLock = new Object();

    /**
     * create consumerId
     *
     * @return consumerId
     */
    public Id fetchConsumerStatId(Map<String, String> tags) {

        String key = tags.toString();
        Id lookoutId = consumerIds.get(key);
        if (lookoutId == null) {
            synchronized (RpcLookoutId.class) {
                lookoutId = consumerIds.get(key);
                if (lookoutId == null) {
                    lookoutId = Lookout.registry().createId("rpc.consumer.service.stats", tags);
                    consumerIds.put(key, lookoutId);
                }
            }
        }
        return lookoutId;
    }

    /**
     * Create ProviderId
     *
     * @return ProviderId
     */
    public Id fetchProviderStatId(Map<String, String> tags) {
        String key = tags.toString();
        Id lookoutId = providerIds.get(key);
        if (lookoutId == null) {
            synchronized (RpcLookoutId.class) {
                lookoutId = providerIds.get(key);
                if (lookoutId == null) {
                    lookoutId = Lookout.registry().createId("rpc.provider.service.stats", tags);
                    providerIds.put(key, lookoutId);
                }
            }
        }
        return lookoutId;
    }

    public Id fetchConsumerSubId() {
        if (consumerConfigId == null) {
            synchronized (consumerConfigIdLock) {
                if (consumerConfigId == null) {
                    consumerConfigId = Lookout.registry().createId("rpc.consumer.info.stats");
                }
            }
        }
        return consumerConfigId;
    }

    public Id fetchProviderPubId() {
        if (providerConfigId == null) {
            synchronized (providerConfigIdLock) {
                if (providerConfigId == null) {
                    providerConfigId = Lookout.registry().createId("rpc.provider.info.stats");
                }
            }
        }
        return providerConfigId;
    }

    public synchronized Id fetchServerThreadConfigId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.config";
        return fetchServerConfigId(key);
    }

    public Id fetchServerThreadPoolActiveCountId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.active.count";
        return fetchServerConfigId(key);
    }

    public Id fetchServerThreadPoolIdleCountId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.idle.count";
        return fetchServerConfigId(key);
    }

    public Id fetchServerThreadPoolQueueSizeId(ServerConfig serverConfig) {
        String key = "rpc." + serverConfig.getProtocol() + ".threadpool.queue.size";
        return fetchServerConfigId(key);
    }

    private Id fetchServerConfigId(String key) {
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

    private Id fetchConsumerStatId(String key) {
        Id lookoutId = consumerIds.get(key);
        if (lookoutId == null) {
            synchronized (RpcLookout.class) {
                lookoutId = consumerIds.get(key);
                if (lookoutId == null) {
                    lookoutId = Lookout.registry().createId(key);
                    consumerIds.put(key, lookoutId);
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