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
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.dynamic.ConfigChangeType;
import com.alipay.sofa.rpc.dynamic.ConfigChangedEvent;
import com.alipay.sofa.rpc.dynamic.DynamicConfigKeyHelper;
import com.alipay.sofa.rpc.dynamic.DynamicConfigKeys;
import com.alipay.sofa.rpc.dynamic.DynamicConfigManager;
import com.alipay.sofa.rpc.dynamic.DynamicHelper;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ConfigListener;
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

    private static final String APOLLO_APPID_KEY = "app.id";

    private static final String APOLLO_ADDR_KEY = "apollo.meta";

    private static final String APOLLO_CLUSTER_KEY = "apollo.cluster";

    private static final String APOLLO_PARAM_APPID_KEY = "appId";

    private static final String APOLLO_PARAM_CLUSTER_KEY = "cluster";

    private static final String APOLLO_PROTOCOL_PREFIX = "http://";
    private final ConcurrentMap<String, ApolloListener> watchListenerMap = new ConcurrentHashMap<>();
    private Config config;

    protected ApolloDynamicConfigManager(String appName) {
        super(appName, SofaConfigs.getOrCustomDefault(DynamicConfigKeys.CONFIG_CENTER_ADDRESS, ""));
        if (getDynamicUrl() != null) {
            if (StringUtils.isNotBlank(getDynamicUrl().getParam(APOLLO_PARAM_APPID_KEY))) {
                System.setProperty(APOLLO_APPID_KEY, getDynamicUrl().getParam(APOLLO_PARAM_APPID_KEY));
            }
            if (StringUtils.isNotBlank(getDynamicUrl().getAddress())) {
                System.setProperty(APOLLO_ADDR_KEY, APOLLO_PROTOCOL_PREFIX + getDynamicUrl().getAddress());
            }
            if (StringUtils.isNotBlank(getDynamicUrl().getParam(APOLLO_PARAM_CLUSTER_KEY))) {
                System.setProperty(APOLLO_CLUSTER_KEY, getDynamicUrl().getParam(APOLLO_PARAM_CLUSTER_KEY));
            }
        }
        config = ConfigService.getAppConfig();
    }

    @Override
    public void initServiceConfiguration(String service) {
        // TODO 暂不支持
    }

    @Override
    public void initServiceConfiguration(String service, ConfigListener listener) {
        String rawConfig = config.getProperty(service, "");
        if (StringUtils.isNotBlank(rawConfig)) {
            listener.process(new ConfigChangedEvent(service, rawConfig));
        }
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
    public void addListener(String key, ConfigListener listener) {
        ApolloListener apolloListener = watchListenerMap.computeIfAbsent(key, k -> new ApolloListener());
        apolloListener.addListener(listener);
        config.addChangeListener(apolloListener, Collections.singleton(key));
    }

    public class ApolloListener implements ConfigChangeListener {

        private Set<ConfigListener> listeners = new CopyOnWriteArraySet<>();

        ApolloListener() {
        }

        @Override
        public void onChange(com.ctrip.framework.apollo.model.ConfigChangeEvent changeEvent) {
            for (String key : changeEvent.changedKeys()) {
                ConfigChange change = changeEvent.getChange(key);
                ConfigChangedEvent event =
                        new ConfigChangedEvent(key, change.getNewValue(), getChangeType(change));
                listeners.forEach(listener -> listener.process(event));
            }
        }

        private ConfigChangeType getChangeType(ConfigChange change) {
            if (change.getChangeType() == PropertyChangeType.DELETED) {
                return ConfigChangeType.DELETED;
            }
            if (change.getChangeType() == PropertyChangeType.ADDED) {
                return ConfigChangeType.ADDED;
            }
            return ConfigChangeType.MODIFIED;
        }

        void addListener(ConfigListener configListener) {
            this.listeners.add(configListener);
        }
    }
}