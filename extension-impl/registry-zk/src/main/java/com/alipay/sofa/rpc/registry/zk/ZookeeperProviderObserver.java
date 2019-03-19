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
package com.alipay.sofa.rpc.registry.zk;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.codec.common.StringSerializer;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;
import org.apache.curator.framework.recipes.cache.ChildData;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ZookeeperObserver for provider node.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ZookeeperProviderObserver {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                                       LOGGER              = LoggerFactory
                                                                                              .getLogger(ZookeeperProviderObserver.class);

    /**
     * The Provider add listener map.
     */
    private ConcurrentMap<ConsumerConfig, List<ProviderInfoListener>> providerListenerMap = new ConcurrentHashMap<ConsumerConfig, List<ProviderInfoListener>>();

    /**
     * Add provider listener.
     *
     * @param consumerConfig the consumer config
     * @param listener       the listener
     */
    public void addProviderListener(ConsumerConfig consumerConfig, ProviderInfoListener listener) {
        if (listener != null) {
            RegistryUtils.initOrAddList(providerListenerMap, consumerConfig, listener);
        }
    }

    /**
     * Remove provider listener.
     *
     * @param consumerConfig the consumer config
     */
    public void removeProviderListener(ConsumerConfig consumerConfig) {
        providerListenerMap.remove(consumerConfig);
    }

    /**
     * Update Provider
     *
     * @param config       ConsumerConfig
     * @param providerPath Provider path of zookeeper
     * @param data         Event data
     * @param currentData  provider data list
     * @throws UnsupportedEncodingException decode error
     */
    public void updateProvider(ConsumerConfig config, String providerPath, ChildData data, List<ChildData> currentData)
        throws UnsupportedEncodingException {
        if (LOGGER.isInfoEnabled(config.getAppName())) {
            LOGGER.infoWithApp(config.getAppName(),
                "Receive update provider: path=[" + data.getPath() + "]" + ", data=[" +
                    StringSerializer.decode(data.getData()) + "]" + ", stat=[" + data.getStat() + "]" + ", list=[" +
                    currentData.size() + "]");
        }
        notifyListeners(config, providerPath, currentData, false);
    }

    /**
     * Remove Provider
     *
     * @param config       ConsumerConfig
     * @param providerPath Provider path of zookeeper
     * @param data         Event data
     * @param currentData  provider data list
     * @throws UnsupportedEncodingException decode error
     */
    public void removeProvider(ConsumerConfig config, String providerPath, ChildData data, List<ChildData> currentData)
        throws UnsupportedEncodingException {
        if (LOGGER.isInfoEnabled(config.getAppName())) {
            LOGGER.infoWithApp(config.getAppName(),
                "Receive remove provider: path=[" + data.getPath() + "]" + ", data=[" +
                    StringSerializer.decode(data.getData()) + "]" + ", stat=[" + data.getStat() + "]" + ", list=[" +
                    currentData.size() + "]");
        }
        notifyListeners(config, providerPath, currentData, false);
    }

    /**
     * Add provider
     *
     * @param config       ConsumerConfig
     * @param providerPath Provider path of zookeeper
     * @param data         Event data
     * @param currentData  provider data list
     * @throws UnsupportedEncodingException decode error
     */
    public void addProvider(ConsumerConfig config, String providerPath, ChildData data, List<ChildData> currentData)
        throws UnsupportedEncodingException {
        if (LOGGER.isInfoEnabled(config.getAppName())) {
            LOGGER.infoWithApp(config.getAppName(),
                "Receive add provider: path=[" + data.getPath() + "]" + ", data=[" +
                    StringSerializer.decode(data.getData()) + "]" + ", stat=[" + data.getStat() + "]" + ", list=[" +
                    currentData.size() + "]");
        }
        notifyListeners(config, providerPath, currentData, true);
    }

    private void notifyListeners(ConsumerConfig config, String providerPath, List<ChildData> currentData, boolean add)
        throws UnsupportedEncodingException {
        List<ProviderInfoListener> providerInfoListeners = providerListenerMap.get(config);
        if (CommonUtils.isNotEmpty(providerInfoListeners)) {
            List<ProviderInfo> providerInfos = ZookeeperRegistryHelper.convertUrlsToProviders(providerPath,
                currentData);
            List<ProviderInfo> providerInfosForProtocol = RegistryUtils.matchProviderInfos(config, providerInfos);
            for (ProviderInfoListener listener : providerInfoListeners) {
                if (add) {
                    listener.addProvider(new ProviderGroup(providerInfosForProtocol));
                } else {
                    listener.updateProviders(new ProviderGroup(providerInfosForProtocol));
                }
            }
        }
    }
}
