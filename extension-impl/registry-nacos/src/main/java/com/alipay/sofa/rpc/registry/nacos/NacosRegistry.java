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
package com.alipay.sofa.rpc.registry.nacos;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;

/**
 * <p>Simple Nacos registry. Features: <br/>
 * 1. register publisher as instance to nacos server.
 * 2. subscribe instances change event
 *
 * <pre>
 *     Structure of nacos storage:
 *     --sofa-rpc (namespace)
 *        |--com.alipay.sofa.rpc.example.HelloService (serviceName):v1.1 (uniqueId):DEFAULT (protocol)
 *        |   |--default-cluster (cluster)
 *        |   |   |--instances
 *        |   |   |   |--{"ip": "192.168.1.100", "port": 22000, "metaData": {"protocol": "bolt", "timeout": "1000", ...}}
 *        |   |   |   |--{"ip": "192.168.1.110", "port": 22000, "metaData": {"protocol": "bolt", "timeout": "1000", ...}}
 *        |--com.alipay.sofa.rpc.example.EchoService (next serviceName):grpc (protocol)
 *        |......
 * </pre>
 *
 *  Remark:
 *  Here we register service name with not only serviceName, but also with 'uniqueId' and 'protocol',
 *  because in Nacos, all service instances(with same service name) are only identified by ip and port,
 *  if there are two service with same service name but different uniqueId, there will be only one instance remained in instance list,
 *  and the consumer can't find the other instance from Nacos
 * </p>
 *
 * @author <a href=mailto:jervyshi@gmail.com>JervyShi</a>
 */
@Extension("nacos")
public class NacosRegistry extends Registry {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                           LOGGER            = LoggerFactory
                                                                                .getLogger(NacosRegistry.class);

    private static final String                           DEFAULT_NAMESPACE = "sofa-rpc";

    private NamingService                                 namingService;

    private NacosRegistryProviderObserver                 providerObserver;

    private List<String>                                  defaultCluster;

    private ConcurrentMap<ProviderConfig, List<Instance>> providerInstances = new ConcurrentHashMap<ProviderConfig, List<Instance>>();

    private ConcurrentMap<ConsumerConfig, EventListener>  consumerListeners = new ConcurrentHashMap<ConsumerConfig, EventListener>();

    private Properties                                    nacosConfig       = new Properties();

    /**
     * Instantiates a new Nacos registry.
     *
     * @param registryConfig the registry config
     */
    public NacosRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    @Override
    public synchronized void init() {
        if (namingService != null) {
            return;
        }

        String addressInput = registryConfig.getAddress(); // xxx:8848,yyy:8848/namespace
        if (StringUtils.isEmpty(addressInput)) {
            throw new SofaRpcRuntimeException("Address of nacos registry is empty.");
        }
        int idx = addressInput.indexOf(CONTEXT_SEP);
        String namespace;
        String address; // IP地址
        if (idx > 0) {
            address = addressInput.substring(0, idx);
            namespace = addressInput.substring(idx + 1);
            //for host:port/ this scene
            if (StringUtils.isBlank(namespace)) {
                namespace = DEFAULT_NAMESPACE;
            }
        } else {
            address = addressInput;
            namespace = DEFAULT_NAMESPACE;
        }

        defaultCluster = Collections.singletonList(NacosRegistryHelper.DEFAULT_CLUSTER);

        nacosConfig.put(PropertyKeyConst.SERVER_ADDR, address);
        nacosConfig.put(PropertyKeyConst.NAMESPACE, namespace);

        try {
            namingService = NamingFactory.createNamingService(nacosConfig);
        } catch (NacosException e) {
            throw new SofaRpcRuntimeException("Init nacos naming service error, address: " + address);
        }
    }

    @Override
    public boolean start() {
        if (namingService == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Nacos client should be initialized before starting.");
            }
            return false;
        }
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

