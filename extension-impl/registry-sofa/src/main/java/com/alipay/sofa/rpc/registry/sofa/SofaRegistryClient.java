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
package com.alipay.sofa.rpc.registry.sofa;

import com.alipay.sofa.registry.client.api.RegistryClient;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClient;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClientConfigBuilder;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.RegistryConfig;

/**
 * @author bystander
 * @version $Id: SofaRegistryClient.java, v 0.1 2018年03月13日 10:20 AM bystander Exp $
 */
public class SofaRegistryClient {

    public static final String    LOCAL_DATACENTER = "DefaultDataCenter";
    public static final String    LOCAL_REGION     = "DEFAULT_ZONE";

    private static RegistryClient registryClient;

    public static synchronized RegistryClient getRegistryClient(String appName, RegistryConfig registryConfig) {
        if (registryClient == null) {
            String address = registryConfig.getAddress();
            final String portStr = StringUtils.substringAfter(address, ":");
            RegistryClientConfig config = DefaultRegistryClientConfigBuilder.start()
                .setAppName(appName).setDataCenter(LOCAL_DATACENTER).setZone(LOCAL_REGION)
                .setRegistryEndpoint(StringUtils.substringBefore(address, ":"))
                .setRegistryEndpointPort(Integer.parseInt(portStr)).build();

            registryClient = new DefaultRegistryClient(config);
            ((DefaultRegistryClient) registryClient).init();
        }
        return registryClient;
    }
}