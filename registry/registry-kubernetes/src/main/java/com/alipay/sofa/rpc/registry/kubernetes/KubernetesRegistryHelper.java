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
package com.alipay.sofa.rpc.registry.kubernetes;

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KubernetesRegistryHelper extends RegistryUtils {

    private final static Logger LOGGER = LoggerFactory.getLogger(KubernetesRegistryHelper.class);

    public static List<ProviderInfo> convertPodsToProviders(List<Pod> pods, ConsumerConfig config) {
        List<ProviderInfo> providerInfos = new ArrayList<>();
        if (CommonUtils.isEmpty(pods) || null == config) {
            return providerInfos;
        }

        for (Pod pod : pods) {
            ProviderInfo providerInfo = getProviderInfo(pod, config);
            if (null == providerInfo) {
                continue;
            }
            providerInfos.add(providerInfo);
        }

        return providerInfos;
    }

    public static String convertToUrl(Pod pod, ServerConfig serverConfig, ProviderConfig providerConfig) {
        String uri = "";
        String protocol = serverConfig.getProtocol();
        if (StringUtils.isNotEmpty(protocol)) {
            uri = protocol + "://";
        }
        uri += pod.getStatus().getPodIP() + ":" + serverConfig.getPort();

        Map<String, String> metaData = RegistryUtils.convertProviderToMap(providerConfig, serverConfig);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : metaData.entrySet()) {
            sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }
        if (sb.length() > 0) {
            uri += sb.replace(0, 1, "?").toString();
        }
        return uri;
    }

    private static ProviderInfo getProviderInfo(Pod pod, ConsumerConfig config) {
        try {
            String dataId = buildDataId(config, config.getProtocol());
            String providerUrlString = pod.getMetadata().getAnnotations().get(dataId);

            if (StringUtils.isBlank(providerUrlString)) {
                return null;
            }
            return ProviderHelper.toProviderInfo(providerUrlString);
        } catch (Exception e) {
            LOGGER.info("get provider config error with pod");
            return null;
        }
    }

    public static String buildDataId(AbstractInterfaceConfig config, String protocol) {
        if (RpcConstants.PROTOCOL_TYPE_BOLT.equals(protocol) || RpcConstants.PROTOCOL_TYPE_TR.equals(protocol)) {
            return ConfigUniqueNameGenerator.getUniqueName(config) + "@DEFAULT";
        } else {
            return ConfigUniqueNameGenerator.getUniqueName(config) + "@" + protocol;
        }
    }
}
