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

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;
import com.alipay.sofa.rpc.registry.etcd.client.ClientBuilder;
import com.alipay.sofa.rpc.registry.etcd.client.EtcdClient;
import com.alipay.sofa.rpc.registry.etcd.grpc.api.KeyValue;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;

/**
 * etcd registry
 *
 * @author Fuwenming
 * @created 2018/7/8
 */
public class EtcdRegistry extends Registry {

    private final static Logger LOGGER = LoggerFactory.getLogger(EtcdRegistry.class);

    private static String CONFIG_USER = "user";
    private static String CONFIG_PASSWORD = "password";
    private static String ROOT_PATH = "ETCD/";

    private final EtcdClient client;

    /**
     * 保存服务发布者的url
     */
    private ConcurrentMap<ProviderConfig, List<String>> providers = new ConcurrentHashMap<ProviderConfig, List<String>>();

    private Cache<String, Map<String,List<String>>> subscribeCache;

    /**
     * 保存服务消费者的url
     */
    private ConcurrentMap<ConsumerConfig, String> consumers = new ConcurrentHashMap<ConsumerConfig, String>();

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected EtcdRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
        ClientBuilder clientBuilder = EtcdClient.builder().endpoints(registryConfig.getAddress());

        //TODO get user and password form configuration
        String user = registryConfig.getParameter(CONFIG_USER);
        String password = registryConfig.getParameter(CONFIG_PASSWORD);
        if(StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)){
            clientBuilder.auth(user, password);
        }
        this.client = clientBuilder.build();
        this.subscribeCache = CacheBuilder.newBuilder().maximumSize(1000).build();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void register(ProviderConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isRegister()) {
            //只订阅不注册
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }
        if (config.isRegister()) {
            //注册服务端节点
            try {
                List<String> urls = EtcdRegistryHelper.convertProviderToUrls(config);
                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = EtcdRegistryHelper.buildProviderPath(ROOT_PATH, config);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, providerPath));
                    }

                    for (String url : urls) {
                        String key = EtcdRegistryHelper.buildUniqueKey(ROOT_PATH, config);
                        client.putWithLease(key, url);

                        if (LOGGER.isInfoEnabled(appName)) {
                            LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB, url));
                        }
                    }
                    providers.put(config, urls);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName,
                                LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, providerPath));
                    }
                }
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to register provider to consulRegistry!", e);
            }

            if (EventBus.isEnable(ProviderPubEvent.class)) {
                ProviderPubEvent event = new ProviderPubEvent(config);
                EventBus.post(event);
            }
        }
    }

    @Override
    public void unRegister(ProviderConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isRegister()) {
            // 注册中心不注册
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }
        // 反注册服务端节点
        if (config.isRegister()) {
            try {
                List<String> urls = providers.remove(config);

                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = EtcdRegistryHelper.buildProviderPath(ROOT_PATH, config);
                    List<KeyValue> keyValues = client.getWithPrefix(providerPath);
                    for (KeyValue keyValue: keyValues){
                        client.revokeLease(keyValue.getLease());
                        removeLocalCachedUrl(keyValue.getValue().toStringUtf8());
                    }
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB, providerPath));
                    }
                }
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException("Failed to unregister provider to consulRegistry!", e);
                }
            }
        }
    }

    private void removeLocalCachedUrl(String url) {

    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        for (ProviderConfig providerConfig : configs) {
            unRegister(providerConfig);
        }
    }

    @Override
    public List<ProviderGroup> subscribe(ConsumerConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isSubscribe()) {
            // 注册中心不订阅
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return null;
        }

        // 注册Consumer节点
        if (config.isRegister()) {
            try {
                String url = EtcdRegistryHelper.convertConsumerToUrl(config);


                Iterator<Map.Entry<String, Map<String, List<String>>>> cacheIterator = subscribeCache.asMap().entrySet().iterator();
                Set<ProviderInfo> result = new HashSet<ProviderInfo>();
                List<String> matchUrls = new ArrayList<String>();
                // find all providerInfos
                while (cacheIterator.hasNext()) {
                    Map.Entry<String, Map<String, List<String>>> entry = cacheIterator.next();
                    Collection<List<String>> urls = entry.getValue().values();
                    List<ProviderInfo> matchProviders = new ArrayList<ProviderInfo>();
                    Iterator<List<String>> consulListIt = urls.iterator();
                    while (consulListIt.hasNext()) {
                        List<String> next = consulListIt.next();
                        matchUrls.addAll(next);
                        matchProviders.addAll(EtcdRegistryHelper.(next));
                    }
                    result.addAll(ConsulRegistryHelper.matchProviderInfos(config, matchProviders));
                }

                NotifyConsumerListner listener = new NotifyConsumerListner(consulURL, matchConsulUrls);

                consumerUrls.put(config, url);

                Pair<ConsulURL, Set<NotifyListener>> listenersPair =
                        notifyServiceListeners.get(consulURL.getServiceKey());

                if (listenersPair == null) {
                    Set<NotifyListener> listeners = Sets.newConcurrentHashSet();
                    listeners.add(listener);
                    listenersPair =
                            new ImmutablePair<ConsulURL, Set<NotifyListener>>(consulURL, listeners);
                } else {
                    listenersPair.getValue().add(listener);
                }

                if (notifyServiceListeners.get(consulURL.getServiceKey()) == null) {
                    notifyServiceListeners.put(consulURL.getServiceKey(), listenersPair);
                }
                if (!serviceGroupLookUped.contains(consulURL.getGroup())) {
                    serviceGroupLookUped.add(consulURL.getGroup());
                    ServiceLookUper serviceLookUper = new ServiceLookUper(consulURL.getGroup());
                    serviceLookUper.setDaemon(true);
                    serviceLookUper.start();
                    ConsulEphemeralNode ephemralNode = this.buildEphemralNode(consulURL, ThrallRoleType.CONSUMER);
                    consulManager.registerEphemralNode(ephemralNode);
                } else {
                    notifyListener(consulURL, listener);
                }

                if (EventBus.isEnable(ConsumerSubEvent.class)) {
                    ConsumerSubEvent event = new ConsumerSubEvent(config);
                    EventBus.post(event);
                }

                return Collections.singletonList(new ProviderGroup().addAll(result));
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to register consumer to consulRegistry!", e);
            }
        }
        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {

    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void init() {

    }
}
