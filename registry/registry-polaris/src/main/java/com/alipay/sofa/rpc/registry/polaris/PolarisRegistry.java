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
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
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
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.buildUniqueName;
import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.convertProviderToMap;
import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.getServerHost;

/**
 * the main logic of polaris registry, similar to consul
 *
 * @author <a href=mailto:bner666@gmail.com>ZhangLibin</a>
 */
@Extension("polaris")
public class PolarisRegistry extends Registry {

    public static final String EXT_NAME = "PolarisRegistry";

    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisRegistry.class);

    private final PolarisRegistryProperties properties;

    public ProviderAPI providerAPI;

    public ConsumerAPI consumerAPI;

    private ScheduledExecutorService heartbeatExecutor;

    private Map<String, ScheduledFuture> heartbeatFutures = new ConcurrentHashMap<>();

    private Map<String, PolarisWatcher> polarisWatchers = new ConcurrentHashMap<>();


    protected PolarisRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
        this.properties = new PolarisRegistryProperties(registryConfig.getParameters());
    }

    public static String buildServiceName(AbstractInterfaceConfig config) {
        return ConfigUniqueNameGenerator.getUniqueName(config);
    }

    @Override
    public void init() {
        if (providerAPI != null) {
            return;
        }

        ConfigurationImpl configuration = new ConfigurationImpl();
        //init configuration
        configuration.setDefault();
        ServerConnectorConfigImpl serverConnector = configuration.getGlobal().getServerConnector();
        serverConnector.setAddresses(Arrays.asList(registryConfig.getAddress()));
        serverConnector.setConnectTimeout((long) registryConfig.getConnectTimeout());
        serverConnector.setProtocol(properties.getConnectorProtocol());

        providerAPI = DiscoveryAPIFactory.createProviderAPIByConfig(configuration);
        consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration);

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
                    LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, config.getInterfaceId()));
                }
                for (InstanceRegisterRequest service : services) {
                    registerPolarisService(config, service);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB, config.getInterfaceId()));
                    }
                }
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, config.getInterfaceId()));
                }
            }
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_REG_PROVIDER, "polarisRegistry", config.buildKey()), e);
        }
        if (EventBus.isEnable(ProviderPubEvent.class)) {
            ProviderPubEvent event = new ProviderPubEvent(config);
            EventBus.post(event);
        }

    }

    //convert ProviderConfig to polaris registerRequest
    public List<InstanceRegisterRequest> buildPolarisRegister(ProviderConfig config) {

        List<ServerConfig> serverConfigs = config.getServer();
        if (CommonUtils.isEmpty(serverConfigs)) {
            return Collections.emptyList();
        }

        List<InstanceRegisterRequest> requestList = new ArrayList<>();
        for (ServerConfig serverConfig : serverConfigs) {
            InstanceRegisterRequest request = new InstanceRegisterRequest();
            request.setNamespace(buildNameSpace(config.getAppName()));
            request.setService(buildServiceName(config));
            request.setHost(getServerHost(serverConfig));
            request.setPort(serverConfig.getPort());
            request.setPriority(config.getPriority());
            request.setProtocol(serverConfig.getProtocol());
            request.setWeight(config.getWeight());
            request.setTimeoutMs(config.getTimeout());
            request.setVersion(config.getVersion());
            request.setTtl(properties.getHealthCheckTTL());
            Map<String, String> metaData = convertProviderToMap(config, serverConfig);
            checkAndDelNull(metaData);
            request.setMetadata(metaData);
            requestList.add(request);
        }
        return requestList;
    }

    private String buildNameSpace(String appName) {
        return null == appName ? "sofa-default" : appName;
    }

    private void checkAndDelNull(Map<String, String> metaData) {
        metaData.entrySet().removeIf((e) -> e.getValue() == null);
    }

    private void registerPolarisService(ProviderConfig config, InstanceRegisterRequest service) {
        providerAPI.register(service);
        if (service.getTtl() != null) {
            ScheduledFuture<?> scheduledFuture = heartbeatExecutor.scheduleAtFixedRate(() -> heartbeatPolaris(service), 0, properties.getHeartbeatInterval(), TimeUnit.MILLISECONDS);
            ScheduledFuture oldFuture = heartbeatFutures.put(buildUniqueName(config, service.getProtocol()), scheduledFuture);
            if (oldFuture != null) {
                oldFuture.cancel(true);
            }
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
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }
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
        //computeIfAbsent avoid creating multiple informers and some Listeners failure due to multiple subscribe
        PolarisWatcher polarisWatcher = polarisWatchers.computeIfAbsent(uniqueName,key->{
            PolarisWatcher watcher = new PolarisWatcher(buildNameSpace(config.getAppName()), buildServiceName(config), config.getProtocol(), consumerAPI, properties);
            watcher.init();
            return watcher;
        });
        polarisWatcher.addListener(config.getProviderInfoListener());
        return polarisWatcher.currentProviders();
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isSubscribe()) {
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
