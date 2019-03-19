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
package com.alipay.sofa.rpc.registry.consul;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
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
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;
import com.alipay.sofa.rpc.registry.consul.common.ConsulConstants;
import com.alipay.sofa.rpc.registry.consul.common.ConsulURL;
import com.alipay.sofa.rpc.registry.consul.common.ConsulURLUtils;
import com.alipay.sofa.rpc.registry.consul.internal.ConsulManager;
import com.alipay.sofa.rpc.registry.consul.model.ConsulEphemeralNode;
import com.alipay.sofa.rpc.registry.consul.model.ConsulService;
import com.alipay.sofa.rpc.registry.consul.model.ConsulServiceResp;
import com.alipay.sofa.rpc.registry.consul.model.NotifyConsumerListener;
import com.alipay.sofa.rpc.registry.consul.model.NotifyListener;
import com.alipay.sofa.rpc.registry.consul.model.ThrallRoleType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;

/**
 * CONSUL 注册中心
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 * @since 5.5.0
 */
@Extension("consul")
public class ConsulRegistry extends Registry {

    /**
     * Logger
     */
    private final static Logger                                               LOGGER                 = LoggerFactory
                                                                                                         .getLogger(ConsulRegistry.class);

    private ConsulManager                                                     consulManager;

    /**
     * Root path of registry data
     */
    private String                                                            rootPath;

    /**
     * 保存服务发布者的url
     */
    private ConcurrentMap<ProviderConfig, List<String>>                       providerUrls           = new ConcurrentHashMap<ProviderConfig, List<String>>();

    /**
     * 保存服务消费者的url
     */
    private ConcurrentMap<ConsumerConfig, String>                             consumerUrls           = new ConcurrentHashMap<ConsumerConfig, String>();

    private Cache<String, Map<String, List<ConsulURL>>>                       serviceCache;

    private final ConcurrentMap<String, Long>                                 lookupGroupServices    = Maps
                                                                                                         .newConcurrentMap();

    private final ConcurrentMap<String, Pair<ConsulURL, Set<NotifyListener>>> notifyServiceListeners = Maps
                                                                                                         .newConcurrentMap();

    private final Set<String>                                                 serviceGroupLookUped   = Sets
                                                                                                         .newConcurrentHashSet();

    private ExecutorService                                                   notifyExecutor;

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected ConsulRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    public String[] validateIp(RegistryConfig registryConfig) {
        String addressInput = registryConfig.getAddress(); // xxx:2181,yyy:2181/path1/paht2

        if (StringUtils.isEmpty(addressInput)) {
            throw new SofaRpcRuntimeException("Address of consul registry is empty.");
        }

        int idx = addressInput.indexOf(CONTEXT_SEP);
        String address; // IP地址
        if (idx > 0) {
            address = addressInput.substring(0, idx);
            rootPath = addressInput.substring(idx);

        } else {
            address = addressInput;
            rootPath = "/";
        }

        if (!ConsulURLUtils.isValidAddress(address)) {
            throw new SofaRpcRuntimeException("Address format of consul registry is wrong.");
        }
        if (!rootPath.endsWith(CONTEXT_SEP)) {
            rootPath += CONTEXT_SEP; // 保证以"/"结尾
        }
        String[] ipAndHost = StringUtils.split(address, ":");
        return ipAndHost;
    }

    private ConsulService buildConsulHealthService(ConsulURL url) {
        return ConsulService.newService()//
            .withAddress(url.getHost())//
            .withPort(Integer.toString(url.getPort()))//
            .withName(ConsulURLUtils.toServiceName(url.getGroup()))//
            .withTag(ConsulURLUtils.healthServicePath(url, ThrallRoleType.PROVIDER))//
            .withId(url.getHost() + ":" + url.getPort() + "-" + url.getPath() + "-" + url.getVersion())//
            .withCheckInterval(Integer.toString(ConsulConstants.TTL)).build();
    }

    private ConsulEphemeralNode buildEphemralNode(ConsulURL url, ThrallRoleType roleType) {
        return ConsulEphemeralNode.newEphemralNode().withUrl(url)//
            .withEphemralType(roleType)//
            .withCheckInterval(Integer.toString(ConsulConstants.TTL * 6))//
            .build();
    }

