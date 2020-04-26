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
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.alipay.sofa.rpc.registry.consul.ConsulUtils.buildServiceId;
import static com.alipay.sofa.rpc.registry.consul.ConsulUtils.buildServiceIds;
import static com.alipay.sofa.rpc.registry.consul.ConsulUtils.buildServiceName;
import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.buildUniqueName;
import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.getServerHost;

/**
 * <p>
 * Consul Registry. Features:
 *
 * <ol>
 * <li> register publisher as instance to consul agent.</li>
 * <li> subscribe instances change event.</li>
 * <li> custom health check, e.g. tcp, http.</li>
 * </ol>
 * <p>
 * The data structure in Consul consists of three parts: service name, service id, tag.
 *
 * <ol>
 * <li> service name is the human-readable name of each service. In sofa-rpc, the default value is interfaceId.</li>
 * <li> tag can be used to filter a set of instances which can be subscribed, we use interfaceId + version + uniqueId + protocol to identify it.</li>
 * <li> each instance needs to have a unique service id so it won't be overwritten by other instances, we use tag + host + port to identify it.</li>
 * </ol>
 * <p>
 * Here is an example:
 * <pre>
 * {
 *     Service: "com.alipay.sofa.rpc.registry.consul.TestService",
 *     Tags: [
 *       "com.alipay.sofa.rpc.registry.consul.TestService:1.0:default@DEFAULT"
 *     ],
 *     ID: "com.alipay.sofa.rpc.registry.consul.TestService:1.0:default@DEFAULT-127.0.0.1-12200"
 * }
 * </pre>
 * </p>
 *
 * @author <a href=mailto:preciousdp11@gmail.com>dingpeng</a>
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 * @since 5.5.0
 */
@Extension("consul")
public class ConsulRegistry extends Registry {

    public static final String EXT_NAME="ConsulRegistry";

