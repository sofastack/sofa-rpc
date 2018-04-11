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
package com.alipay.sofa.rpc.config;

import java.io.Serializable;

/**
 * 应用信息配置
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ApplicationConfig implements Serializable {

    /**
     * The App name.
     */
    protected String appName;

    /**
     * The App id.
     */
    protected String appId;

    /**
     * The Ins id.
     */
    protected String insId;

    /**
     * Gets app name.
     *
     * @return the app name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Sets app name.
     *
     * @param appName the app name
     * @return the app name
     */
    public ApplicationConfig setAppName(String appName) {
        this.appName = appName;
        return this;
    }

    /**
     * Gets app id.
     *
     * @return the app id
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Sets app id.
     *
     * @param appId the app id
     * @return the app id
     */
    public ApplicationConfig setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    /**
     * Gets ins id.
     *
     * @return the ins id
     */
    public String getInsId() {
        return insId;
    }

    /**
     * Sets ins id.
     *
     * @param insId the ins id
     * @return the ins id
     */
    public ApplicationConfig setInsId(String insId) {
        this.insId = insId;
        return this;
    }
}