    @Override
    public void init() {

        if (consulManager != null) {
            return;
        }

        String[] address = validateIp(registryConfig);
        consulManager = new ConsulManager(address[0], Integer.parseInt(address[1]));
        serviceCache = CacheBuilder.newBuilder().maximumSize(1000).build();
        notifyExecutor = Executors.newCachedThreadPool(
            new NamedThreadFactory("NotifyConsumerListener", true));
    }

    @Override
    public void destroy() {
        providerUrls.clear();
        consumerUrls.clear();
    }

    @Override
    public void destroy(DestroyHook hook) {
        hook.postDestroy();
        destroy();
        hook.postDestroy();
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
                List<String> urls = ConsulRegistryHelper.convertProviderToUrls(config);
                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = ConsulRegistryHelper.buildProviderPath(rootPath, config);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName,
                            LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, providerPath));
                    }
                    for (String url : urls) {
                        //                        url = URLEncoder.encode(url, "UTF-8");
                        String providerUrl = providerPath + CONTEXT_SEP + url;

                        ConsulURL providerConfigUrl = ConsulURL.valueOf(url);
                        ConsulService service = this.buildConsulHealthService(providerConfigUrl);
                        consulManager.registerService(service);
                        ConsulEphemeralNode ephemralNode = this.buildEphemralNode(providerConfigUrl,
                            ThrallRoleType.PROVIDER);
                        consulManager.registerEphemralNode(ephemralNode);
                        if (LOGGER.isInfoEnabled(appName)) {
                            LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB, providerUrl));
                        }
                    }
                    providerUrls.put(config, urls);
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
                List<String> urls = providerUrls.remove(config);

                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = ConsulRegistryHelper.buildProviderPath(rootPath, config);

                    for (String url : urls) {
                        ConsulURL providerConfigUrl = ConsulURL.valueOf(url);
                        ConsulService service = this.buildConsulHealthService(providerConfigUrl);
                        consulManager.unregisterService(service);
                    }
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB,
                            providerPath, "1"));
                    }
                }
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException("Failed to unregister provider to consulRegistry!", e);
                }
            }
        }

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

                String url = ConsulRegistryHelper.convertConsumerToUrl(config);
                ConsulURL consulURL = ConsulURL.valueOf(url);

                Iterator<Map.Entry<String, Map<String, List<ConsulURL>>>> it = serviceCache.asMap().entrySet()
                    .iterator();

                Set<ProviderInfo> result = new HashSet<ProviderInfo>();

                List<ConsulURL> matchConsulUrls = new ArrayList<ConsulURL>();
                // find all providerInfos
                while (it.hasNext()) {
                    Map.Entry<String, Map<String, List<ConsulURL>>> entry = it.next();
                    Collection<List<ConsulURL>> consulURLList = entry.getValue().values();

                    List<ProviderInfo> matchProviders = new ArrayList<ProviderInfo>();
                    for (List<ConsulURL> next : consulURLList) {
                        matchConsulUrls.addAll(next);
                        matchProviders.addAll(ConsulRegistryHelper.convertUrl2ProviderInfos(next));
                    }
                    result.addAll(ConsulRegistryHelper.matchProviderInfos(config, matchProviders));
                }

                NotifyConsumerListener listener = new NotifyConsumerListener(consulURL, matchConsulUrls);

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

        String appName = config.getAppName();
        if (!registryConfig.isSubscribe()) {
            // 注册中心不订阅
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
        }

        // 注册Consumer节点
        if (config.isRegister()) {
            // 向服务器端发送取消订阅请求
            String url = ConsulRegistryHelper.convertConsumerToUrl(config);
            ConsulURL consulURL = ConsulURL.valueOf(url);
            consumerUrls.remove(config);
            notifyServiceListeners.remove(consulURL.getServiceKey());

        }

    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {

        for (ConsumerConfig consumerConfig : configs) {
            unSubscribe(consumerConfig);
        }
    }

    private void notifyListener(ConsulURL url, NotifyListener listener) {
        Map<String, List<ConsulURL>> groupCacheUrls = serviceCache.getIfPresent(url.getGroup());
        if (groupCacheUrls != null) {
            for (Map.Entry<String, List<ConsulURL>> entry : groupCacheUrls.entrySet()) {
                String cacheServiceKey = entry.getKey();
                if (url.getServiceKey().equals(cacheServiceKey)) {
                    List<ConsulURL> newUrls = entry.getValue();
                    ConsulRegistry.this.notify(url, listener, newUrls);
                }
            }
        }
    }

    protected void notify(final ConsulURL url, final NotifyListener listener,
                          final List<ConsulURL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        try {

            notifyExecutor.submit(new Runnable() {

                @Override
                public void run() {
                    listener.notify(url, urls);
                }
            });
        } catch (Exception t) {
            // 将失败的通知请求记录到失败列表，定时重试
            LOGGER.error(
                "Failed to notify for subscribe " + url + ", waiting for retry, cause: " + t.getMessage(),
                t);
        }
    }

    private Map<String, List<ConsulURL>> lookupServiceUpdate(String group) {
        Long lastConsulIndexId =
                lookupGroupServices.get(group) == null ? Long.valueOf(0L) : lookupGroupServices.get(group);
        String serviceName = ConsulURLUtils.toServiceName(group);
        ConsulServiceResp consulResp = consulManager.lookupHealthService(serviceName, lastConsulIndexId);
        if (consulResp != null) {
            List<ConsulService> consulServcies = consulResp.getConsulServices();
            boolean updated = consulServcies != null && !consulServcies.isEmpty()
                && consulResp.getConsulIndex() > lastConsulIndexId;
            if (updated) {
                Map<String, List<ConsulURL>> groupProviderUrls = Maps.newConcurrentMap();
                for (ConsulService service : consulServcies) {
                    ConsulURL providerUrl = buildURL(service);
                    String serviceKey = providerUrl.getServiceKey();
                    List<ConsulURL> urlList = groupProviderUrls.get(serviceKey);
                    if (urlList == null) {
                        urlList = Lists.newArrayList();
                        groupProviderUrls.put(serviceKey, urlList);
                    }
                    urlList.add(providerUrl);
                }
                lookupGroupServices.put(group, consulResp.getConsulIndex());
                return groupProviderUrls;
            }
        }
        return null;
    }

    private ConsulURL buildURL(ConsulService service) {
        try {
            for (String tag : service.getTags()) {
                if (org.apache.commons.lang3.StringUtils.indexOf(tag, ConsulConstants.PROVIDERS_CATEGORY) != -1) {
                    String toUrlPath = org.apache.commons.lang3.StringUtils.substringAfter(tag,
                        ConsulConstants.PROVIDERS_CATEGORY);
                    ConsulURL consulUrl = ConsulURL.valueOf(ConsulURL.decode(toUrlPath));
                    return consulUrl;
                }
            }
        } catch (Exception e) {
            LOGGER.error("convert consul service to url fail! service:" + service, e);
        }
        return null;
    }

    private class ServiceLookUper extends Thread {

        private final String group;

        public ServiceLookUper(String group) {
            this.group = group;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // 最新拉取的值
                    Map<String, List<ConsulURL>> groupNewUrls = lookupServiceUpdate(group);
                    if (groupNewUrls != null && !groupNewUrls.isEmpty()) {
                        // 缓存中的值
                        Map<String, List<ConsulURL>> groupCacheUrls = serviceCache.getIfPresent(group);
                        if (groupCacheUrls == null) {
                            groupCacheUrls = Maps.newConcurrentMap();
                            serviceCache.put(group, groupCacheUrls);
                        }
                        for (Map.Entry<String, List<ConsulURL>> entry : groupNewUrls.entrySet()) {
                            List<ConsulURL> oldUrls = groupCacheUrls.get(entry.getKey());
                            List<ConsulURL> newUrls = entry.getValue();
                            boolean isSame = CommonUtils.listEquals(newUrls, oldUrls);
                            if (!isSame) {
                                groupCacheUrls.put(entry.getKey(), newUrls);
                                Pair<ConsulURL, Set<NotifyListener>> listenerPair =
                                        notifyServiceListeners.get(entry.getKey());
                                if (listenerPair != null) {
                                    ConsulURL subscribeUrl = listenerPair.getKey();
                                    Set<NotifyListener> listeners = listenerPair.getValue();
                                    for (NotifyListener listener : listeners) {
                                        ConsulRegistry.this.notify(subscribeUrl, listener, newUrls);
                                    }
                                }
                            }
                        }
                    }
                    sleep(ConsulConstants.DEFAULT_LOOKUP_INTERVAL);
                } catch (Throwable e) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}