    /**
     * Logger
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ConsulRegistry.class);

    private final ConsulRegistryProperties properties;

    private Map<String, ScheduledFuture> heartbeatFutures = new ConcurrentHashMap<>();

    private Map<String, HealthServiceInformer> healthServiceInformers = new ConcurrentHashMap<>();

    private ConsulClient consulClient;

    private ScheduledExecutorService heartbeatExecutor;

    protected ConsulRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
        this.properties = new ConsulRegistryProperties(registryConfig.getParameters());
    }

    @Override
    public void init() {
        if (consulClient != null) {
            return;
        }

        String[] hostAndPort = StringUtils.split(registryConfig.getAddress(), ":");
        String host = hostAndPort[0];
        int port = hostAndPort.length > 1 ? Integer.parseInt(hostAndPort[1]) : ConsulConstants.DEFAULT_CONSUL_PORT;
        consulClient = new ConsulClient(host, port);

        int coreSize = properties.getHeartbeatCoreSize();

        heartbeatExecutor = Executors.newScheduledThreadPool(coreSize);
    }

    @Override
    public void destroy() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
        healthServiceInformers.values().forEach(HealthServiceInformer::shutdown);
    }

    @Override
    public void destroy(DestroyHook hook) {
        hook.preDestroy();
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
            // 只订阅不注册
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }
        if (!config.isRegister()) {
            return;
        }
        // 注册服务端节点
        try {
            List<NewService> services = buildNewServices(config);
            if (CommonUtils.isNotEmpty(services)) {
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName,
                            LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, config.getInterfaceId()));
                }
                for (NewService service : services) {
                    registerConsulService(service);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB, config.getInterfaceId()));
                    }
                }
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName,
                            LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, config.getInterfaceId()));
                }
            }
        }catch (SofaRpcRuntimeException e){
            throw e;
        } catch (Exception e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_REG_PROVIDER, "consulRegistry", config.buildKey()), e);
        }

        if (EventBus.isEnable(ProviderPubEvent.class)) {
            ProviderPubEvent event = new ProviderPubEvent(config);
            EventBus.post(event);
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
            List<String> ids = buildServiceIds(config);
            if (CommonUtils.isNotEmpty(ids)) {
                ids.forEach(this::deregisterConsulService);
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB,
                            config.getInterfaceId(), ids.size()));
                }
            }
        } catch (Exception e) {
            if (!RpcRunningState.isShuttingDown()) {
                if ( e instanceof SofaRpcRuntimeException){
                    throw e;
                }else{
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_UNREG_PROVIDER ,EXT_NAME), e);
            }}
        }
    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        configs.forEach(this::unRegister);
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
            List<ProviderInfo> providers = lookupHealthService(config);

            if (EventBus.isEnable(ConsumerSubEvent.class)) {
                ConsumerSubEvent event = new ConsumerSubEvent(config);
                EventBus.post(event);
            }

            return Collections.singletonList(new ProviderGroup().addAll(providers));
        } catch (SofaRpcRuntimeException e){
            throw e;
        }catch (Exception e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_SUB_PROVIDER ,EXT_NAME), e);
        }
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
        HealthServiceInformer informer = healthServiceInformers.get(uniqueName);
        if (informer == null) {
            return;
        }
        informer.removeListener(config.getProviderInfoListener());
        if (informer.getListenerSize() == 0) {
            healthServiceInformers.remove(uniqueName);
            informer.shutdown();
        }
    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {
        configs.forEach(this::unSubscribe);
    }

    private List<ProviderInfo> lookupHealthService(ConsumerConfig config) {
        String uniqueName = buildUniqueName(config, config.getProtocol());
        String serviceName = buildServiceName(config);
        String informerKey = String.join("-", serviceName, uniqueName);
        HealthServiceInformer informer = healthServiceInformers.get(informerKey);
        if (informer == null) {
            informer = new HealthServiceInformer(serviceName, uniqueName, consulClient, properties);
            informer.init();
            healthServiceInformers.put(informerKey, informer);
        }
        informer.addListener(config.getProviderInfoListener());
        return informer.currentProviders();
    }

    private void deregisterConsulService(String id) {
        consulClient.agentServiceDeregister(id);
        ScheduledFuture future = heartbeatFutures.remove(id);
        if (future != null) {
            future.cancel(true);
        }
    }

    private void registerConsulService(NewService service) {
        consulClient.agentServiceRegister(service);
        if (service.getCheck().getTtl() != null) {
            ScheduledFuture<?> scheduledFuture =
                    heartbeatExecutor.scheduleAtFixedRate(
                            () -> checkPass(service),
                            0, properties.getHeartbeatInterval(), TimeUnit.MILLISECONDS);

            // multiple heartbeat use the same service id, remove and cancel the old one, or still use it?
            ScheduledFuture oldFuture = heartbeatFutures.remove(service.getId());
            if (oldFuture != null) {
                oldFuture.cancel(true);
            }
            heartbeatFutures.put(service.getId(), scheduledFuture);
        }
    }

    private void checkPass(NewService service) {
        try {
            consulClient.agentCheckPass("service:" + service.getId(), "TTL check passing by SOFA RPC");
        } catch (Exception e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_CHECK_PASS ,"Consul"), e);
        }
    }

    private List<NewService> buildNewServices(ProviderConfig<?> config) {
        List<ServerConfig> servers = config.getServer();
        if (CommonUtils.isEmpty(servers)) {
            return Collections.emptyList();
        }
        return servers.stream().map(server -> {
            NewService service = new NewService();
            service.setId(buildServiceId(config, server));
            service.setName(buildServiceName(config));

            String host = getServerHost(server);
            int port = server.getPort();
            service.setAddress(host);
            service.setPort(port);

            Map<String, String> metaData = RegistryUtils.convertProviderToMap(config, server).entrySet().stream()
                    .filter(e -> ConsulUtils.isValidMetaKey(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            service.setMeta(metaData);
            service.setTags(Collections.singletonList(buildUniqueName(config, server.getProtocol())));

            service.setCheck(buildCheck(host, port));
            return service;
        }).collect(Collectors.toList());
    }

    private NewService.Check buildCheck(String serverHost, int serverPort) {
        NewService.Check check = new NewService.Check();
        ConsulRegistryProperties.HealthCheckType healthCheckType = properties.getHealthCheckType();
        if (healthCheckType == ConsulRegistryProperties.HealthCheckType.TTL) {
            check.setTtl(properties.getHealthCheckTTL());
        } else if (healthCheckType == ConsulRegistryProperties.HealthCheckType.TCP) {
            String host = properties.getHealthCheckHost(serverHost);
            int port = properties.getHealthCheckPort(serverPort);
            check.setTcp(host + ":" + port);
            check.setInterval(properties.getHealthCheckInterval());
            check.setTimeout(properties.getHealthCheckTimeout());
        } else {
            String host = properties.getHealthCheckHost(serverHost);
            int port = properties.getHealthCheckPort(serverPort);
            String address;
            try {
                address = new URL(properties.getHealthCheckProtocol(), host, port, properties.getHealthCheckPath()).toString();
            } catch (SofaRpcRuntimeException e){
                throw e;
            }catch (Exception e) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_HEALTH_CHECK_URL ), e);
            }
            check.setHttp(address);
            check.setMethod(properties.getHealthCheckMethod());
            check.setInterval(properties.getHealthCheckInterval());
            check.setTimeout(properties.getHealthCheckTimeout());
        }
        return check;
    }
}
