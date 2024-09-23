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
package com.alipay.sofa.rpc.dynamic.zk;

import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.auth.AuthRuleGroup;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.dynamic.*;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;
import static com.alipay.sofa.rpc.common.utils.StringUtils.KEY_SEPARATOR;

/**
 * @author Narziss
 * @version ZookeeperDynamicConfigManager.java, v 0.1 2024年07月20日 09:23 Narziss
 */

@Extension(value = "zookeeper", override = true)
public class ZookeeperDynamicConfigManager extends DynamicConfigManager {

    private final static Logger                      LOGGER            = LoggerFactory
                                                                           .getLogger(ZookeeperDynamicConfigManager.class);

    private final CuratorFramework                   zkClient;

    private final String                      address;

    private final String                             rootPath;
    private ConcurrentMap<String, String> configMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, ZookeeperConfigListener> watchListenerMap = new ConcurrentHashMap<>();

    protected ZookeeperDynamicConfigManager(String appName) {
        super(appName);
        rootPath = CONTEXT_SEP + DynamicConfigKeys.CONFIG_NODE + CONTEXT_SEP + DynamicConfigKeys.DEFAULT_GROUP + CONTEXT_SEP + getAppName();
        address = SofaConfigs.getOrDefault(DynamicConfigKeys.ZK_ADDRESS);
        zkClient = CuratorFrameworkFactory.builder()
            .connectString(address)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .namespace(DynamicConfigKeys.DEFAULT_NAMESPACE)
            .build();
        zkClient.start();

        PathChildrenCache cache = new PathChildrenCache(zkClient, rootPath, true);
        cache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                switch (event.getType()) {
                    case CHILD_ADDED:
                    case CHILD_UPDATED:
                        String key = event.getData().getPath().substring(rootPath.length() + 1);
                        String value = new String(event.getData().getData());
                        configMap.put(key, value);
                        LOGGER.info("Receive zookeeper event: " + "type=[" + event.getType() + "] key=[" + key + "] value=[" + value + "]");
                        break;
                    case CHILD_REMOVED:
                        key = event.getData().getPath().substring(rootPath.length() + 1);
                        configMap.remove(key);
                        LOGGER.info("Receive zookeeper event: " + "type=[" + event.getType() + "] key=[" + key + "]");
                        break;
                    default:
                        break;
                }
            }
        });
        try {
            cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        } catch (Exception e) {
            LOGGER.error("setupPathChildrenCache error", e);
        }
    }

    protected ZookeeperDynamicConfigManager(String appName,String address) {
        super(appName);
        rootPath = CONTEXT_SEP + DynamicConfigKeys.CONFIG_NODE + CONTEXT_SEP + DynamicConfigKeys.DEFAULT_GROUP;
        this.address = address;
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(address)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace(DynamicConfigKeys.DEFAULT_NAMESPACE)
                .build();
        zkClient.start();
    }

    @Override
    public void initServiceConfiguration(String service) {
        //TODO not now

    }

    @Override
    public String getProviderServiceProperty(String service, String key) {
        try {
            String configValue = configMap.get(DynamicConfigKeyHelper.buildProviderServiceProKey(service, key));
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (Exception e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getConsumerServiceProperty(String service, String key) {
        try {
            String configValue = configMap.get(DynamicConfigKeyHelper.buildConsumerServiceProKey(service, key));
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (Exception e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getProviderMethodProperty(String service, String method, String key) {
        try {
            String configValue = configMap.get(DynamicConfigKeyHelper.buildProviderMethodProKey(service, method, key));
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (Exception e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getConsumerMethodProperty(String service, String method, String key) {
        try {
            String configValue = configMap.get(DynamicConfigKeyHelper.buildConsumerMethodProKey(service, method, key));
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (Exception e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }

    }

    @Override
    public AuthRuleGroup getServiceAuthRule(String service) {
        //TODO 暂不支持
        return null;
    }

    @Override
    public String getConfig(String key){
        try {
            byte[] bytes = zkClient.getData().forPath(rootPath+ CONTEXT_SEP +getAppName()+ KEY_SEPARATOR + key);
            String configValue = new String(bytes, RpcConstants.DEFAULT_CHARSET);
            return configValue != null ? configValue : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (Exception e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public void addListener(String key, ConfigListener listener){
        String pathKey = rootPath+ CONTEXT_SEP +getAppName()+ KEY_SEPARATOR + key;

        ZookeeperConfigListener zookeeperConfigListener = watchListenerMap.computeIfAbsent(
                key, k -> createTargetListener(pathKey));

        zookeeperConfigListener.addListener(listener);
    }

    private ZookeeperConfigListener createTargetListener(String pathKey) {
        ZookeeperConfigListener configListener = new ZookeeperConfigListener(pathKey);
        return configListener;
    }

    public class ZookeeperConfigListener implements NodeCacheListener {

        private String pathKey;
        private Set<ConfigListener> listeners = new CopyOnWriteArraySet<>();
        private NodeCache nodeCache;

        public ZookeeperConfigListener(String pathKey) {
            this.pathKey = pathKey;
            this.nodeCache = new NodeCache(zkClient, pathKey);
            nodeCache.getListenable().addListener(this);
            try {
                nodeCache.start();
            } catch (Exception e) {
                LOGGER.error("Failed to add listener for path:{}", pathKey, e);
            }
        }

        public void addListener(ConfigListener configListener) {
            listeners.add(configListener);
        }

        @Override
        public void nodeChanged() throws Exception {
            ChildData childData = nodeCache.getCurrentData();
            String content = null;
            ConfigChangeType changeType;
            if (childData == null) {
                changeType = ConfigChangeType.DELETED;

            } else if (childData.getStat().getVersion() == 0) {
                content = new String(childData.getData(), RpcConstants.DEFAULT_CHARSET);
                changeType = ConfigChangeType.ADDED;
            } else {
                content = new String(childData.getData(), RpcConstants.DEFAULT_CHARSET);
                changeType = ConfigChangeType.MODIFIED;
            }
            ConfigChangedEvent configChangeEvent = new ConfigChangedEvent(pathKey, DynamicConfigKeys.DEFAULT_GROUP, (String) content, changeType);
            listeners.forEach(listener -> listener.process(configChangeEvent));

        }
    }



}