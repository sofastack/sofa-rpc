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
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.apache.curator.framework.recipes.cache.ChildData;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZookeeperObserver for provider node.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ZookeeperProviderObserver extends AbstractZookeeperObserver {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                                           LOGGER              = LoggerFactory
                                                                                                  .getLogger(ZookeeperConfigObserver.class);

    /**
     * The Provider add listener map.
     */
    private ConcurrentHashMap<ConsumerConfig, List<ProviderInfoListener>> providerListenerMap = new ConcurrentHashMap<ConsumerConfig, List<ProviderInfoListener>>();

    /**
     * Add provider listener.
     *
     * @param consumerConfig the consumer config
     * @param listener       the listener
     */
    public void addProviderListener(ConsumerConfig consumerConfig, ProviderInfoListener listener) {
        if (listener != null) {
            initOrAddList(providerListenerMap, consumerConfig, listener);
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

    public void updateProvider(ConsumerConfig config, String providerPath, ChildData data)
        throws UnsupportedEncodingException {
        if (LOGGER.isInfoEnabled(config.getAppName())) {
            LOGGER.infoWithApp(config.getAppName(), "Receive update provider: path=[" + data.getPath() + "]"
                + ", data=[" + new String(data.getData(), RpcConstants.DEFAULT_CHARSET) + "]"
                + ", stat=[" + data.getStat() + "]");
        }
        List<ProviderInfoListener> providerInfoListeners = providerListenerMap.get(config);
        if (CommonUtils.isNotEmpty(providerInfoListeners)) {
            List<ProviderInfo> providerInfos = Arrays.asList(
                ZookeeperRegistryHelper.convertUrlToProvider(providerPath, data));
            for (ProviderInfoListener listener : providerInfoListeners) {
                List<ProviderInfo> providerInfosForProtocol = filterByProtocol(config.getProtocol(), providerInfos);
                listener.addProvider(new ProviderGroup(providerInfosForProtocol));
            }
        }
    }

    public void removeProvider(ConsumerConfig config, String providerPath, ChildData data)
        throws UnsupportedEncodingException {
        if (LOGGER.isInfoEnabled(config.getAppName())) {
            LOGGER.infoWithApp(config.getAppName(), "Receive remove provider: path=[" + data.getPath() + "]"
                + ", data=[" + new String(data.getData(), RpcConstants.DEFAULT_CHARSET) + "]"
                + ", stat=[" + data.getStat() + "]");
        }
        List<ProviderInfoListener> providerInfoListeners = providerListenerMap.get(config);
        if (CommonUtils.isNotEmpty(providerInfoListeners)) {
            List<ProviderInfo> providerInfos = Arrays.asList(
                ZookeeperRegistryHelper.convertUrlToProvider(providerPath, data));
            List<ProviderInfo> providerInfosForProtocol = filterByProtocol(config.getProtocol(), providerInfos);
            for (ProviderInfoListener listener : providerInfoListeners) {
                listener.removeProvider(new ProviderGroup(providerInfosForProtocol));
            }
        }
    }

    public void addProvider(ConsumerConfig config, String providerPath, ChildData data)
        throws UnsupportedEncodingException {
        if (LOGGER.isInfoEnabled(config.getAppName())) {
            LOGGER.infoWithApp(config.getAppName(), "Receive add provider: path=[" + data.getPath() + "]"
                + ", data=[" + new String(data.getData(), RpcConstants.DEFAULT_CHARSET) + "]"
                + ", stat=[" + data.getStat() + "]");
        }
        List<ProviderInfoListener> providerInfoListeners = providerListenerMap.get(config);
        if (CommonUtils.isNotEmpty(providerInfoListeners)) {
            List<ProviderInfo> providerInfos = Arrays.asList(
                ZookeeperRegistryHelper.convertUrlToProvider(providerPath, data));
            for (ProviderInfoListener listener : providerInfoListeners) {
                List<ProviderInfo> providerInfosForProtocol = filterByProtocol(config.getProtocol(), providerInfos);
                listener.addProvider(new ProviderGroup(providerInfosForProtocol));
            }
        }
    }

    private List<ProviderInfo> filterByProtocol(String protocol, List<ProviderInfo> providerInfos) {
        List<ProviderInfo> result = new ArrayList<ProviderInfo>();
        for (ProviderInfo providerInfo : providerInfos) {
            if (providerInfo.getProtocolType().equalsIgnoreCase(protocol)) {
                result.add(providerInfo);
            }
        }
        return result;
    }
}
