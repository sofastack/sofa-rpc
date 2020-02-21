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
package com.alipay.sofa.rpc.registry.nacos;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The type Nacos registry provider observer.
 * @author <a href=mailto:jervyshi@gmail.com>JervyShi</a>
 */
public class NacosRegistryProviderObserver {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                                       LOGGER              = LoggerFactory
                                                                                              .getLogger(NacosRegistryProviderObserver.class);

    /**
     * The Provider add listener map.
     */
    private ConcurrentMap<ConsumerConfig, List<ProviderInfoListener>> providerListenerMap = new ConcurrentHashMap<ConsumerConfig, List<ProviderInfoListener>>();

    /**
     * Add provider listener.
     *
     * @param consumerConfig the consumer config  
     * @param listener the listener
     */
    void addProviderListener(ConsumerConfig consumerConfig, ProviderInfoListener listener) {
        if (listener != null) {
            RegistryUtils.initOrAddList(providerListenerMap, consumerConfig, listener);
        }
    }

    /**
     * Remove provider listener.
     *
     * @param consumerConfig the consumer config
     */
    void removeProviderListener(ConsumerConfig consumerConfig) {
        providerListenerMap.remove(consumerConfig);
    }

    /**
     * Update providers.
     *
     * @param config the config 
     * @param instances the instances
     */
    void updateProviders(ConsumerConfig config, List<Instance> instances) {
        if (LOGGER.isInfoEnabled(config.getAppName())) {
            LOGGER.infoWithApp(config.getAppName(),
                "Receive update provider: serviceName={}, size={}, data={}",
                NacosRegistryHelper.buildServiceName(config, config.getProtocol()), instances.size(),
                instances);
        }
        List<ProviderInfoListener> providerInfoListeners = providerListenerMap.get(config);
        if (CommonUtils.isNotEmpty(providerInfoListeners)) {
            List<ProviderInfo> providerInfos = NacosRegistryHelper.convertInstancesToProviders(instances);
            List<ProviderInfo> matchProviders = RegistryUtils.matchProviderInfos(config, providerInfos);

            for (ProviderInfoListener providerInfoListener : providerInfoListeners) {
                providerInfoListener
                    .updateAllProviders(Collections.singletonList(new ProviderGroup().addAll(matchProviders)));
            }
        }
    }
}
