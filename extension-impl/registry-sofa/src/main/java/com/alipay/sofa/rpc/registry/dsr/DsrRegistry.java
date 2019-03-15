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
package com.alipay.sofa.rpc.registry.dsr;

import com.alipay.sofa.registry.client.api.Configurator;
import com.alipay.sofa.registry.client.api.RegistryClient;
import com.alipay.sofa.registry.client.api.Subscriber;
import com.alipay.sofa.registry.client.api.model.RegistryType;
import com.alipay.sofa.registry.client.api.registration.ConfiguratorRegistration;
import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.api.registration.SubscriberRegistration;
import com.alipay.sofa.registry.core.model.ScopeEnum;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.common.DsrConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanggeng on 2017/7/3.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
@Extension("dsr")
public class DsrRegistry extends Registry {

    /**
     * Logger
     */
    private static final Logger               LOGGER        = LoggerFactory.getLogger(DsrRegistry.class);

    /**
     * 用于缓存所有数据订阅者，避免同一个dataId订阅两次
     */
    protected final Map<String, Subscriber>   subscribers   = new ConcurrentHashMap<String, Subscriber>();

    /**
     * 用于缓存所有配置订阅者，避免同一个dataId订阅两次
     */
    protected final Map<String, Configurator> configurators = new ConcurrentHashMap<String, Configurator>();

    protected RegistryClient                  registryClient;

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected DsrRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    @Override
    public void init() {
        //TODO 这里加一下 碧远
        registryClient = ConfregClient.getRegistryClient("", registryConfig);
    }

