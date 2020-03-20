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

import com.alipay.sofa.rpc.common.utils.StringUtils;

/**
 * 统一的配置名称生成器
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ConfigUniqueNameGenerator {

    /**
     * 得到服务唯一名称，无需兼容之前的版本
     *
     * @param interfaceConfig 服务提供者或者服务消费者配置
     * @return 服务唯一名称
     * @since 5.4.0
     */
    public static String getServiceName(AbstractInterfaceConfig interfaceConfig) {
        String uniqueId = interfaceConfig.getUniqueId();
        return interfaceConfig.getInterfaceId() + (StringUtils.isEmpty(uniqueId) ? "" : ":" + uniqueId);
    }

    /**
     * 唯一标识UniqueName的产生方法，主要用于内部找接口等，格式为interface:version[:uniqueId]
     *
     * @param interfaceConfig 服务提供者或者服务消费者配置
     * @return 配置唯一名字
     */
    public static String getUniqueName(AbstractInterfaceConfig interfaceConfig) {
        // 加上 1.0 是为了兼容之前的版本
        String version = interfaceConfig.getVersion();
        String uniqueId = interfaceConfig.getUniqueId();
        return interfaceConfig.getInterfaceId()
            + (StringUtils.isEmpty(version) ? ":1.0" : ":" + version)
            + (StringUtils.isEmpty(uniqueId) ? "" : ":" + uniqueId);
    }

    /**
     * 唯一标识UniqueName的产生方法，主要用于外部服务发现等，格式为interface:version[:uniqueId]@protocol
     *
     * @param providerConfig 服务端提供者配置
     * @param protocol       协议
     * @return 配置唯一名字
     */
    public static String getUniqueNameProtocol(ProviderConfig providerConfig, String protocol) {
        if (StringUtils.isNotEmpty(protocol)) {
            return getUniqueName(providerConfig) + "@" + protocol;
        } else {
            return getUniqueName(providerConfig);
        }
    }

    /**
     * 解析唯一标识UniqueName得到接口名
     *
     * @param uniqueName 服务唯一标识
     * @return 接口名
     */
    public static String getInterfaceName(String uniqueName) {
        if (StringUtils.isEmpty(uniqueName)) {
            return uniqueName;
        }
        int index = uniqueName.indexOf(':');
        return index < 0 ? uniqueName : uniqueName.substring(0, index);
    }

    /**
     * 唯一标识UniqueName的产生方法，主要用于外部服务发现等
     *
     * @param consumerConfig 服务端调用者配置
     * @return 配置唯一名字
     */
    public static String getUniqueNameProtocol(ConsumerConfig consumerConfig) {
        return getUniqueName(consumerConfig) + "@" + consumerConfig.getProtocol();
    }
}
