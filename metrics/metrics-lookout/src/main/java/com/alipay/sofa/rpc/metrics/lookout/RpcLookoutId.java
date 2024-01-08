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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
public class RpcLookoutId {

    private final ConcurrentMap<String, Id> consumerIds          = new ConcurrentHashMap<String, Id>();

    private final ConcurrentMap<String, Id> providerIds          = new ConcurrentHashMap<String, Id>();

    private final ConcurrentMap<String, Id> serverConfigIds      = new ConcurrentHashMap<String, Id>();

    private volatile Id                     consumerConfigId;

    private volatile Id                     providerConfigId;

    private static final Lock               classLock            = new ReentrantLock();
    private final Lock                      consumerConfigIdLock = new ReentrantLock();
    private final Lock                      providerConfigIdLock = new ReentrantLock();

    /**
     * create consumerId
     *
     * @return consumerId
     */
    public Id fetchConsumerStatId(Map<String, String> tags) {

        String key = tags.toString();
        Id lookoutId = consumerIds.get(key);
        if (lookoutId == null) {
            try {
                classLock.lock();
                lookoutId = consumerIds.get(key);
                if (lookoutId == null) {
                    lookoutId = Lookout.registry().createId("rpc.consumer.service.stats", tags);
                    consumerIds.put(key, lookoutId);
                }
            } finally {
                classLock.unlock();
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
            try {
                classLock.lock();
                lookoutId = providerIds.get(key);
                if (lookoutId == null) {
                    lookoutId = Lookout.registry().createId("rpc.provider.service.stats", tags);
                    providerIds.put(key, lookoutId);
                }
            } finally {
                classLock.unlock();
            }
        }
        return lookoutId;
    }

    public Id fetchConsumerSubId() {
        if (consumerConfigId == null) {
            try {
                consumerConfigIdLock.lock();
                if (consumerConfigId == null) {
                    consumerConfigId = Lookout.registry().createId("rpc.consumer.info.stats");
                }
            } finally {
                consumerConfigIdLock.unlock();
            }
        }
        return consumerConfigId;
    }

    public Id fetchProviderPubId() {
        if (providerConfigId == null) {
            try {
                providerConfigIdLock.lock();
                if (providerConfigId == null) {
                    providerConfigId = Lookout.registry().createId("rpc.provider.info.stats");
                }
            } finally {
                providerConfigIdLock.unlock();
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
            try {
                classLock.lock();
                lookoutId = serverConfigIds.get(key);
                if (lookoutId == null) {
                    lookoutId = Lookout.registry().createId(key);
                    serverConfigIds.put(key, lookoutId);
                }
            } finally {
                classLock.unlock();
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