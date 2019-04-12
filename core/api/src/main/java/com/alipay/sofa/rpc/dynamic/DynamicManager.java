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

import com.alipay.sofa.rpc.ext.Extensible;

import java.util.Properties;

/**
 * @author bystander
 * @version : DynamicManager.java, v 0.1 2019年04月12日 11:35 bystander Exp $
 */
@Extensible(singleton = true)
public abstract class DynamicManager {

    /**
     * appname appname
     */
    private String appName;

    /**
     * init by appName
     * @param appName
     */
    protected DynamicManager(String appName) {
        this.appName = appName;
    }

    /**
     * init service ,which service is servicename:1.0.method.xx=1000
     * @param type 类型,consumers,providers
     * @param service
     * @return
     */
    public abstract Properties initServiceConfigutration(String type, String service);

    /**
     * fetch the value of the specify key
     * @param type 类型,consumers,providers
     * @param service
     * @param key you need specify
     * @return
     */
    public abstract String fetchKey(String type, String service, String key);

}