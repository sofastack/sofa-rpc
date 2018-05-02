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
package com.alipay.sofa.rpc.registry.mesh.model;

/**
 * @author bystander
 * @version $Id: ApplicationInfoRequest.java, v 0.1 2018年04月20日 3:19 PM bystander Exp $
 */
public class ApplicationInfoRequest {

    private boolean antShareCloud;   //true表示蚂蚁金融云共享版，其它环境为false 默认为 false
    private String  dataCenter;      //当前应用所在的数据中心
    private String  appName;         //应用名 必填
    private String  zone;            //当前应用所属的zone信息
    private String  registryEndpoint; //配置中心Endpoint地址 可空
    private String  accessKey;       //金融云共享版环境下的AccessKey，其它环境可不传
    private String  secretKey;       //金融云共享版环境下的密码，其它环境可不传

    public boolean isAntShareCloud() {
        return antShareCloud;
    }

    public void setAntShareCloud(boolean antShareCloud) {
        this.antShareCloud = antShareCloud;
    }

    public String getDataCenter() {
        return dataCenter;
    }

    public void setDataCenter(String dataCenter) {
        this.dataCenter = dataCenter;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getRegistryEndpoint() {
        return registryEndpoint;
    }

    public void setRegistryEndpoint(String registryEndpoint) {
        this.registryEndpoint = registryEndpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}