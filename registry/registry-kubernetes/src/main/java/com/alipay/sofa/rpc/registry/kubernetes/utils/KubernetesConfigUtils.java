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
package com.alipay.sofa.rpc.registry.kubernetes.utils;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.RegistryConfig;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;

import java.util.Base64;

import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.API_VERSION;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.CA_CERT_DATA;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.CA_CERT_FILE;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.CLIENT_CERT_DATA;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.CLIENT_CERT_FILE;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.CLIENT_KEY_ALGO;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.CLIENT_KEY_DATA;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.CLIENT_KEY_FILE;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.CLIENT_KEY_PASSPHRASE;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.CONNECTION_TIMEOUT;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.DEFAULT_MASTER_URL;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.HTTP2_DISABLE;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.HTTPS_PROXY;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.HTTP_PROXY;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.LOGGING_INTERVAL;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.NAMESPACE;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.OAUTH_TOKEN;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.PASSWORD;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.PROXY_PASSWORD;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.PROXY_USERNAME;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.REQUEST_TIMEOUT;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.TRUST_CERTS;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.USERNAME;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.USE_HTTPS;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.WATCH_RECONNECT_INTERVAL;
import static com.alipay.sofa.rpc.registry.kubernetes.constant.KubernetesClientConstants.WATCH_RECONNECT_LIMIT;

public class KubernetesConfigUtils {

    public static Config buildKubernetesConfig(RegistryConfig registryConfig) {

        // Init default config
        Config base = Config.autoConfigure(null);

        return new ConfigBuilder(base)
            .withMasterUrl(buildMasterUrl(registryConfig))
            .withApiVersion(registryConfig.getParameter(API_VERSION, base.getApiVersion()))
            .withNamespace(registryConfig.getParameter(NAMESPACE, base.getNamespace()))
            .withUsername(registryConfig.getParameter(USERNAME, base.getUsername()))
            .withPassword(registryConfig.getParameter(PASSWORD, base.getPassword()))
            .withOauthToken(registryConfig.getParameter(OAUTH_TOKEN, base.getOauthToken()))
            .withCaCertFile(registryConfig.getParameter(CA_CERT_FILE, base.getCaCertFile()))
            .withCaCertData(registryConfig.getParameter(CA_CERT_DATA, decodeBase64(base.getCaCertData())))
            .withClientKeyFile(registryConfig.getParameter(CLIENT_KEY_FILE, base.getClientKeyFile()))
            .withClientKeyData(registryConfig.getParameter(CLIENT_KEY_DATA, decodeBase64(base.getClientKeyData())))
            .withClientCertFile(registryConfig.getParameter(CLIENT_CERT_FILE, base.getClientCertFile()))
            .withClientCertData(registryConfig.getParameter(CLIENT_CERT_DATA, decodeBase64(base.getClientCertData())))
            .withClientKeyAlgo(registryConfig.getParameter(CLIENT_KEY_ALGO, base.getClientKeyAlgo()))
            .withClientKeyPassphrase(registryConfig.getParameter(CLIENT_KEY_PASSPHRASE, base.getClientKeyPassphrase()))
            .withConnectionTimeout(registryConfig.getParameter(CONNECTION_TIMEOUT, base.getConnectionTimeout()))
            .withRequestTimeout(registryConfig.getParameter(REQUEST_TIMEOUT, base.getRequestTimeout()))
            .withWatchReconnectInterval(
                registryConfig.getParameter(WATCH_RECONNECT_INTERVAL, base.getWatchReconnectInterval()))
            .withWatchReconnectLimit(registryConfig.getParameter(WATCH_RECONNECT_LIMIT, base.getWatchReconnectLimit()))
            .withLoggingInterval(registryConfig.getParameter(LOGGING_INTERVAL, base.getLoggingInterval()))
            .withTrustCerts(registryConfig.getParameter(TRUST_CERTS, base.isTrustCerts()))
            .withHttp2Disable(registryConfig.getParameter(HTTP2_DISABLE, base.isHttp2Disable()))
            .withHttpProxy(registryConfig.getParameter(HTTP_PROXY, base.getHttpProxy()))
            .withHttpsProxy(registryConfig.getParameter(HTTPS_PROXY, base.getHttpsProxy()))
            .withProxyUsername(registryConfig.getParameter(PROXY_USERNAME, base.getProxyUsername()))
            .withProxyPassword(registryConfig.getParameter(PROXY_PASSWORD, base.getProxyPassword()))
            .build();
    }

    private static String buildMasterUrl(RegistryConfig registryConfig) {
        String address = registryConfig.getAddress();
        if (StringUtils.isBlank(address)) {
            return DEFAULT_MASTER_URL;
        }
        if (address.startsWith("http")) {
            return address;
        }
        return registryConfig.getParameter(USE_HTTPS, true) ? "https://" + address : "http://" + address;
    }

    private static String decodeBase64(String str) {
        return StringUtils.isNotEmpty(str) ? new String(Base64.getDecoder().decode(str)) : null;
    }
}
