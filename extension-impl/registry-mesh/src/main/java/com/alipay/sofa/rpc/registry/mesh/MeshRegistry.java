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
package com.alipay.sofa.rpc.registry.mesh;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;
import com.alipay.sofa.rpc.registry.mesh.client.MeshApiClient;
import com.alipay.sofa.rpc.registry.mesh.model.ApplicationInfoRequest;
import com.alipay.sofa.rpc.registry.mesh.model.MeshConstants;
import com.alipay.sofa.rpc.registry.mesh.model.ProviderMetaInfo;
import com.alipay.sofa.rpc.registry.mesh.model.PublishServiceRequest;
import com.alipay.sofa.rpc.registry.mesh.model.SubscribeServiceRequest;
import com.alipay.sofa.rpc.registry.mesh.model.SubscribeServiceResult;
import com.alipay.sofa.rpc.registry.mesh.model.UnPublishServiceRequest;
import com.alipay.sofa.rpc.registry.mesh.model.UnSubscribeServiceRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * mesh registry
 *
 * @author <a href="mailto:zhiyuan.lzy@antfin.com">zhiyuan.lzy</a>
 */
@Extension("mesh")
public class MeshRegistry extends Registry {

    /**
     * Logger
     */
    private static final Logger LOGGER  = LoggerFactory.getLogger(MeshRegistry.class);

    private static final String VERSION = "4.0";

    private MeshApiClient       client;

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected MeshRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    //init only once
    private boolean inited;

    //has registed app info
    private boolean registedApp;

