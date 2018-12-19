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
package com.alipay.sofa.rpc.server.rest;

import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.config.ProviderConfig;
import org.jboss.resteasy.plugins.server.resourcefactory.SingletonResource;

/**
 * SofaResourceFactory base on SingletonResource.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @see org.jboss.resteasy.plugins.server.resourcefactory.SingletonResource
 */
public class SofaResourceFactory extends SingletonResource {
    private final ProviderConfig providerConfig;
    private final String         serviceName;
    private final String         appName;

    public SofaResourceFactory(ProviderConfig providerConfig, Object object) {
        super(object);
        this.providerConfig = providerConfig;
        // 缓存服务名计算和应用名计算
        this.serviceName = ConfigUniqueNameGenerator.getServiceName(providerConfig);
        this.appName = providerConfig.getAppName();
    }

    public ProviderConfig getProviderConfig() {
        return providerConfig;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getAppName() {
        return appName;
    }
}
