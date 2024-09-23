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
package com.alipay.sofa.rpc.dynamic.nacos;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.auth.AuthRuleGroup;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.dynamic.*;
import com.alipay.sofa.rpc.ext.Extension;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import static com.alipay.sofa.rpc.common.utils.StringUtils.KEY_SEPARATOR;

/**
 * @author Narziss
 * @version NaocsDynamicConfigManager.java, v 0.1 2024年07月26日 09:37 Narziss
 */

@Extension(value = "nacos", override = true)
public class NacosDynamicConfigManager extends DynamicConfigManager {

    private final static Logger LOGGER            = LoggerFactory.getLogger(NacosDynamicConfigManager.class);

    private static final long   DEFAULT_TIMEOUT   = 5000;

    private final String        address;

    private ConfigService       configService;

    private Properties          nacosConfig       = new Properties();

    private final String        group;

    private final ConcurrentMap<String, NacosConfigListener> watchListenerMap = new ConcurrentHashMap<>();

    protected NacosDynamicConfigManager(String appName) {
        super(appName);
        address=SofaConfigs.getOrDefault(DynamicConfigKeys.NACOS_ADDRESS);
        group = DynamicConfigKeys.DEFAULT_GROUP;
        try {
            nacosConfig.put(PropertyKeyConst.SERVER_ADDR, address);
            configService = ConfigFactory.createConfigService(nacosConfig);

        } catch (NacosException e) {
            LOGGER.error("Failed to create ConfigService", e);
        }
    }

    protected NacosDynamicConfigManager(String appName, String address) {
        super(appName);
        this.address = address;
        group = DynamicConfigKeys.DEFAULT_GROUP;
        try {
            nacosConfig.put(PropertyKeyConst.SERVER_ADDR, address);
            configService = ConfigFactory.createConfigService(nacosConfig);

        } catch (NacosException e) {
            LOGGER.error("Failed to create ConfigService", e);
        }
    }

    @Override
    public void initServiceConfiguration(String service) {
        //TODO not now

    }

    @Override
    public String getProviderServiceProperty(String service, String key) {
        try {
            String configValue = configService.getConfig(
                DynamicConfigKeyHelper.buildProviderServiceProKey(service, key),
                    group, DEFAULT_TIMEOUT);
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (NacosException e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getConsumerServiceProperty(String service, String key) {
        try {
            String configValue = configService.getConfig(
                DynamicConfigKeyHelper.buildConsumerServiceProKey(service, key),
                    group, DEFAULT_TIMEOUT);
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (NacosException e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getProviderMethodProperty(String service, String method, String key) {
        try {
            String configValue = configService.getConfig(
                DynamicConfigKeyHelper.buildProviderMethodProKey(service, method, key),
                    group, DEFAULT_TIMEOUT);
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (NacosException e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getConsumerMethodProperty(String service, String method, String key) {
        try {
            String configValue = configService.getConfig(
                    buildDataId(DynamicConfigKeyHelper.buildConsumerMethodProKey(service, method, key)),
                    group, DEFAULT_TIMEOUT);
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (NacosException e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    public String buildDataId(String proKey) {
        return getAppName() + KEY_SEPARATOR + proKey;
    }

    @Override
    public AuthRuleGroup getServiceAuthRule(String service) {
        //TODO 暂不支持
        return null;
    }

    @Override
    public String getConfig(String key){
        try {
            return configService.getConfig(getAppName()+ KEY_SEPARATOR +key, group, DEFAULT_TIMEOUT);
        } catch (NacosException e) {
            LOGGER.error("Failed to getConfig for key:{}, group:{}", key, group, e);
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public void addListener(String key, ConfigListener listener) {
        NacosConfigListener nacosConfigListener = watchListenerMap.computeIfAbsent(
                key, k -> createTargetListener(key));
        nacosConfigListener.addListener(listener);
        try {
            configService.addListener(getAppName()+KEY_SEPARATOR +key, group, nacosConfigListener);
        } catch (NacosException e) {
            LOGGER.error("Failed to add listener for key:{}, group:{}", key, group, e);
        }
    }

    private NacosConfigListener createTargetListener(String key) {
        NacosConfigListener configListener = new NacosConfigListener();
        configListener.fillContext(key, group);
        return configListener;
    }

    public class NacosConfigListener extends AbstractSharedListener {

        private Set<ConfigListener> listeners = new CopyOnWriteArraySet<>();
        /**
         * cache data to store old value
         */
        private Map<String, String> cacheData = new ConcurrentHashMap<>();

        @Override
        public Executor getExecutor() {
            return null;
        }

        /**
         * receive
         *
         * @param dataId     data ID
         * @param group      group
         * @param configInfo content
         */
        @Override
        public void innerReceive(String dataId, String group, String configInfo) {
            String oldValue = cacheData.get(dataId);
            ConfigChangedEvent event =
                    new ConfigChangedEvent(dataId, group, configInfo, getChangeType(configInfo, oldValue));
            if (configInfo == null) {
                cacheData.remove(dataId);
            } else {
                cacheData.put(dataId, configInfo);
            }
            listeners.forEach(listener -> listener.process(event));
        }

        void addListener(ConfigListener configListener) {

            this.listeners.add(configListener);
        }

        private ConfigChangeType getChangeType(String configInfo, String oldValue) {
            if (StringUtils.isBlank(configInfo)) {
                return ConfigChangeType.DELETED;
            }
            if (StringUtils.isBlank(oldValue)) {
                return ConfigChangeType.ADDED;
            }
            return ConfigChangeType.MODIFIED;
        }
    }
}