        if (config.isRegister()) {
            // register server
            try {
                List<Instance> instances = NacosRegistryHelper.convertProviderToInstances(config);
                if (CommonUtils.isNotEmpty(instances)) {
                    for (Instance instance : instances) {
                        String serviceName = instance.getServiceName();
                        if (LOGGER.isInfoEnabled(appName)) {
                            LOGGER.infoWithApp(appName,
                                LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, serviceName));
                        }
                        namingService.registerInstance(serviceName, instance);
                        if (LOGGER.isInfoEnabled(appName)) {
                            LOGGER.infoWithApp(appName,
                                LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, serviceName));
                        }
                    }
                    providerInstances.put(config, instances);
                }
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to register provider to nacosRegistry! service: "
                    + config.buildKey(), e);
            }
        }
    }

    @Override
    public void unRegister(ProviderConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isRegister()) {
            // registry ignored
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }

        // unregister publisher
        if (config.isRegister()) {
            try {
                List<Instance> instances = providerInstances.remove(config);
                if (CommonUtils.isNotEmpty(instances)) {
                    for (Instance instance : instances) {
                        String serviceName = instance.getServiceName();
                        namingService.deregisterInstance(serviceName, instance.getIp(), instance.getPort(),
                            instance.getClusterName());
                        if (LOGGER.isInfoEnabled(appName)) {
                            LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB,
                                serviceName, instances.size()));
                        }
                    }
                }

            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException(
                        "Failed to unregister provider to nacos registry! service: "
                            + config.buildKey(), e);
                }
            }
        }

    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        for (ProviderConfig config : configs) {
            String appName = config.getAppName();
            try {
                unRegister(config);
            } catch (Exception e) {
                LOGGER.errorWithApp(appName, "Batch unregister from nacos error", e);
            }
        }
    }

    @Override
    public List<ProviderGroup> subscribe(final ConsumerConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isSubscribe()) {
            // registry ignored
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return null;
        }

        if (config.isSubscribe()) {
            String serviceName = NacosRegistryHelper.buildServiceName(config, config.getProtocol());

            if (LOGGER.isInfoEnabled()) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_SUB, serviceName));
            }

            try {
                if (providerObserver == null) {
                    providerObserver = new NacosRegistryProviderObserver();
                }

                ProviderInfoListener providerInfoListener = config.getProviderInfoListener();
                providerObserver.addProviderListener(config, providerInfoListener);

                EventListener eventListener = new EventListener() {
                    @Override
                    public void onEvent(Event event) {
                        if (event instanceof NamingEvent) {
                            NamingEvent namingEvent = (NamingEvent) event;
                            List<Instance> instances = namingEvent.getInstances();
                            // avoid npe
                            if (null == instances) {
                                instances = new ArrayList<Instance>();
                            }
                            providerObserver.updateProviders(config, instances);
                        }
                    }
                };
                namingService.subscribe(serviceName, defaultCluster, eventListener);
                consumerListeners.put(config, eventListener);

                List<Instance> allInstances = namingService.getAllInstances(serviceName, defaultCluster);

                List<ProviderInfo> providerInfos = NacosRegistryHelper.convertInstancesToProviders(allInstances);
                List<ProviderInfo> matchProviders = RegistryUtils.matchProviderInfos(config, providerInfos);
                return Collections.singletonList(new ProviderGroup().addAll(matchProviders));
            } catch (Exception e) {
                throw new SofaRpcRuntimeException(
                    "Failed to subscribe provider from nacosRegistry, service: " + serviceName, e);
            }

        }

        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        if (config.isSubscribe()) {
            String serviceName = NacosRegistryHelper.buildServiceName(config, config.getProtocol());
            try {
                EventListener eventListener = consumerListeners.remove(config);
                if (null != eventListener) {
                    namingService.unsubscribe(serviceName, defaultCluster, eventListener);
                }
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException(
                        "Failed to unsubscribe listener from nacosRegistry, service:" + serviceName, e);
                }
            }

            providerObserver.removeProviderListener(config);
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
        for (ProviderConfig providerConfig : providerInstances.keySet()) {
            unRegister(providerConfig);
        }
        for (ConsumerConfig consumerConfig : consumerListeners.keySet()) {
            unSubscribe(consumerConfig);
        }
        namingService = null;
        providerObserver = null;
    }

    public Properties getNacosConfig() {
        return nacosConfig;
    }
}
