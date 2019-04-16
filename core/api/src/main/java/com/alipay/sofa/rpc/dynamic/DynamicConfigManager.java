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

/**
 * @author bystander
 * @version : DynamicManager.java, v 0.1 2019年04月12日 11:35 bystander Exp $
 */
@Extensible(singleton = true)
public abstract class DynamicConfigManager {

    private String appName;

    protected DynamicConfigManager(String appName) {
        this.appName = appName;
    }

    public abstract void initServiceConfiguration(String service);

    public abstract String getAppProperty(String key);

    public abstract String getProviderServiceProperty(String service, String key);

    public abstract String getConsumerServiceProperty(String service, String key);

    public abstract String getProviderMethodProperty(String service, String method, String key);

    public abstract String getConsumerMethodProperty(String service, String method, String key);

    public abstract AuthRuleGroup getServiceAuthRule(String service);
}