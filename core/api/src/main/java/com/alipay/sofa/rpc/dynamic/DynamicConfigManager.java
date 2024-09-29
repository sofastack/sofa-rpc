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
package com.alipay.sofa.rpc.dynamic;

import com.alipay.sofa.rpc.auth.AuthRuleGroup;
import com.alipay.sofa.rpc.ext.Extensible;
import com.alipay.sofa.rpc.listener.ConfigListener;

/**
 *
 * @author bystander
 * @version : DynamicManager.java, v 0.1 2019年04月12日 11:35 bystander Exp $
 */
@Extensible(singleton = true)
public abstract class DynamicConfigManager {

    private String appName;

    private String address;

    private String params[];

    protected DynamicConfigManager(String appName, String remainUrl) {
        this.appName = appName;
        int queryIndex = remainUrl.indexOf("?");
        this.address = (queryIndex > -1) ? remainUrl.substring(0, queryIndex) : remainUrl;
        if (queryIndex > -1 && queryIndex < remainUrl.length() - 1) {
            String query = remainUrl.substring(queryIndex + 1);
            this.params = query.split("&");
        }
    }

    protected String getAppName() {
        return appName;
    }

    protected String getAddress() {
        return address;
    }

    protected String[] getParams() {
        return params;
    }

    /**
     * Init service's governance related configuration.
     * Such as auth rules、lb rules
     *
     * @param service target service
     */
    public abstract void initServiceConfiguration(String service);

    /**
     * Init service's governance related configuration.
     * Such as auth rules、lb rules
     *
     * @param service  target service
     * @param listener config listener
     */
    public abstract void initServiceConfiguration(String service, ConfigListener listener);

    /**
     * Get provider service related property.
     *
     * @param service target service
     * @param key property key
     * @return property value
     */
    public abstract String getProviderServiceProperty(String service, String key);

    /**
     * Get consumer service related property.
     *
     * @param service target service
     * @param key property key
     * @return property value
     */
    public abstract String getConsumerServiceProperty(String service, String key);

    /**
     * Get provider method related property.
     *
     * @param service target service
     * @param method target method
     * @param key property key
     * @return property value
     */
    public abstract String getProviderMethodProperty(String service, String method, String key);

    /**
     * Get consumer method related property.
     *
     * @param service target service
     * @param method target method
     * @param key property key
     * @return property value
     */
    public abstract String getConsumerMethodProperty(String service, String method, String key);

    /**
     * Get service's auth rules.
     *
     * @param service target service
     * @return auth rules
     */
    public abstract AuthRuleGroup getServiceAuthRule(String service);

    /**
     * Add config listener.
     *
     * @param key config key
     * @param listener config listener
     */
    public abstract void addListener(String key, ConfigListener listener);

}