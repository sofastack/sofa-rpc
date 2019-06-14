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
import com.alipay.sofa.rpc.ext.Extensible;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;
import com.alipay.sofa.rpc.registry.etcd.client.ClientBuilder;
import com.alipay.sofa.rpc.registry.etcd.client.EtcdClient;
import com.alipay.sofa.rpc.registry.utils.RegistryUtils;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * etcd registry
 *
 * @created 2018/7/8
 */
@Extension("etcd")
public class EtcdRegistry extends Registry {

    private final static Logger                                  LOGGER          = LoggerFactory
                                                                                     .getLogger(EtcdRegistry.class);

    private static String                                        CONFIG_USER     = "user";
    private static String                                        CONFIG_PASSWORD = "password";

    private EtcdClient                                           client;
    private EtcdHelper                                           etcdHelper;

    /**
     * 保存服务发布者的url
     */
    private ConcurrentMap<ProviderConfig, List<ServiceInstance>> providers       = new ConcurrentHashMap<ProviderConfig, List<ServiceInstance>>();

    /**
     * 保存服务消费者的url
     */
    private ConcurrentMap<ConsumerConfig, List<ProviderGroup>>   consumers       = new ConcurrentHashMap<ConsumerConfig, List<ProviderGroup>>();
    private EtcdProviderObserver                                 providerObserver;

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected EtcdRegistry(RegistryConfig registryConfig) {
        super(registryConfig);

    }

    @Override
    public void init() {
        ClientBuilder clientBuilder = EtcdClient.builder().endpoints(registryConfig.getAddress());
        //get user and password form configuration
        String user = registryConfig.getParameter(CONFIG_USER);
        String password = registryConfig.getParameter(CONFIG_PASSWORD);
        if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
            clientBuilder.auth(user, password);
        }
        this.providerObserver = new EtcdProviderObserver();
        this.client = clientBuilder.build();
        this.etcdHelper = new EtcdHelper(client);
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

        if (config.isRegister()) {
            // register server
            try {
                List<ServiceInstance> instances = EtcdRegistryHelper.convertProviderToInstances(config);
                if (CommonUtils.isNotEmpty(instances)) {
                    String serviceName = config.getInterfaceId();

                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName,
                            LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, serviceName));
                    }

                    for (ServiceInstance instance : instances) {
                        etcdHelper.register(instance);
                    }
                    providers.put(config, instances);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName,
                            LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, serviceName));
                    }
                }
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to register provider to etcd registry!", e);
            }
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

        if (config.isRegister()) {
            String serviceName = config.getInterfaceId();
            try {
                List<ServiceInstance> instances = providers.remove(config);
                if (CommonUtils.isNotEmpty(instances)) {
                    for (ServiceInstance instance : instances) {
                        etcdHelper.deregister(instance);
                    }
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB,
                            serviceName, instances.size()));
                    }
                }

            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException(
                        "Failed to unregister provider to etcd registry! service: " + serviceName, e);
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
            // registry ignored
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return null;
        }

        if (config.isSubscribe()) {
            String serviceName = config.getInterfaceId();

            if (LOGGER.isInfoEnabled()) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_SUB, serviceName));
            }

            try {
                List<ServiceInstance> allInstances = etcdHelper.getInstances(serviceName, config.getProtocol(),
                    config.getUniqueId());
                List<ProviderInfo> providerInfos = EtcdRegistryHelper.convertInstancesToProviders(allInstances);
                List<ProviderInfo> matchProviders = RegistryUtils.matchProviderInfos(config, providerInfos);
                List<ProviderGroup> providerGroups = Collections.singletonList(new ProviderGroup()
                    .addAll(matchProviders));
                consumers.put(config, providerGroups);
                providerObserver.addProviderListener(config, config.getProviderInfoListener());
                etcdHelper.startWatch(new Watcher(etcdHelper, config, providerObserver));
                return providerGroups;
            } catch (Exception e) {
                throw new SofaRpcRuntimeException(
                    "Failed to subscribe provider from etcd registry, service: " + serviceName, e);
            }
        }
        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        if (config.isSubscribe()) {
            String serviceName = config.getInterfaceId();
            try {
                consumers.remove(config);
                providerObserver.removeProviderListener(config);
                etcdHelper.unsubscribe(config);
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException(
                        "Failed to unsubscribe listener from etcd registry, service:" + serviceName, e);
                }
            }

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
        for (ProviderConfig providerConfig : providers.keySet()) {
            unRegister(providerConfig);
        }
        for (ConsumerConfig consumerConfig : consumers.keySet()) {
            unSubscribe(consumerConfig);
        }
        client.close();
        providerObserver = null;
    }
}
