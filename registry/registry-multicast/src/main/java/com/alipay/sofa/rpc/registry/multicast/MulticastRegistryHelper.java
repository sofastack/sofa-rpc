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
package com.alipay.sofa.rpc.registry.multicast;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * @author zhaowang
 * @version : MultiCastRegistryHelper.java, v 0.1 2020年03月04日 2:51 下午 zhaowang Exp $
 */
public class MulticastRegistryHelper extends RegistryUtils {

    public static void checkMulticastAddress(InetAddress multicastAddress) {
        if (!multicastAddress.isMulticastAddress()) {
            String message = "Invalid multicast address " + multicastAddress;
            if (multicastAddress instanceof Inet4Address) {
                throw new IllegalArgumentException(message + ", " +
                    "ipv4 multicast address scope: 224.0.0.0 - 239.255.255.255.");
            } else {
                throw new IllegalArgumentException(message + ", " + "ipv6 multicast address must start with ff, " +
                    "for example: ff01::1");
            }
        }
    }

    /**
     * 服务注册中心的Key
     *
     * @param config   配置
     * @param protocol 协议
     * @return 返回值
     */
    public static String buildListDataId(AbstractInterfaceConfig config, String protocol) {
        if (RpcConstants.PROTOCOL_TYPE_BOLT.equals(protocol)
            || RpcConstants.PROTOCOL_TYPE_TR.equals(protocol)) {
            return ConfigUniqueNameGenerator.getUniqueName(config) + "@DEFAULT";
        } else {
            return ConfigUniqueNameGenerator.getUniqueName(config) + "@" + protocol;
        }

    }

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

}