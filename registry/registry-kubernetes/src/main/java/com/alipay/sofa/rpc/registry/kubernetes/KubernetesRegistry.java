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
package com.alipay.sofa.rpc.registry.kubernetes;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.annotation.VisibleForTesting;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
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
import com.alipay.sofa.rpc.registry.kubernetes.utils.KubernetesClientUtils;
import com.alipay.sofa.rpc.registry.kubernetes.utils.KubernetesConfigUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Extension("kubernetes")
public class KubernetesRegistry extends Registry {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(KubernetesRegistry.class);

    private KubernetesClient kubernetesClient;

    private String currentHostname;

    private String namespace;

    private KubernetesRegistryProviderWatcher kubernetesRegistryProviderWatcher;

    private final ConcurrentMap<ConsumerConfig, SharedIndexInformer<Pod>> consumerListeners = new ConcurrentHashMap<>(64);

    /**
     * Instantiates a new kubernetes registry.
     *
     * @param registryConfig
     */
    public KubernetesRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    @Override
    public synchronized void init() {
        // init kubernetes config
        Config config = KubernetesConfigUtils.buildKubernetesConfig(registryConfig);
        // init kubernetes client
        if (kubernetesClient == null) {
            this.kubernetesClient = KubernetesClientUtils.buildKubernetesClient(config);
        }
        // init Watcher
        if (kubernetesRegistryProviderWatcher == null) {
            kubernetesRegistryProviderWatcher = new KubernetesRegistryProviderWatcher();
        }
        this.currentHostname = System.getenv("HOSTNAME");
        this.namespace = config.getNamespace();
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
            PodResource podResource = kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(currentHostname);

            List<ServerConfig> serverConfigs = config.getServer();

            if (CommonUtils.isNotEmpty(serverConfigs)) {
                for (ServerConfig serverConfig : serverConfigs) {
                    String dataId = KubernetesRegistryHelper.buildDataId(config, serverConfig.getProtocol());
                    // 对外提供服务的URL
                    String url = KubernetesRegistryHelper.convertToUrl(podResource.get(), serverConfig, config);

                    podResource.edit(pod -> new PodBuilder(pod).editOrNewMetadata()
                            // 将ProviderConfig存在Annotations上
                            .addToAnnotations(dataId, url)
                            // 为了过滤pod、其实value是用不到的
                            .addToLabels(dataId, "")
                            .endMetadata().build());
                }
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
            PodResource podResource = kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(currentHostname);

            List<ServerConfig> serverConfigs = config.getServer();
            if (CommonUtils.isNotEmpty(serverConfigs)) {
                for (ServerConfig serverConfig : serverConfigs) {
                    String dataId = KubernetesRegistryHelper.buildDataId(config, serverConfig.getProtocol());

                    podResource.edit(pod -> new PodBuilder(pod).editOrNewMetadata()
                            .removeFromAnnotations(dataId)
                            .removeFromLabels(dataId)
                            .endMetadata()
                            .build());
                }
            }
        }
    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        // one by one
        for (ProviderConfig config : configs) {
            try {
                this.unRegister(config);
            } catch (Exception e) {
                LOGGER.errorWithApp(config.getAppName(), "Batch unregister error", e);
            }
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

            ProviderInfoListener providerInfoListener = config.getProviderInfoListener();
            kubernetesRegistryProviderWatcher.addProviderListener(config, providerInfoListener);

            String dataId = KubernetesRegistryHelper.buildDataId(config, config.getProtocol());
            FilterWatchListDeletable<Pod, PodList, PodResource> podPodListPodResourceFilterWatchListDeletable =
                    kubernetesClient.pods()
                            .inNamespace(namespace)
                            .withLabel(dataId);

            SharedIndexInformer<Pod> inform = podPodListPodResourceFilterWatchListDeletable.inform(new ResourceEventHandler<Pod>() {
                @Override
                public void onAdd(Pod pod) {
                    kubernetesRegistryProviderWatcher.updateProviders(config, getPods());
                }

                @Override
                public void onUpdate(Pod pod, Pod t1) {
                    kubernetesRegistryProviderWatcher.updateProviders(config, getPods());
                }

                @Override
                public void onDelete(Pod pod, boolean b) {
                    kubernetesRegistryProviderWatcher.updateProviders(config, getPods());
                }
            });

            consumerListeners.put(config, inform);

            inform.start();

            List<Pod> pods = podPodListPodResourceFilterWatchListDeletable.list().getItems();
            List<ProviderInfo> providerInfos = KubernetesRegistryHelper.convertPodsToProviders(pods, config);
            return Collections.singletonList(new ProviderGroup().addAll(providerInfos));
        }

        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        if (config.isSubscribe()) {
            SharedIndexInformer<Pod> informer = consumerListeners.remove(config);
            if (null != informer) {
                informer.stop();
            }
        }

        kubernetesRegistryProviderWatcher.removeProviderListener(config);
    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {
        // one by one
        for (ConsumerConfig config : configs) {
            try {
                this.unSubscribe(config);
            } catch (Exception e) {
                LOGGER.errorWithApp(config.getAppName(), "Batch unSubscribe error", e);
            }
        }
    }

    @Override
    public void destroy() {
        // unRegister consumer
        consumerListeners.forEach((k, v) -> unSubscribe(k));

        // close kubernetes client
        kubernetesClient.close();
    }

    private List<Pod> getPods() {
        return kubernetesClient.pods()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    /**
     * UT used only
     */
    @VisibleForTesting
    public void setCurrentHostname(String currentHostname) {
        this.currentHostname = currentHostname;
    }

    /**
     * UT used only
     */
    @VisibleForTesting
    public ConcurrentMap<ConsumerConfig, SharedIndexInformer<Pod>> getConsumerListeners() {
        return consumerListeners;
    }
}