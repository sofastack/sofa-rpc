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
package com.alipay.sofa.registry;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.Version;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.MethodConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.alipay.sofa.rpc.client.ProviderInfoAttrs.ATTR_APP_NAME;
import static com.alipay.sofa.rpc.client.ProviderInfoAttrs.ATTR_RPC_VERSION;
import static com.alipay.sofa.rpc.client.ProviderInfoAttrs.ATTR_SERIALIZATION;
import static com.alipay.sofa.rpc.client.ProviderInfoAttrs.ATTR_START_TIME;
import static com.alipay.sofa.rpc.client.ProviderInfoAttrs.ATTR_TIMEOUT;
import static com.alipay.sofa.rpc.client.ProviderInfoAttrs.ATTR_WARMUP_TIME;
import static com.alipay.sofa.rpc.client.ProviderInfoAttrs.ATTR_WARMUP_WEIGHT;
import static com.alipay.sofa.rpc.client.ProviderInfoAttrs.ATTR_WEIGHT;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("mocktest")
public class MockTestRegistry extends Registry {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockTestRegistry.class);

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected MockTestRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    /**
     * 订阅者通知列表（key为订阅者关键字，value为ConsumerConfig列表）
     */
    protected ConcurrentHashMap<String, Map<ConsumerConfig, ProviderInfoListener>> notifyListeners = new ConcurrentHashMap<String, Map<ConsumerConfig, ProviderInfoListener>>();

    /**
     * 内存里的服务列表 {service : [provider...]}
     */
    protected ConcurrentHashMap<String, ProviderGroup>                             memoryCache     = new ConcurrentHashMap<String, ProviderGroup>();

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void register(ProviderConfig config) {
        String key = buildKey(config);
        ProviderGroup group = memoryCache.get(key);
        if (group == null) {
            group = buildProviderGroup();
            ProviderGroup old = memoryCache.putIfAbsent(key, group);
            if (old != null) {
                group = old;
            }
        }
        List<ServerConfig> serverConfigs = config.getServer();
        if (CommonUtils.isNotEmpty(serverConfigs)) {
            for (ServerConfig server : serverConfigs) {
                group.add(ProviderHelper.toProviderInfo(convertProviderToUrls(config, server)));
            }
        }
        Map<ConsumerConfig, ProviderInfoListener> listeners = notifyListeners.get(key);
        if (listeners != null) {
            for (ProviderInfoListener listener : listeners.values()) {
                listener.updateProviders(group);
            }
        }
    }

    @Override
    public void unRegister(ProviderConfig config) {
        String key = buildKey(config);
        ProviderGroup group = memoryCache.get(key);
        if (group != null) {
            List<ServerConfig> serverConfigs = config.getServer();
            if (CommonUtils.isNotEmpty(serverConfigs)) {
                for (ServerConfig server : serverConfigs) {
                    group.remove(ProviderHelper.toProviderInfo(convertProviderToUrls(config, server)));
                }
            }
        }
        Map<ConsumerConfig, ProviderInfoListener> listeners = notifyListeners.get(key);
        if (listeners != null) {
            for (ProviderInfoListener listener : listeners.values()) {
                listener.updateProviders(group);
            }
        }
    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        for (ProviderConfig config : configs) {
            unRegister(config);
        }
    }

    @Override
    public List<ProviderGroup> subscribe(ConsumerConfig config) {
        String key = buildKey(config);
        Map<ConsumerConfig, ProviderInfoListener> listeners = notifyListeners.get(key);
        if (listeners == null) {
            listeners = new ConcurrentHashMap<ConsumerConfig, ProviderInfoListener>();
            Map<ConsumerConfig, ProviderInfoListener> old = notifyListeners.putIfAbsent(key, listeners);
            if (old != null) {
                listeners = old;
            }
        }
        final ProviderInfoListener listener = config.getProviderInfoListener();
        listeners.put(config, listener);

        ProviderGroup group = memoryCache.get(key);
        List<ProviderGroup> groups = new ArrayList<ProviderGroup>();
        if (group != null) {
            groups.add(group);
        }
        return doReturn(listener, groups);
    }

    protected List<ProviderGroup> doReturn(ProviderInfoListener listener, List<ProviderGroup> groups) {
        return groups;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        String key = buildKey(config);
        Map<ConsumerConfig, ProviderInfoListener> listeners = notifyListeners.get(key);
        if (listeners != null) {
            listeners.remove(config);
        }
    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {
        for (ConsumerConfig config : configs) {
            unSubscribe(config);
        }
    }

    @Override
    public void destroy() {
        memoryCache.clear();
        notifyListeners.clear();
    }

    @Override
    public void init() {

    }

    protected String buildKey(AbstractInterfaceConfig config) {
        return config.getInterfaceId() + ":" + config.getVersion();
    }

    protected ProviderGroup buildProviderGroup() {
        return new ProviderGroup("mocktest");
    }

    /**
     * Convert provider to url.
     *
     * @param providerConfig the ProviderConfig
     * @return the url list
     */
    public static String convertProviderToUrls(ProviderConfig providerConfig, ServerConfig server) {
        StringBuilder sb = new StringBuilder(200);
        String appName = providerConfig.getAppName();
        String host = server.getVirtualHost(); // 虚拟ip
        if (host == null) {
            host = server.getHost();
            if (NetUtils.isLocalHost(host) || NetUtils.isAnyHost(host)) {
                host = SystemInfo.getLocalHost();
            }
        } else {
            if (LOGGER.isWarnEnabled(appName)) {
                LOGGER.warnWithApp(appName,
                    "Virtual host is specified, host will be change from {} to {} when register",
                    server.getHost(), host);
            }
        }
        Integer port = server.getVirtualPort(); // 虚拟port
        if (port == null) {
            port = server.getPort();
        } else {
            if (LOGGER.isWarnEnabled(appName)) {
                LOGGER.warnWithApp(appName,
                    "Virtual port is specified, host will be change from {} to {} when register",
                    server.getPort(), port);
            }
        }

        String protocol = server.getProtocol();
        sb.append(host).append(":").append(port).append(server.getContextPath());
        sb.append("?").append(ATTR_RPC_VERSION).append("=").append(Version.RPC_VERSION);
        sb.append(getKeyPairs(ATTR_SERIALIZATION, providerConfig.getSerialization()));
        sb.append(getKeyPairs(ATTR_WEIGHT, providerConfig.getWeight()));
        if (providerConfig.getTimeout() > 0) {
            sb.append(getKeyPairs(ATTR_TIMEOUT, providerConfig.getTimeout()));
        }
        sb.append(getKeyPairs(ATTR_APP_NAME, appName));
        sb.append(getKeyPairs(ATTR_WARMUP_TIME, providerConfig.getParameter(ATTR_WARMUP_TIME.toString())));
        sb.append(getKeyPairs(ATTR_WARMUP_WEIGHT, providerConfig.getParameter(ATTR_WARMUP_WEIGHT.toString())));

        Map<String, MethodConfig> methodConfigs = providerConfig.getMethods();
        if (CommonUtils.isNotEmpty(methodConfigs)) {
            for (Map.Entry<String, MethodConfig> entry : methodConfigs.entrySet()) {
                String methodName = entry.getKey();
                MethodConfig methodConfig = entry.getValue();
                sb.append(getKeyPairs("." + methodName + "." + ATTR_TIMEOUT, methodConfig.getTimeout()));

                // 方法级配置，只能放timeout 
                String key = "[" + methodName + "]";
                String value = "[clientTimeout" + "#" + methodConfig.getTimeout() + "]";
                sb.append(getKeyPairs(key, value));
            }
        }
        sb.append(getKeyPairs(ATTR_START_TIME, RpcRuntimeContext.now()));
        return sb.toString();
    }

    /**
     * Gets key pairs.
     *
     * @param key   the key
     * @param value the value
     * @return the key pairs
     */
    private static String getKeyPairs(CharSequence key, Object value) {
        if (value != null) {
            return "&" + key + "=" + value.toString();
        } else {
            return StringUtils.EMPTY;
        }
    }
}
