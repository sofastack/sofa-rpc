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

import com.alipay.sofa.rpc.auth.AuthRuleGroup;
import com.alipay.sofa.rpc.dynamic.DynamicConfigKeyHelper;
import com.alipay.sofa.rpc.dynamic.DynamicConfigManager;
import com.alipay.sofa.rpc.dynamic.DynamicHelper;
import com.alipay.sofa.rpc.ext.Extension;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;

/**
 * @author bystander
 * @version : ApolloDynamicConfigManager.java, v 0.1 2019年06月04日 20:29 bystander Exp $
 * apollo.cluster need to config or not
 * app.id need to config
 */

@Extension(value = "apollo", override = true)
public class ApolloDynamicConfigManager extends DynamicConfigManager {

    private Config config;

    protected ApolloDynamicConfigManager(String appName) {
        super(appName);
        config = ConfigService.getAppConfig();
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
}