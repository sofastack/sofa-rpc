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
package com.alipay.sofa.rpc.registry;

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
import com.alipay.sofa.rpc.registry.common.ConsulURL;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for ConsulRegistry
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 */
public class ConsulRegistryHelper {

    /**
     * Convert provider to url.
     *
     * @param providerConfig the ProviderConfig
     * @return the url list
     */
    static List<String> convertProviderToUrls(ProviderConfig providerConfig) {
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
                sb.append(server.getProtocol()).append("://").append(host)
                    .append(":").append(server.getPort()).append(server.getContextPath())
                    .append("?uniqueId=").append(providerConfig.getUniqueId())
                    .append(getKeyPairs("version", "1.0"))
                    .append(getKeyPairs(RpcConstants.CONFIG_KEY_TIMEOUT, providerConfig.getTimeout()))
                    .append(getKeyPairs("delay", providerConfig.getDelay()))
                    .append(getKeyPairs("id", providerConfig.getId()))
                    .append(getKeyPairs(RpcConstants.CONFIG_KEY_DYNAMIC, providerConfig.isDynamic()))
                    .append(getKeyPairs(RpcConstants.CONFIG_KEY_WEIGHT, providerConfig.getWeight()))
                    .append(getKeyPairs("crossLang", providerConfig.getParameter("crossLang")))
                    .append(getKeyPairs("accepts", server.getAccepts()))
                    .append(getKeyPairs("interface", providerConfig.getInterfaceId()))
                    .append(getKeyPairs(ProviderInfoAttrs.ATTR_START_TIME, RpcRuntimeContext.START_TIME))
                    .append(getKeyPairs(RpcConstants.CONFIG_KEY_APP_NAME, providerConfig.getAppName()));
                addCommonAttrs(sb);
                urls.add(sb.toString());
            }
            return urls;
        }
        return null;
    }

    static ProviderInfo convertUrlToProvider(String providerPath, ConsulURL url) throws UnsupportedEncodingException {

        ProviderInfo providerInfo = ProviderHelper.toProviderInfo(url.toString());
        processWarmUpWeight(providerInfo);

        return providerInfo;
    }

    /**
     * Read the warmup weight parameter,
     * decide whether to switch the state to the preheating period,
     * and set the corresponding parameters during the preheating period.
     *
     * @param providerInfo
     */
    static void processWarmUpWeight(ProviderInfo providerInfo) {

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

    /**
     * Convert consumer to url.
     *
     * @param consumerConfig the ConsumerConfig
     * @return the url list
     */
    static String convertConsumerToUrl(ConsumerConfig consumerConfig) {
        StringBuilder sb = new StringBuilder(200);
        String host = SystemInfo.getLocalHost();
        sb.append(consumerConfig.getProtocol()).append("://").append(host).append("/")
            .append(consumerConfig.getInterfaceId())
            .append("?uniqueId=").append(consumerConfig.getUniqueId())
            .append(getKeyPairs("version", "1.0"))
            .append(getKeyPairs("pid", RpcRuntimeContext.PID))
            //.append(getKeyPairs("randomPort", server.isRandomPort()))
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_TIMEOUT, consumerConfig.getTimeout()))
            .append(getKeyPairs("id", consumerConfig.getId()))
            .append(getKeyPairs("crossLang", consumerConfig.getParameter("crossLang")))
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_GENERIC, consumerConfig.isGeneric()))
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_APP_NAME, consumerConfig.getAppName()))
            .append(getKeyPairs(RpcConstants.CONFIG_KEY_SERIALIZATION, consumerConfig.getSerialization()))
            .append(getKeyPairs(ProviderInfoAttrs.ATTR_START_TIME, RpcRuntimeContext.START_TIME));
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
    private static String getKeyPairs(String key, Object value) {
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
        sb.append(getKeyPairs("appPath", RpcRuntimeContext.get(RpcRuntimeContext.KEY_APPAPTH)));
        sb.append(getKeyPairs(RpcConstants.CONFIG_KEY_RPC_VERSION, Version.RPC_VERSION + ""));
        if (RpcRuntimeContext.get("reg.backfile") != null || RpcRuntimeContext.get("provider.backfile") != null) {
            sb.append(getKeyPairs("backfile", "false"));
        }
    }

    static String buildProviderPath(String rootPath, AbstractInterfaceConfig config) {
        return rootPath + "sofa-rpc/" + config.getInterfaceId() + "/providers";
    }

    static String buildConsumerPath(String rootPath, AbstractInterfaceConfig config) {
        return rootPath + "sofa-rpc/" + config.getInterfaceId() + "/consumers";
    }

    static String buildConfigPath(String rootPath, AbstractInterfaceConfig config) {
        return rootPath + "sofa-rpc/" + config.getInterfaceId() + "/configs";
    }

    static List<ProviderInfo> matchProviderInfos(ConsumerConfig consumerConfig, List<ProviderInfo> providerInfos) {
        String protocol = consumerConfig.getProtocol();
        List<ProviderInfo> result = new ArrayList<ProviderInfo>();
        for (ProviderInfo providerInfo : providerInfos) {
            if (providerInfo.getProtocolType().equalsIgnoreCase(protocol)
                && StringUtils.equals(consumerConfig.getUniqueId(),
                    providerInfo.getAttr(ProviderInfoAttrs.ATTR_UNIQUEID))) {
                result.add(providerInfo);
            }
        }
        return result;
    }

    static List<ConsulURL> matchConsulURLs(ConsumerConfig consumerConfig, List<ConsulURL> consulURLs) {
        List<ConsulURL> result = new ArrayList<ConsulURL>();
        String protocol = consumerConfig.getProtocol();
        for (ConsulURL consulUrl : consulURLs) {
            if (consulUrl.getProtocol().equalsIgnoreCase(protocol)
                && StringUtils.equals(consumerConfig.getUniqueId(),
                    consulUrl.getParameter(ProviderInfoAttrs.ATTR_UNIQUEID))) {
                result.add(consulUrl);
            }
        }
        return result;
    }

    static List<ProviderInfo> convertUrl2ProviderInfos(List<ConsulURL> consulUrls) {
        List<ProviderInfo> result = new ArrayList<ProviderInfo>();

        for (ConsulURL consulUrl : consulUrls) {
            ProviderInfo providerInfo = new ProviderInfo();

            ProviderHelper.toProviderInfo(consulUrl.toString());
            providerInfo.setPath(consulUrl.getPath());
            providerInfo.setPort(consulUrl.getPort());

            result.add(providerInfo);
        }

        return result;
    }

}
