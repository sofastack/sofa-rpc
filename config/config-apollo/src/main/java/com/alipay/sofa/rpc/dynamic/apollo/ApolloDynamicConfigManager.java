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
package com.alipay.sofa.rpc.dynamic.apollo;

import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.auth.AuthRuleGroup;
import com.alipay.sofa.rpc.dynamic.*;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author bystander
 * @version : ApolloDynamicConfigManager.java, v 0.1 2019年06月04日 20:29 bystander Exp $
 * apollo.cluster need to config or not
 * app.id need to config
 */

@Extension(value = "apollo", override = true)
public class ApolloDynamicConfigManager extends DynamicConfigManager {

    Logger LOGGER = LoggerFactory.getLogger(ApolloDynamicConfigManager.class);

    private static final String APOLLO_APPID_KEY = "app.id";

    private static final String APOLLO_ADDR_KEY = "apollo.meta";

    private static final String APOLLO_PROTOCOL_PREFIX = "http://";

    private Config config;

    private final ConcurrentMap<String, ApolloListener> watchListenerMap = new ConcurrentHashMap<>();

    protected ApolloDynamicConfigManager(String appName) {
        super(appName);
        System.setProperty(APOLLO_APPID_KEY, appName);
        System.setProperty(APOLLO_ADDR_KEY, APOLLO_PROTOCOL_PREFIX + SofaConfigs.getOrDefault(DynamicConfigKeys.APOLLO_ADDRESS));
        config = ConfigService.getConfig(DynamicConfigKeys.DEFAULT_GROUP);
    }

    protected ApolloDynamicConfigManager(String appName,String address) {
        super(appName);
        System.setProperty(APOLLO_APPID_KEY, appName);
        System.setProperty(APOLLO_ADDR_KEY, APOLLO_PROTOCOL_PREFIX + address);
        config = ConfigService.getConfig(DynamicConfigKeys.DEFAULT_GROUP);
    }

    @Override
    public void initServiceConfiguration(String service) {
        //TODO not now
    }

    @Override
    public String getProviderServiceProperty(String service, String key) {
        return config.getProperty(DynamicConfigKeyHelper.buildProviderServiceProKey(service, key),
            DynamicHelper.DEFAULT_DYNAMIC_VALUE);
    }

    @Override
    public String getConsumerServiceProperty(String service, String key) {
        return config.getProperty(DynamicConfigKeyHelper.buildConsumerServiceProKey(service, key),
            DynamicHelper.DEFAULT_DYNAMIC_VALUE);

    }

    @Override
    public String getProviderMethodProperty(String service, String method, String key) {
        return config.getProperty(DynamicConfigKeyHelper.buildProviderMethodProKey(service, method, key),
            DynamicHelper.DEFAULT_DYNAMIC_VALUE);
    }

    @Override
    public String getConsumerMethodProperty(String service, String method, String key) {
        return config.getProperty(DynamicConfigKeyHelper.buildConsumerMethodProKey(service, method, key),
            DynamicHelper.DEFAULT_DYNAMIC_VALUE);

    }

    @Override
    public AuthRuleGroup getServiceAuthRule(String service) {
        //TODO 暂不支持
        return null;
    }

    @Override
    public String getConfig(String key){
        return config.getProperty(key, DynamicHelper.DEFAULT_DYNAMIC_VALUE);
    }

    @Override
    public void addListener(String key, ConfigListener listener){
        ApolloListener apolloListener = watchListenerMap.computeIfAbsent(key, k -> new ApolloListener());
        apolloListener.addListener(listener);
        config.addChangeListener(apolloListener, Collections.singleton(key));
    }

    public class ApolloListener implements ConfigChangeListener {

        private Set<ConfigListener> listeners = new CopyOnWriteArraySet<>();

        ApolloListener() {}

        @Override
        public void onChange(com.ctrip.framework.apollo.model.ConfigChangeEvent changeEvent) {
            for (String key : changeEvent.changedKeys()) {
                ConfigChange change = changeEvent.getChange(key);
                if ("".equals(change.getNewValue())) {
                        LOGGER.info("an empty rule is received for key: " + key);
                    return;
                }

                ConfigChangedEvent event =
                        new ConfigChangedEvent(key, change.getNamespace(), change.getNewValue(), getChangeType(change));
                listeners.forEach(listener -> listener.process(event));
            }
        }

        private ConfigChangeType getChangeType(ConfigChange change) {
            if (change.getChangeType() == PropertyChangeType.DELETED) {
                return ConfigChangeType.DELETED;
            }
            return ConfigChangeType.MODIFIED;
        }

        void addListener(ConfigListener configListener) {
            this.listeners.add(configListener);
        }
    }
}