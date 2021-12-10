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
package com.alipay.sofa.rpc.registry.polaris;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;

import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
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
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.config.global.SystemConfig;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.*;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.discovery.client.api.DefaultProviderAPI;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.APIConfigImpl;
import com.tencent.polaris.factory.config.global.GlobalConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.config.global.SystemConfigImpl;

import java.util.*;
import java.util.concurrent.*;

import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.*;


@Extension("polaris")
public class PolarisRegistry extends Registry {
    public static final String EXT_NAME = "PolarisRegistry";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(PolarisRegistry.class);

    ProviderAPI providerAPI;
    ConsumerAPI consumerAPI;
    private ScheduledExecutorService heartbeatExecutor;
    private Map<String, ScheduledFuture> heartbeatFutures = new ConcurrentHashMap<>();
    private final PolarisRegistryProperties properties;
    private Map<String, PolarisWatcher> polarisWatchers = new ConcurrentHashMap<>();


    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected PolarisRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
        this.properties = new PolarisRegistryProperties(registryConfig.getParameters());
    }

    @Override
    public void init() {

        ConfigurationImpl configuration = new ConfigurationImpl();
        //init configuration
        configuration.setDefault();
        ServerConnectorConfigImpl serverConnector = configuration.getGlobal().getServerConnector();
        SystemConfigImpl system = configuration.getGlobal().getSystem();
        APIConfigImpl api = configuration.getGlobal().getAPI();

        //host:port
        serverConnector.setAddresses(Arrays.asList(registryConfig.getAddress()));
        serverConnector.setConnectTimeout((long) registryConfig.getConnectTimeout());
        //TODO: more config

        providerAPI = DiscoveryAPIFactory.createProviderAPI();
        consumerAPI = DiscoveryAPIFactory.createConsumerAPI();

        int coreSize = properties.getHeartbeatCoreSize();
        heartbeatExecutor = Executors.newScheduledThreadPool(coreSize);

    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void register(ProviderConfig config) {
        String appName = config.getAppName();

        if (!registryConfig.isRegister()) {
            // 只订阅不注册
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }
        if (!config.isRegister()) {
            return;
        }

        try {
            List<InstanceRegisterRequest> services = buildPolarisRegister(config);
            if (CommonUtils.isNotEmpty(services)) {
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName,
                            LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, config.getInterfaceId()));
                }
                for (InstanceRegisterRequest service : services) {
                    registerPolarisService(config, service);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB, config.getInterfaceId()));
                    }
                }
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName,
                            LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, config.getInterfaceId()));
                }
            }
        } catch (
                SofaRpcRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_REG_PROVIDER, "consulRegistry", config.buildKey()), e);
        }
        if (EventBus.isEnable(ProviderPubEvent.class)) {
            ProviderPubEvent event = new ProviderPubEvent(config);
            EventBus.post(event);
        }

    }

    //convert ProviderConfig to polaris registerRequest
    public List<InstanceRegisterRequest> buildPolarisRegister(ProviderConfig config) {

        List<ServerConfig> servers = config.getServer();
        if (CommonUtils.isEmpty(servers)) {
            return Collections.emptyList();
        }

        List<InstanceRegisterRequest> res = new ArrayList<>();
        for (ServerConfig server : servers) {
            InstanceRegisterRequest service = new InstanceRegisterRequest();
            service.setNamespace(config.getAppName());
            service.setService(config.getInterfaceId());
            service.setHost(getServerHost(server));
            service.setPort(server.getPort());
            service.setPriority(config.getPriority());
            service.setProtocol(server.getProtocol());
            service.setWeight(config.getWeight());
            service.setTimeoutMs(config.getTimeout());
            // service.setTtl(properties.getHealthCheckTTL());
            //service.setToken();
            Map<String, String> metaData = convertProviderToMap(config, server);
            service.setMetadata(metaData);
            res.add(service);
        }

        return res;
    }

    private void registerPolarisService(ProviderConfig config, InstanceRegisterRequest service) {

        InstanceRegisterResponse response = providerAPI.register(service);
        if (service.getTtl() != null) {
            ScheduledFuture<?> scheduledFuture =
                    heartbeatExecutor.scheduleAtFixedRate(
                            () -> heartbeatPolaris(service),
                            0, properties.getHeartbeatInterval(), TimeUnit.MILLISECONDS);

            // multiple heartbeat use the same service id, remove and cancel the old one, or still use it?
            ScheduledFuture oldFuture = heartbeatFutures.remove(buildUniqueName(config, service.getProtocol()));
            if (oldFuture != null) {
                oldFuture.cancel(true);
            }
            heartbeatFutures.put(response.getInstanceId(), scheduledFuture);
        }
    }

    private void heartbeatPolaris(InstanceRegisterRequest service) {
        try {
            InstanceHeartbeatRequest instanceHeartbeatRequest = new InstanceHeartbeatRequest();
            instanceHeartbeatRequest.setNamespace(service.getNamespace());
            instanceHeartbeatRequest.setService(service.getService());
            instanceHeartbeatRequest.setHost(service.getHost());
            instanceHeartbeatRequest.setPort(service.getPort());
            providerAPI.heartbeat(instanceHeartbeatRequest);
        } catch (Exception e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_CHECK_PASS, "Polaris"), e);
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
        if (!config.isRegister()) {
            return;
        }

        try {
            List<InstanceRegisterRequest> instanceRegisterRequests = buildPolarisRegister(config);

            for (InstanceRegisterRequest request : instanceRegisterRequests) {
                deregisterPolarisService(config, request);
            }

        } catch (Exception e) {
            if (!RpcRunningState.isShuttingDown()) {
                if (e instanceof SofaRpcRuntimeException) {
                    throw e;
                } else {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_UNREG_PROVIDER, EXT_NAME), e);
                }
            }
        }
    }

    private void deregisterPolarisService(ProviderConfig config, InstanceRegisterRequest request) {

        InstanceDeregisterRequest instanceDeregisterRequest = new InstanceDeregisterRequest();
        instanceDeregisterRequest.setNamespace(request.getNamespace());
        instanceDeregisterRequest.setService(request.getService());
        instanceDeregisterRequest.setHost(request.getHost());
        instanceDeregisterRequest.setPort(request.getPort());

        providerAPI.deRegister(instanceDeregisterRequest);
        ScheduledFuture future = heartbeatFutures.remove(buildUniqueName(config, request.getProtocol()));
        if (future != null) {
            future.cancel(true);
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
        String appName = config.getAppName();
        if (!registryConfig.isSubscribe()) {
            // 注册中心不订阅
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return null;
        }
        if (!config.isSubscribe()) {
            return null;
        }

        try {
            List<ProviderInfo> providers = findService(config);
            if (EventBus.isEnable(ConsumerSubEvent.class)) {
                ConsumerSubEvent event = new ConsumerSubEvent(config);
                EventBus.post(event);
            }

            return Collections.singletonList(new ProviderGroup().addAll(providers));
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_SUB_PROVIDER, EXT_NAME), e);
        }

    }

    private List<ProviderInfo> findService(ConsumerConfig config) {

        String uniqueName = buildUniqueName(config, config.getProtocol());
        PolarisWatcher watcher = polarisWatchers.get(uniqueName);
        if (watcher == null) {
            watcher = new PolarisWatcher(config.getAppName(), config.getProtocol(), config.getInterfaceId(), consumerAPI, properties);
            watcher.init();
            polarisWatchers.put(uniqueName, watcher);
        }
        watcher.addListener(config.getProviderInfoListener());
        return watcher.currentProviders();
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isSubscribe()) {
            // 注册中心不订阅
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(config.getAppName(), LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
        }

        if (!config.isSubscribe()) {
            return;
        }
        String uniqueName = buildUniqueName(config, config.getProtocol());
        PolarisWatcher informer = polarisWatchers.get(uniqueName);
        if (informer == null) {
            return;
        }
        informer.removeListener(config.getProviderInfoListener());
        if (informer.getListenerSize() == 0) {
            polarisWatchers.remove(uniqueName);
            informer.shutdown();
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
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
        for (PolarisWatcher watcher : polarisWatchers.values()) {
            watcher.shutdown();
        }
        if (providerAPI != null) {
            providerAPI.destroy();

        }
        if (consumerAPI != null) {
            consumerAPI.destroy();
        }
    }


}
