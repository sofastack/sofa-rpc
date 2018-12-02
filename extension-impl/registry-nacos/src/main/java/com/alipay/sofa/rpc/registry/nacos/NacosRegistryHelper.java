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
package com.alipay.sofa.rpc.registry.nacos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.nacos.api.naming.pojo.Cluster;
import com.alibaba.nacos.api.naming.pojo.Instance;
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

/**
 * The type Nacos registry helper.
 * @author <a href=mailto:jervyshi@gmail.com>JervyShi</a>
 * @version $Id : NacosRegistryHelper.java, v 0.1 2018-10-05 21:47 JervyShi Exp $$
 */
class NacosRegistryHelper {

    static final String DEFAULT_CLUSTER = "default-cluster";

    /**
     * Convert provider to instances list.
     *
     * @param providerConfig the provider config 
     * @return the list
     */
    static List<Instance> convertProviderToInstances(ProviderConfig providerConfig) {
        @SuppressWarnings("unchecked")
        List<ServerConfig> servers = providerConfig.getServer();
        if (servers != null && !servers.isEmpty()) {
            List<Instance> instances = new ArrayList<Instance>();
            for (ServerConfig server : servers) {
                Instance instance = new Instance();
                instance.setClusterName(DEFAULT_CLUSTER);

                // set host port
                String host = server.getVirtualHost();
                if (host == null) {
                    host = server.getHost();
                    if (NetUtils.isLocalHost(host) || NetUtils.isAnyHost(host)) {
                        host = SystemInfo.getLocalHost();
                    }
                }
                instance.setIp(host);
                instance.setPort(server.getPort());

                // set meta data
                Map<String, String> metaData = RegistryUtils.convertProviderToMap(providerConfig, server);
                instance.setMetadata(metaData);

                instances.add(instance);
            }
            return instances;
        }
        return null;
    }

    /**
     * Convert instances to providers list.
     *
     * @param allInstances the all instances 
     * @return the list
     */
    static List<ProviderInfo> convertInstancesToProviders(List<Instance> allInstances) {
        List<ProviderInfo> providerInfos = new ArrayList<ProviderInfo>();
        if (CommonUtils.isEmpty(allInstances)) {
            return providerInfos;
        }

        for (Instance instance : allInstances) {
            String url = convertInstanceToUrl(instance);
            ProviderInfo providerInfo = ProviderHelper.toProviderInfo(url);
            providerInfos.add(providerInfo);
        }
        return providerInfos;
    }

    /**
     * Match provider infos list.
     *
     * @param consumerConfig the consumer config 
     * @param providerInfos the provider infos 
     * @return the list
     */
    static List<ProviderInfo> matchProviderInfos(ConsumerConfig consumerConfig,
                                                 List<ProviderInfo> providerInfos) {
        String protocol = consumerConfig.getProtocol();
        List<ProviderInfo> result = new ArrayList<ProviderInfo>();
        for (ProviderInfo providerInfo : providerInfos) {
            if (providerInfo.getProtocolType().equalsIgnoreCase(protocol) &&
                StringUtils.equals(consumerConfig.getUniqueId(),
                    providerInfo.getAttr(ProviderInfoAttrs.ATTR_UNIQUEID))) {
                result.add(providerInfo);
            }
        }
        return result;
    }

    private static String convertInstanceToUrl(Instance instance) {
        Map<String, String> metaData = instance.getMetadata();
        if (metaData == null) {
            metaData = new HashMap<String, String>();
        }
        String uri = metaData.get(RpcConstants.CONFIG_KEY_PROTOCOL) + "://" + instance.getIp() + ":" +
            instance.getPort();

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : metaData.entrySet()) {
            sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }
        if (sb.length() > 0) {
            uri += sb.replace(0, 1, "?").toString();
        }
        return uri;
    }
}
