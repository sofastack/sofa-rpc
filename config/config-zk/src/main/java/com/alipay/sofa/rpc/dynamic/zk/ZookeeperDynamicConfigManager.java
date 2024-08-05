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
    private ConcurrentMap<String, PathChildrenCache> cacheMap          = new ConcurrentHashMap<String, PathChildrenCache>();

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
    }

    @Override
    public void initServiceConfiguration(String service) {
        //TODO not now
    }

    @Override
    public String getProviderServiceProperty(String service, String key) {
        try {
            String path = rootPath + CONTEXT_SEP + DynamicConfigKeyHelper.buildProviderServiceProKey(service, key);
            byte[] bytes = getCachedData(path);
            return bytes != null ? new String(bytes) : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (Exception e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getConsumerServiceProperty(String service, String key) {
        try {
            String path = rootPath + CONTEXT_SEP + DynamicConfigKeyHelper.buildConsumerServiceProKey(service, key);
            byte[] bytes = getCachedData(path);
            return bytes != null ? new String(bytes) : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (Exception e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getProviderMethodProperty(String service, String method, String key) {
        try {
            String path = rootPath + CONTEXT_SEP +
                DynamicConfigKeyHelper.buildProviderMethodProKey(service, method, key);
            byte[] bytes = getCachedData(path);
            return bytes != null ? new String(bytes) : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (Exception e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }
    }

    @Override
    public String getConsumerMethodProperty(String service, String method, String key) {
        try {
            String path = rootPath + CONTEXT_SEP +
                DynamicConfigKeyHelper.buildConsumerMethodProKey(service, method, key);
            byte[] bytes = getCachedData(path);
            return bytes != null ? new String(bytes) : DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        } catch (Exception e) {
            return DynamicHelper.DEFAULT_DYNAMIC_VALUE;
        }

    }

    @Override
    public AuthRuleGroup getServiceAuthRule(String service) {
        //TODO 暂不支持
        return null;
    }

    private void setupPathChildrenCache() {
        PathChildrenCache cache = new PathChildrenCache(zkClient, rootPath, true);
        try {
            cache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                    LOGGER.info("Receive zookeeper event: " + "type=[" + event.getType() + "]");
                }
            });
            cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            cacheMap.put(rootPath, cache);
        } catch (Exception e) {
            LOGGER.error("setupPathChildrenCache error", e);
        }
    }

    private byte[] getCachedData(String path) throws Exception {
        PathChildrenCache cache = cacheMap.get(rootPath);
        if (cache == null || cache.getCurrentData() == null) {
            setupPathChildrenCache();
            cache = cacheMap.get(rootPath);
        }
        return cache.getCurrentData(path).getData();
    }

}