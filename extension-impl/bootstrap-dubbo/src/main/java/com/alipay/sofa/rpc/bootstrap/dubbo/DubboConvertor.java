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
package com.alipay.sofa.rpc.bootstrap.dubbo;

import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.RegistryConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bystander
 * @version $Id: DubboConvetor.java, v 0.1 2018年12月10日 20:24 bystander Exp $
 */
public class DubboConvertor {

    public static void copyRegistries(com.alipay.sofa.rpc.config.AbstractInterfaceConfig sofaConfig,
                                      com.alibaba.dubbo.config.AbstractInterfaceConfig dubboConfig) {
        List<RegistryConfig> registryConfigs = sofaConfig.getRegistry();
        if (CommonUtils.isNotEmpty(registryConfigs)) {
            List<com.alibaba.dubbo.config.RegistryConfig> dubboRegistryConfigs =
                    new ArrayList<com.alibaba.dubbo.config.RegistryConfig>();
            for (RegistryConfig registryConfig : registryConfigs) {
                // 生成并丢到缓存里
                com.alibaba.dubbo.config.RegistryConfig dubboRegistryConfig = DubboSingleton.REGISTRY_MAP
                    .get(registryConfig);
                if (dubboRegistryConfig == null) {
                    dubboRegistryConfig = new com.alibaba.dubbo.config.RegistryConfig();
                    copyRegistryFields(registryConfig, dubboRegistryConfig);
                    com.alibaba.dubbo.config.RegistryConfig old = DubboSingleton.REGISTRY_MAP.putIfAbsent(
                        registryConfig, dubboRegistryConfig);
                    if (old != null) {
                        dubboRegistryConfig = old;
                    }
                }
                dubboRegistryConfigs.add(dubboRegistryConfig);
            }
            dubboConfig.setRegistries(dubboRegistryConfigs);
        } else {
            com.alibaba.dubbo.config.RegistryConfig dubboRegistryConfig = new com.alibaba.dubbo.config.RegistryConfig();
            dubboRegistryConfig.setAddress(com.alibaba.dubbo.config.RegistryConfig.NO_AVAILABLE);
            dubboConfig.setRegistry(dubboRegistryConfig);
        }
    }

    public static void copyRegistryFields(com.alipay.sofa.rpc.config.RegistryConfig sofaRegistryConfig,
                                          com.alibaba.dubbo.config.RegistryConfig dubboRegistryConfig) {
        dubboRegistryConfig.setAddress(sofaRegistryConfig.getAddress());
        dubboRegistryConfig.setProtocol(sofaRegistryConfig.getProtocol());
        dubboRegistryConfig.setRegister(sofaRegistryConfig.isRegister());
        dubboRegistryConfig.setSubscribe(sofaRegistryConfig.isSubscribe());
        dubboRegistryConfig.setAddress(sofaRegistryConfig.getAddress());
        dubboRegistryConfig.setTimeout(sofaRegistryConfig.getTimeout());
        dubboRegistryConfig.setId(sofaRegistryConfig.getId());
        dubboRegistryConfig.setParameters(sofaRegistryConfig.getParameters());
    }
}