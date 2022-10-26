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
package com.alipay.sofa.rpc.registry.local;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.struct.ScheduledService;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhaowang
 * @version : DomainRegistry.java, v 0.1 2022年05月24日 5:15 下午 zhaowang
 */
@Extension("domain")
public class DomainRegistry extends Registry {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DomainRegistry.class);
    private static final Map<ProviderInfo, ProviderInfo> EMPTY_MAP = new HashMap<>(0);

    protected Map<String, List<ConsumerConfig>> notifyListeners = new ConcurrentHashMap<String, List<ConsumerConfig>>();

    protected Map<String, List<ProviderInfo>> domainCache = new ConcurrentHashMap<String, List<ProviderInfo>>();
    /**
     * 扫描周期，毫秒
     */
    protected int scanPeriod = 10000;

    /**
     * 定时加载
     */
    protected ScheduledService scheduledExecutorService;

    protected AtomicBoolean inited = new AtomicBoolean();

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected DomainRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    @Override
    public void init() {
        if (inited.compareAndSet(false, true)) {
            this.scanPeriod = CommonUtils.parseInt(registryConfig.getParameter("registry.domain.scan.period"),
                    scanPeriod);

            Runnable task = () -> {
                try {
                    refreshDomain();
                    notifyListener();
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            };

            scheduledExecutorService = new ScheduledService("DomainRegistry-Back-Load",
                    ScheduledService.MODE_FIXEDDELAY,
                    task, //定时load任务
                    scanPeriod, // 延迟一个周期
                    scanPeriod, // 一个周期循环
                    TimeUnit.MILLISECONDS
            ).start();
        }
    }

    protected void refreshDomain() {
        Set<String> keySet = new HashSet<>();
        for (Map.Entry<String, List<ConsumerConfig>> entry : notifyListeners.entrySet()) {
            String directUrl = entry.getKey();
            String[] providerStrs = StringUtils.splitWithCommaOrSemicolon(directUrl);
            keySet.addAll(Arrays.asList(providerStrs));
        }

        for (String directUrl : keySet) {
            ProviderInfo providerInfo = ProviderHelper.toProviderInfo(directUrl);
            List<ProviderInfo> result = directUrl2IpUrl(providerInfo, domainCache.get(directUrl));
            domainCache.put(directUrl, result);
        }
    }

    protected void notifyListener() {
        for (Map.Entry<String, List<ConsumerConfig>> entry : notifyListeners.entrySet()) {
            String directUrls = entry.getKey();
            String[] providerStrs = StringUtils.splitWithCommaOrSemicolon(directUrls);
            List<ProviderInfo> result = new ArrayList<>();
            for (String providerStr : providerStrs) {
                List<ProviderInfo> providerInfos = domainCache.get(providerStr);
                result.addAll(providerInfos);
            }
            List<ConsumerConfig> configs = entry.getValue();
            for (ConsumerConfig config : configs) {
                ProviderGroup providerGroup = new ProviderGroup(RpcConstants.ADDRESS_DIRECT_GROUP, result);
                ProviderInfoListener providerInfoListener = config.getProviderInfoListener();
                if (config.getProviderInfoListener() != null) {
                    providerInfoListener.updateProviders(providerGroup);
                }
            }
        }
    }

    private ProviderGroup getDirectGroup(String directUrl) {
        List<ProviderInfo> tmpProviderInfoList = new ArrayList<ProviderInfo>();
        String[] providerStrs = StringUtils.splitWithCommaOrSemicolon(directUrl);
        for (String providerStr : providerStrs) {
            List<ProviderInfo> providerInfos = resolveDomain(providerStr);
            tmpProviderInfoList.addAll(providerInfos);
        }
        return new ProviderGroup(RpcConstants.ADDRESS_DIRECT_GROUP, tmpProviderInfoList);
    }

    protected List<ProviderInfo> resolveDomain(String directUrl) {
        List<ProviderInfo> providerInfos = domainCache.get(directUrl);
        if (providerInfos != null) {
            return providerInfos;
        }
        ProviderInfo providerInfo = convertToProviderInfo(directUrl);
        List<ProviderInfo> result = directUrl2IpUrl(providerInfo, domainCache.get(directUrl));
        domainCache.put(directUrl, result);
        return result;
    }

    protected ProviderInfo convertToProviderInfo(String providerStr) {
        return ProviderHelper.toProviderInfo(providerStr);
    }

    protected List<ProviderInfo> directUrl2IpUrl(ProviderInfo providerInfo, List<ProviderInfo> originList) {
        List<ProviderInfo> result = new ArrayList<>();
        try {
            String originHost = providerInfo.getHost();
            String originUrl = providerInfo.getOriginUrl();
            InetAddress[] addresses = InetAddress.getAllByName(originHost);
            if (addresses != null && addresses.length > 0) {
                Map<ProviderInfo, ProviderInfo> originMap = originList == null ? EMPTY_MAP : originList.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
                String firstHost = addresses[0].getHostAddress();
                if (firstHost == null || firstHost.equals(providerInfo.getHost())) {
                    addProviderInfo(result, originMap, providerInfo);
                    return result;
                } else if (StringUtils.isNotBlank(originUrl)) {
                    for (InetAddress address : addresses) {
                        String newHost = address.getHostAddress();
                        ProviderInfo tmp = providerInfo.clone();
                        String newUrl = originUrl.replace(originHost, newHost);
                        tmp.setOriginUrl(newUrl);
                        tmp.setHost(newHost);
                        addProviderInfo(result, originMap, tmp);
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            LOGGER.error("directUrl2IpUrl error", e);
        }

        List<ProviderInfo> providerInfos = new ArrayList<>();
        providerInfos.add(providerInfo);
        return providerInfos;

    }

    protected void addProviderInfo(List<ProviderInfo> target, Map<ProviderInfo, ProviderInfo> originMap, ProviderInfo providerInfo) {
        ProviderInfo originProviderInfo = originMap.get(providerInfo);
        if (originProviderInfo != null) {
            target.add(originProviderInfo);
        } else {
            target.add(providerInfo);
        }
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public void register(ProviderConfig config) {
        throw new UnsupportedOperationException("DomainRegistry not support register providerConfig:" +
                config.getInterfaceId());
    }

    @Override
    public void unRegister(ProviderConfig config) {
        throw new UnsupportedOperationException("DomainRegistry not support unRegister providerConfig:" +
                config.getInterfaceId());
    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        // do nothing
    }

    @Override
    public List<ProviderGroup> subscribe(ConsumerConfig config) {
        String directUrl = config.getDirectUrl();
        if (StringUtils.isNotBlank(directUrl)) {
            List<ConsumerConfig> listeners = notifyListeners.get(directUrl);
            if (listeners == null) {
                notifyListeners.putIfAbsent(directUrl, new CopyOnWriteArrayList<>());
                listeners = notifyListeners.get(directUrl);
            }
            listeners.add(config);
            ProviderGroup directGroup = getDirectGroup(directUrl);
            ArrayList<ProviderGroup> providerGroups = new ArrayList<>();
            providerGroups.add(directGroup);
            return providerGroups;
        } else {
            return null;
        }
    }

    @Override
    public void unSubscribe(ConsumerConfig consumerConfig) {
        String directUrl = consumerConfig.getDirectUrl();
        notifyListeners.get(directUrl).remove(consumerConfig);
    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {
        for (ConsumerConfig config : configs) {
            unSubscribe(config);
        }
    }

    @Override
    public void destroy() {
        notifyListeners.clear();
        domainCache.clear();
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

}