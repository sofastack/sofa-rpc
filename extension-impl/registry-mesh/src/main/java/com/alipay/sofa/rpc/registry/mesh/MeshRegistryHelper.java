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
package com.alipay.sofa.rpc.registry.mesh;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;

/**
 * Util method of mesh registry.
 *
 * @author <a href="mailto:zhiyuan.lzy@antfin.com">zhiyuan.lzy</a>
 */
public class MeshRegistryHelper {

    /**
     * 转为服务端提供者对象
     *
     * @param config 服务提供者配置
     * @param server 服务端
     * @return 本地服务提供者对象
     */
    public static ProviderInfo convertProviderToProviderInfo(ProviderConfig config, ServerConfig server) {
        ProviderInfo providerInfo = new ProviderInfo()
            .setPort(server.getPort())
            .setWeight(config.getWeight())
            .setSerializationType(config.getSerialization())
            .setProtocolType(server.getProtocol())
            .setPath(server.getContextPath())
            .setStaticAttrs(config.getParameters());
        String host = server.getHost();
        if (NetUtils.isLocalHost(host) || NetUtils.isAnyHost(host)) {
            host = SystemInfo.getLocalHost();
        }
        providerInfo.setHost(host);
        return providerInfo;
    }

    /**
     * 配置中心的Key
     *
     * @param config   配置
     * @param protocol 协议
     * @return 返回值
     */
    public static String buildMeshKey(AbstractInterfaceConfig config, String protocol) {
        return ConfigUniqueNameGenerator.getUniqueName(config);
    }

}
