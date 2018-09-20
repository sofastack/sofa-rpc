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
package com.alipay.sofa.rpc.registry.utils;

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.client.ProviderStatus;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.Version;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Common Utils for Registry extensions
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public class RegistryUtils {

    /**
     * Convert provider to url.
     *
     * @param providerConfig the ProviderConfig
     * @return the url list
     */
    public static List<String> convertProviderToUrls(ProviderConfig providerConfig) {
        List<ServerConfig> servers = providerConfig.getServer();
        if (servers != null && !servers.isEmpty()) {
            List<String> urls = new ArrayList<String>();
            for (ServerConfig server : servers) {
                StringBuilder sb = new StringBuilder(200);
                String host = server.getVirtualHost(); // 虚拟ip
                if (host == null) {
                    host = server.getHost();
                    if (NetUtils.isLocalHost(host) || NetUtils.isAnyHost(host)) {
                        host = SystemInfo.getLocalHost();
                    }
                }
                sb.append(server.getProtocol()).append("://").append(host).append(":")
                    .append(server.getPort()).append(server.getContextPath()).append("?version=1.0")
                    .append(
                        getKeyPairs(RpcConstants.CONFIG_KEY_UNIQUEID, providerConfig.getUniqueId()))
                    .append(
                        getKeyPairs(RpcConstants.CONFIG_KEY_INTERFACE, providerConfig.getInterfaceId()))
                    .append(
                        getKeyPairs(RpcConstants.CONFIG_KEY_TIMEOUT, providerConfig.getTimeout()))
                    .append(getKeyPairs("delay", providerConfig.getDelay()))
                    .append(getKeyPairs("id", providerConfig.getId()))
                    .append(
                        getKeyPairs(RpcConstants.CONFIG_KEY_DYNAMIC, providerConfig.isDynamic()))
                    .append(getKeyPairs(ProviderInfoAttrs.ATTR_WEIGHT, providerConfig.getWeight()))
                    .append(getKeyPairs("accepts", server.getAccepts()))
                    .append(getKeyPairs(ProviderInfoAttrs.ATTR_START_TIME, RpcRuntimeContext.now()))
                    .append(
                        getKeyPairs(RpcConstants.CONFIG_KEY_APP_NAME, providerConfig.getAppName()))
                    .append(getKeyPairs(RpcConstants.CONFIG_KEY_SERIALIZATION,
                        providerConfig.getSerialization()))
                    .append(convertMap2Pair(providerConfig.getParameters()));
                addCommonAttrs(sb);
                urls.add(sb.toString());
            }
            return urls;
        }
        return null;
    }

    /**
     * Convert consumer to url.
     *
     * @param consumerConfig the ConsumerConfig
     * @return the url list
     */
    public static String convertConsumerToUrl(ConsumerConfig consumerConfig) {
        StringBuilder sb = new StringBuilder(200);
        String host = SystemInfo.getLocalHost();
        sb.append(consumerConfig.getProtocol()).append("://").append(host).append("?version=1.0")
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_UNIQUEID, consumerConfig.getUniqueId()))
            .append(getKeyPairs("pid", RpcRuntimeContext.PID))
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_TIMEOUT, consumerConfig.getTimeout()))
            .append(getKeyPairs("id", consumerConfig.getId()))
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_GENERIC, consumerConfig.isGeneric()))
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_INTERFACE, consumerConfig.getInterfaceId()))
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_APP_NAME, consumerConfig.getAppName()))
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_SERIALIZATION,
                consumerConfig.getSerialization()))
            .append(getKeyPairs(ProviderInfoAttrs.ATTR_START_TIME, RpcRuntimeContext.now()))
            .append(convertMap2Pair(consumerConfig.getParameters()));
        addCommonAttrs(sb);
        return sb.toString();
    }

    /**
     * Gets key pairs.
     *
     * @param key   the key
     * @param value the value
     * @return the key pairs
     */
    public static String getKeyPairs(String key, Object value) {
        if (value != null) {
            return "&" + key + "=" + value.toString();
        } else {
            return "";
        }
    }

    /**
     * 加入一些公共的额外属性
     *
     * @param sb 属性
     */
    private static void addCommonAttrs(StringBuilder sb) {
        sb.append(getKeyPairs("pid", RpcRuntimeContext.PID));
        sb.append(getKeyPairs("language", "java"));
        sb.append(getKeyPairs(RpcConstants.CONFIG_KEY_RPC_VERSION, Version.RPC_VERSION + ""));
    }

    /**
     * 转换 map to url pair
     *
     * @param map 属性
     */
    private static String convertMap2Pair(Map<String, String> map) {

        if (CommonUtils.isEmpty(map)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder(128);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(getKeyPairs(entry.getKey(), entry.getValue()));
        }

        return sb.toString();
    }

    public static String buildProviderPath(String rootPath, AbstractInterfaceConfig config) {
        return rootPath + "sofa-rpc/" + config.getInterfaceId() + "/providers";
    }

    public static String buildConsumerPath(String rootPath, AbstractInterfaceConfig config) {
        return rootPath + "sofa-rpc/" + config.getInterfaceId() + "/consumers";
    }

    public static String buildConfigPath(String rootPath, AbstractInterfaceConfig config) {
        return rootPath + "sofa-rpc/" + config.getInterfaceId() + "/configs";
    }

    /**
     * Read the warmup weight parameter,
     * decide whether to switch the state to the preheating period,
     * and set the corresponding parameters during the preheating period.
     *
     * @param providerInfo
     */
    public static void processWarmUpWeight(ProviderInfo providerInfo) {

        String warmupTimeStr = providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_TIME);
        String warmupWeightStr = providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT);
        String startTimeStr = providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_START_TIME);

        if (StringUtils.isNotBlank(warmupTimeStr) && StringUtils.isNotBlank(warmupWeightStr)
            && StringUtils.isNotBlank(startTimeStr)) {

            long warmupTime = CommonUtils.parseLong(warmupTimeStr, 0);
            int warmupWeight = CommonUtils.parseInt(warmupWeightStr,
                Integer.parseInt(providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_WEIGHT)));
            long startTime = CommonUtils.parseLong(startTimeStr, 0);
            long warmupEndTime = startTime + warmupTime;

            // set for dynamic
            providerInfo.setDynamicAttr(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT, warmupWeight);
            providerInfo.setDynamicAttr(ProviderInfoAttrs.ATTR_WARM_UP_END_TIME, warmupEndTime);
            providerInfo.setStatus(ProviderStatus.WARMING_UP);
        }

        // remove from static
        providerInfo.getStaticAttrs().remove(ProviderInfoAttrs.ATTR_WARMUP_TIME);
        providerInfo.getStaticAttrs().remove(ProviderInfoAttrs.ATTR_WARMUP_WEIGHT);

    }
}
