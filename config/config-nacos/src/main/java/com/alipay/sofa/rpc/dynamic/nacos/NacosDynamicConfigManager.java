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
import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.auth.AuthRuleGroup;
import com.alipay.sofa.rpc.common.config.RpcConfigKeys;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.dynamic.DynamicConfigKeyHelper;
import com.alipay.sofa.rpc.dynamic.DynamicConfigManager;
import com.alipay.sofa.rpc.dynamic.DynamicHelper;
import com.alipay.sofa.rpc.ext.Extension;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Properties;

/**
 * @author Narziss
 * @version NaocsDynamicConfigManager.java, v 0.1 2024年07月26日 09:37 Narziss
 */

@Extension(value = "nacos", override = true)
public class NacosDynamicConfigManager extends DynamicConfigManager {

    private final static Logger LOGGER            = LoggerFactory.getLogger(NacosDynamicConfigManager.class);
    private static final String DEFAULT_NAMESPACE = "sofa-rpc";
    private static final String ADDRESS           = SofaConfigs.getOrDefault(RpcConfigKeys.NACOS_ADDRESS);
    private static final String DEFAULT_GROUP     = "sofa-rpc";
    private static final long   DEFAULT_TIMEOUT   = 5000;
    private ConfigService       configService;
    private Properties          nacosConfig       = new Properties();
    private final String        appName;

    protected NacosDynamicConfigManager(String appName) {
        super(appName);
        if (StringUtils.isEmpty(appName)) {
            this.appName = DEFAULT_GROUP;
        } else {
            this.appName = appName;
        }
        try {
            nacosConfig.put(PropertyKeyConst.SERVER_ADDR, ADDRESS);
            nacosConfig.put(PropertyKeyConst.NAMESPACE, DEFAULT_NAMESPACE);
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
                appName, DEFAULT_TIMEOUT);
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
                appName, DEFAULT_TIMEOUT);
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
                appName, DEFAULT_TIMEOUT);
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (NacosException e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getConsumerMethodProperty(String service, String method, String key) {
        try {
            String configValue = configService.getConfig(
                DynamicConfigKeyHelper.buildConsumerMethodProKey(service, method, key),
                appName, DEFAULT_TIMEOUT);
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (NacosException e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public AuthRuleGroup getServiceAuthRule(String service) {
        //TODO 暂不支持
        return null;
    }
}