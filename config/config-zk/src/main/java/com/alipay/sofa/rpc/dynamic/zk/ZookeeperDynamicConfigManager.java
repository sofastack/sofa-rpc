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
import com.alipay.sofa.rpc.common.config.RpcConfigKeys;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.dynamic.DynamicConfigKeyHelper;
import com.alipay.sofa.rpc.dynamic.DynamicConfigManager;
import com.alipay.sofa.rpc.dynamic.DynamicHelper;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;

/**
 * @author Narziss
 * @version ZookeeperDynamicConfigManager.java, v 0.1 2024年07月20日 09:23 Narziss
 */

@Extension(value = "zookeeper", override = true)
public class ZookeeperDynamicConfigManager extends DynamicConfigManager {

    private final static Logger                      LOGGER            = LoggerFactory
                                                                           .getLogger(ZookeeperDynamicConfigManager.class);
    private final CuratorFramework                   zkClient;
    private static final String                      ADDRESS           = SofaConfigs
                                                                           .getOrDefault(RpcConfigKeys.ZK_ADDRESS);
    private static final String                      DEFAULT_NAMESPACE = "sofa-rpc";
    private static final String                      CONFIG_NODE       = "config";
    private static final String                      DEFAULT_APP       = "sofa-rpc";
    private final String                             appName;
    private final String                             rootPath;
    private ConcurrentMap<String, String> configMap = new ConcurrentHashMap<>();

    protected ZookeeperDynamicConfigManager(String appName) {
        super(appName);
        if (StringUtils.isEmpty(appName)) {
            this.appName = DEFAULT_APP;
        } else {
            this.appName = appName;
        }
        rootPath = CONTEXT_SEP + CONFIG_NODE + CONTEXT_SEP + appName;
        zkClient = CuratorFrameworkFactory.builder()
            .connectString(ADDRESS)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .namespace(DEFAULT_NAMESPACE)
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
}