    @Override
    public void destroy() {
        subscribers.clear();
        configurators.clear();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void register(ProviderConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isRegister()) {
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }
        if (!config.isRegister()) {
            // 注册中心不注册或者服务不注册
            return;
        }
        List<ServerConfig> serverConfigs = config.getServer();
        if (CommonUtils.isNotEmpty(serverConfigs)) {
            for (ServerConfig server : serverConfigs) {
                String serviceName = DsrRegistryHelper.buildListDataId(config, server.getProtocol());
                String serviceData = DsrRegistryHelper.convertProviderToUrls(config, server);
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, serviceName));
                }
                String groupId = config.getParameter(DsrConstants.DSR_GROUP);
                groupId = groupId == null ? DsrRegistryHelper.SUBSCRIBER_LIST_GROUP_ID : groupId;
                doRegister(appName, serviceName, serviceData, groupId);

                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, serviceName));
                }
            }
        }
    }

    /**
     * 注册单条服务信息
     *
     * @param appName     应用
     * @param serviceName 服务关键字
     * @param serviceData 服务提供者数据
     * @param group       服务分组
     */
    protected void doRegister(String appName, String serviceName, String serviceData, String group) {
        PublisherRegistration dsrRegistration;
        // 生成注册对象，并添加额外属性
        dsrRegistration = new PublisherRegistration(serviceName);

        dsrRegistration.setGroup(group);
        addAttributes(dsrRegistration, group);

        // 去注册
        registryClient.register(dsrRegistration, serviceData);
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
        if (!config.isRegister()) {
            // 服务不注册
            return;
        }
        List<ServerConfig> serverConfigs = config.getServer();
        if (CommonUtils.isNotEmpty(serverConfigs)) {
            for (ServerConfig server : serverConfigs) {
                String serviceName = DsrRegistryHelper.buildListDataId(config, server.getProtocol());
                try {
                    String groupId = config.getParameter(DsrConstants.DSR_GROUP);
                    groupId = groupId == null ? DsrRegistryHelper.SUBSCRIBER_LIST_GROUP_ID : groupId;
                    doUnRegister(appName, serviceName, groupId);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB,
                            serviceName, "1"));
                    }
                } catch (Exception e) {
                    LOGGER.errorWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB,
                        serviceName, "0"), e);
                }
            }
        }
    }

    /**
     * 反注册服务信息
     *
     * @param appName     应用
     * @param serviceName 服务关键字
     * @param group       服务分组
     */
    protected void doUnRegister(String appName, String serviceName, String group) {

        registryClient.unregister(serviceName, group, RegistryType.PUBLISHER);
    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        // 不支持批量反注册，那就一个个来吧
        for (ProviderConfig config : configs) {
            String appName = config.getAppName();
            try {
                unRegister(config);
            } catch (Exception e) {
                LOGGER.errorWithApp(appName, "Error when batch unregistry", e);
            }
        }
    }

    @Override
    public List<ProviderGroup> subscribe(ConsumerConfig config) {
        ProviderInfoListener providerInfoListener = config.getProviderInfoListener();
        String appName = config.getAppName();
        if (!registryConfig.isSubscribe()) {
            // 注册中心不订阅
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return null;
        }
        if (!config.isSubscribe()) {
            // 服务不订阅
            return null;
        }

        String serviceName = DsrRegistryHelper.buildListDataId(config, config.getProtocol());

        DsrSubscribeCallback callback;

        Subscriber listSubscriber = subscribers.get(serviceName);
        Configurator attrSubscriber;
        if (listSubscriber != null && providerInfoListener != null) {
            // 已经有人订阅过这个Key，那么地址已经存在了，
            callback = (DsrSubscribeCallback) listSubscriber.getDataObserver();
            callback.addProviderInfoListener(serviceName, config, providerInfoListener);
            // 使用旧数据通知下
            callback.handleDataToListener(serviceName, config, providerInfoListener);
        } else {

            callback = new DsrSubscribeCallback();

            callback.addProviderInfoListener(serviceName, config, providerInfoListener);

            // 生成订阅对象，并添加额外属性
            SubscriberRegistration subscriberRegistration = new SubscriberRegistration(serviceName, callback);
            String groupId = config.getParameter(DsrConstants.DSR_GROUP);
            groupId = groupId == null ? DsrRegistryHelper.SUBSCRIBER_LIST_GROUP_ID : groupId;
            addAttributes(subscriberRegistration, groupId);

            ConfiguratorRegistration configRegistration = new ConfiguratorRegistration(serviceName, callback);
            addAttributes(configRegistration, DsrRegistryHelper.SUBSCRIBER_CONFIG_GROUP_ID);

            // 去配置中心订阅

            // 去注册
            listSubscriber = registryClient.register(subscriberRegistration);

            attrSubscriber = registryClient.register(configRegistration);

            // 放入缓存
            subscribers.put(serviceName, listSubscriber);
            configurators.put(serviceName, attrSubscriber);
        }
        // DSR统一走异步获取地址，所以此处返回null
        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        String serviceName = DsrRegistryHelper.buildListDataId(config, config.getProtocol());
        String appName = config.getAppName();
        Subscriber dsrSubscriber = subscribers.get(serviceName);
        if (dsrSubscriber != null) {
            DsrSubscribeCallback callback = (DsrSubscribeCallback) dsrSubscriber.getDataObserver();
            callback.remove(serviceName, config);
            if (callback.getListenerNum() == 0) {
                // 已经没人订阅这个data Key了
                registryClient.unregister(serviceName, dsrSubscriber.getGroup(),
                    RegistryType.SUBSCRIBER);
                subscribers.remove(serviceName);

                // 已经没人订阅这个config Key了
                registryClient.unregister(serviceName, dsrSubscriber.getGroup(),
                    RegistryType.CONFIGURATOR);
                configurators.remove(serviceName);
            }
        }
    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {
        // 不支持批量反注册，那就一个个来吧
        for (ConsumerConfig config : configs) {
            String appName = config.getAppName();
            try {
                unSubscribe(config);
            } catch (Exception e) {
                LOGGER.errorWithApp(appName, "Error when batch unSubscribe", e);
            }
        }
    }

    /**
     * 添加额外的属性
     *
     * @param dsrRegistration 注册或者订阅对象
     * @param group           分组
     */
    private void addAttributes(PublisherRegistration dsrRegistration, String group) {
        // if group == null; group = "DEFAULT_GROUP"
        if (StringUtils.isNotEmpty(group)) {
            dsrRegistration.setGroup(group);
        }

    }

    /**
     * 添加额外的属性
     *
     * @param dsrRegistration 注册或者订阅对象
     * @param group           分组
     */
    private void addAttributes(SubscriberRegistration dsrRegistration, String group) {

        // if group == null; group = "DEFAULT_GROUP"
        if (StringUtils.isNotEmpty(group)) {
            dsrRegistration.setGroup(group);
        }

        dsrRegistration.setScopeEnum(ScopeEnum.global);
    }

    /**
     * 添加额外的属性
     *
     * @param dsrRegistration 注册或者订阅对象
     * @param group           分组
     */
    private void addAttributes(ConfiguratorRegistration dsrRegistration, String group) {
        // if group == null; group = "DEFAULT_GROUP"
        if (StringUtils.isNotEmpty(group)) {
            dsrRegistration.setGroup(group);
        }

    }
}
