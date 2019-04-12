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

import com.alipay.sofa.rpc.ext.Extension;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bystander
 * @version : SimpleDynamicManager.java, v 0.1 2019年04月12日 11:52 bystander Exp $
 */
@Extension("simple")
public class SimpleDynamicManager extends DynamicManager {

    private Map<String, Properties> contents = new ConcurrentHashMap<String, Properties>();

    public SimpleDynamicManager(String appName) {
        super(appName);
    }

    @Override
    public Properties initServiceConfigutration(String type, String service) {
        final Properties value = new Properties();
        value.setProperty("timeout", "5000");
        value.setProperty("methodName" + "." + "timeout", "1000");

        contents.put(service, value);
        return value;
    }

    @Override
    public String fetchKey(String type, String service, String key) {
        return (String) contents.get(service).get(key);
    }
}