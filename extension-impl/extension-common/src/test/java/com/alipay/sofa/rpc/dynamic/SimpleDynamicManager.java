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
import com.alipay.sofa.rpc.ext.Extension;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bystander
 * @version : SimpleDynamicManager.java, v 0.1 2019年04月12日 11:52 bystander Exp $
 */
@Extension("simple")
public class SimpleDynamicManager extends DynamicConfigManager {

    private Map<String, Properties> contents = new ConcurrentHashMap<String, Properties>();

    public SimpleDynamicManager(String appName) {
        super(appName);
    }

    @Override
    public void initServiceConfiguration(String service) {
        final Properties value = new Properties();
        value.setProperty("timeout", "5000");
        value.setProperty("methodName" + "." + "timeout", "1000");

        contents.put(service, value);
    }

    @Override
    public String getProviderServiceProperty(String service, String key) {
        return null;
    }

    @Override
    public String getConsumerServiceProperty(String service, String key) {
        return null;
    }

    @Override
    public String getProviderMethodProperty(String service, String method, String key) {
        return null;
    }

    @Override
    public String getConsumerMethodProperty(String service, String method, String key) {
        return contents.get(service).getProperty(method + "." + key);
    }

    @Override
    public AuthRuleGroup getServiceAuthRule(String service) {
        return new AuthRuleGroup();
    }

}