    @Override
    public void init() {
        synchronized (MeshRegistry.class) {
            if (!inited) {
                String address = registryConfig.getAddress();
                client = new MeshApiClient(address);
                inited = true;
            }
        }
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
        if (!config.isRegister()) { // 注册中心不注册或者服务不注册
            return;
        }
        List<ServerConfig> serverConfigs = config.getServer();
        if (CommonUtils.isNotEmpty(serverConfigs)) {
            for (ServerConfig server : serverConfigs) {
                String serviceName = MeshRegistryHelper.buildMeshKey(config, server.getProtocol());
                ProviderInfo providerInfo = MeshRegistryHelper.convertProviderToProviderInfo(config, server);
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, serviceName));
                }
                doRegister(appName, serviceName, providerInfo);

                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, serviceName));
                }
            }
            if (EventBus.isEnable(ProviderPubEvent.class)) {
                ProviderPubEvent event = new ProviderPubEvent(config);
                EventBus.post(event);
            }

        }
    }

    /**
     * 注册单条服务信息
     *
     * @param appName      应用名
     * @param serviceName  服务关键字
     * @param providerInfo 服务提供者数据
     */
    protected void doRegister(String appName, String serviceName, ProviderInfo providerInfo) {

        registerAppInfoOnce(appName);

        if (LOGGER.isInfoEnabled(appName)) {
            LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB, serviceName));
        }

        PublishServiceRequest publishServiceRequest = new PublishServiceRequest();
        publishServiceRequest.setServiceName(serviceName);
        ProviderMetaInfo providerMetaInfo = new ProviderMetaInfo();
        providerMetaInfo.setProtocol(providerInfo.getProtocolType());
        providerMetaInfo.setSerializeType(providerInfo.getSerializationType());
        providerMetaInfo.setAppName(appName);
        providerMetaInfo.setVersion(VERSION);
        publishServiceRequest.setProviderMetaInfo(providerMetaInfo);

        client.publishService(publishServiceRequest);
    }

    @Override
    public void unRegister(ProviderConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isRegister()) { // 注册中心不注册
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }
        if (!config.isRegister()) { // 服务不注册
            return;
        }
        List<ServerConfig> serverConfigs = config.getServer();
        if (CommonUtils.isNotEmpty(serverConfigs)) {
            for (ServerConfig server : serverConfigs) {
                String serviceName = MeshRegistryHelper.buildMeshKey(config, server.getProtocol());
                ProviderInfo providerInfo = MeshRegistryHelper.convertProviderToProviderInfo(config, server);
                try {
                    doUnRegister(serviceName, providerInfo);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName,
                            LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB, serviceName, "1"));
                    }
                } catch (Exception e) {
                    LOGGER.errorWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB, serviceName, "0"),
                        e);
                }
            }
        }
    }

    /**
     * 反注册服务信息
     *
     * @param serviceName  服务关键字
     * @param providerInfo 服务提供者数据
     */
    protected void doUnRegister(String serviceName, ProviderInfo providerInfo) {

        UnPublishServiceRequest unPublishServiceRequest = new UnPublishServiceRequest();
        unPublishServiceRequest.setServiceName(serviceName);
        client.unPublishService(unPublishServiceRequest);

    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
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
        final String appName = config.getAppName();

        registerAppInfoOnce(appName);

        String key = MeshRegistryHelper.buildMeshKey(config, config.getProtocol());
        SubscribeServiceRequest subscribeRequest = new SubscribeServiceRequest();
        subscribeRequest.setServiceName(key);
        SubscribeServiceResult subscribeServiceResult = client.subscribeService(subscribeRequest);

        if (subscribeServiceResult == null || !subscribeServiceResult.isSuccess()) {
            throw new RuntimeException("regist consumer occors error," + subscribeRequest);

        }

        List<ProviderGroup> providerGroups = new ArrayList<ProviderGroup>();

        ProviderGroup providerGroup = new ProviderGroup();

        List<ProviderInfo> providerInfos = new ArrayList<ProviderInfo>();

        String url = fillProtocolAndVersion(subscribeServiceResult, client.getHost(), "");

        ProviderInfo providerInfo = ProviderHelper.toProviderInfo(url);
        providerInfos.add(providerInfo);
        providerGroup.setProviderInfos(providerInfos);

        providerGroups.add(providerGroup);

        if (EventBus.isEnable(ConsumerSubEvent.class)) {
            ConsumerSubEvent event = new ConsumerSubEvent(config);
            EventBus.post(event);
        }

        return providerGroups;
    }

    protected void registerAppInfoOnce(String appName) {
        synchronized (MeshRegistry.class) {
            if (!registedApp) {
                ApplicationInfoRequest applicationInfoRequest = buildApplicationRequest(appName);
                boolean registed = client.registeApplication(applicationInfoRequest);
                if (!registed) {
                    throw new RuntimeException("registe application occors error," + applicationInfoRequest);
                } else {
                    registedApp = true;
                }
            }
        }
    }

    /**
     * can be extended
     *
     * @param appName
     * @return
     */
    protected ApplicationInfoRequest buildApplicationRequest(String appName) {
        ApplicationInfoRequest applicationInfoRequest = new ApplicationInfoRequest();
        applicationInfoRequest.setAppName(appName);
        return applicationInfoRequest;
    }

    protected String fillProtocolAndVersion(SubscribeServiceResult subscribeServiceResult, String targetURL,
                                            String serviceName) {

        final List<String> datas = subscribeServiceResult.getDatas();

        if (datas == null) {
            targetURL = targetURL + ":" + MeshConstants.TCP_PORT;
        } else {
            for (String data : subscribeServiceResult.getDatas()) {
                String param = data.substring(data.indexOf("?"));
                targetURL = targetURL + ":" + MeshConstants.TCP_PORT;
                targetURL = targetURL + param;
                break;
            }
        }
        return targetURL;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        String key = MeshRegistryHelper.buildMeshKey(config, config.getProtocol());
        UnSubscribeServiceRequest unsubscribeRequest = new UnSubscribeServiceRequest();

        unsubscribeRequest.setServiceName(key);
        client.unSubscribeService(unsubscribeRequest);
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

    @Override
    public void destroy() {
        // 销毁前备份一下
        client = null;
    }
}
