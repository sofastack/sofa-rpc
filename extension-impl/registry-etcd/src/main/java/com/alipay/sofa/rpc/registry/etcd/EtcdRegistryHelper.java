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
package com.alipay.sofa.rpc.registry.etcd;

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EtcdRegistryHelper extends RegistryUtils {

    /**
     * make unique key by appending uuid to config path
     */
    public static String buildUniqueKey(ServiceInstance instance) {
        return instance.getServiceName() + ":" + instance.getProtocol() + ":" + instance.getUniqueId() + ":" +
            instance.getUuid();
    }

    public static String buildKeyPrefix(String serviceName, String protocol, String uniqueId) {
        return serviceName + ":" + protocol + ":" + uniqueId;
    }

    static List<ServiceInstance> convertProviderToInstances(ProviderConfig config) {
        List<ServerConfig> servers = config.getServer();
        if (CommonUtils.isNotEmpty(servers)) {
            List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
            for (ServerConfig server : servers) {
                ServiceInstance instance = new ServiceInstance();
                instance.setUuid(UUID.randomUUID().toString());
                instance.setUniqueId(config.getUniqueId());
                instance.setServiceName(config.getInterfaceId());

                // set host port
                String host = server.getVirtualHost();
                if (host == null) {
                    host = server.getHost();
                    if (NetUtils.isLocalHost(host) || NetUtils.isAnyHost(host)) {
                        host = SystemInfo.getLocalHost();
                    }
                }
                instance.setHost(host);
                instance.setPort(server.getPort());
                instance.setProtocol(server.getProtocol());

                // set meta data
                Map<String, String> metaData = RegistryUtils.convertProviderToMap(config, server);
                instance.setMetadata(metaData);
                instances.add(instance);
            }
            return instances;
        }
        return Collections.emptyList();
    }

    public static List<ProviderInfo> matchProviderInfos(ConsumerConfig config, List<ProviderInfo> providerInfos) {
        String protocol = config.getProtocol();
        List<ProviderInfo> result = new ArrayList<ProviderInfo>();
        for (ProviderInfo providerInfo : providerInfos) {
            if (providerInfo.getProtocolType().equalsIgnoreCase(protocol) &&
                StringUtils.equals(config.getUniqueId(), providerInfo.getAttr(ProviderInfoAttrs.ATTR_UNIQUEID))) {
                result.add(providerInfo);
            }
        }
        return result;
    }

    public static List<ProviderInfo> convertInstancesToProviders(List<ServiceInstance> allInstances) {
        List<ProviderInfo> providerInfos = new ArrayList<ProviderInfo>();
        if (CommonUtils.isEmpty(allInstances)) {
            return providerInfos;
        }

        for (ServiceInstance instance : allInstances) {
            String url = convertInstanceToUrl(instance);
            ProviderInfo providerInfo = ProviderHelper.toProviderInfo(url);
            providerInfos.add(providerInfo);
        }
        return providerInfos;
    }

    private static String convertInstanceToUrl(ServiceInstance instance) {
        Map<String, String> metaData = instance.getMetadata();
        if (metaData == null) {
            metaData = new HashMap<String, String>();
        }
        String uri = metaData.get(RpcConstants.CONFIG_KEY_PROTOCOL) + "://" + instance.getHost() + ":" +
            instance.getPort();

        StringBuilder sb = new StringBuilder("?");
        for (Map.Entry<String, String> entry : metaData.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
            uri += sb.toString();
        }
        return uri;
    }
}
