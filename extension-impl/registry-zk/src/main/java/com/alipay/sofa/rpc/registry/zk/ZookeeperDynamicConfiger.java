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
package com.alipay.sofa.rpc.registry.zk;

import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.dynamic.DynamicConfiger;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;
import static com.alipay.sofa.rpc.registry.utils.RegistryUtils.buildConfigPath;
import static com.alipay.sofa.rpc.registry.zk.ZookeeperRegistryHelper.buildOverridePath;

/**
 * @author bystander
 * @version $Id: ZookeeperDynamicConfiger.java, v 0.1 2018年12月26日 20:06 bystander Exp $
 */
@Extension("zookeeper")
public class ZookeeperDynamicConfiger implements DynamicConfiger {

    /**
     * Root path of registry data
     */
    private String                                                rootPath;

    /**
     * slf4j Logger for this class
     */
    private final static Logger                                   LOGGER                   = LoggerFactory
                                                                                               .getLogger(ZookeeperRegistry.class);

    /**
     * 接口级配置项观察者
     */
    private ZookeeperConfigObserver                               configObserver;

    /**
     * IP级配置项观察者
     */
    private ZookeeperOverrideObserver                             overrideObserver;

    /**
     * Zookeeper zkClient
     */
    private CuratorFramework                                      zkClient;

    /**
     * 接口配置{接口配置路径：PathChildrenCache} <br>
     * 例如：{/sofa-rpc/com.alipay.sofa.rpc.example/configs ： PathChildrenCache }
     */
    private static final ConcurrentMap<String, PathChildrenCache> INTERFACE_CONFIG_CACHE   = new ConcurrentHashMap<String, PathChildrenCache>();

    /**
     * IP配置{接口配置路径：PathChildrenCache} <br>
     * 例如：{/sofa-rpc/com.alipay.sofa.rpc.example/overrides ： PathChildrenCache }
     */
    private static final ConcurrentMap<String, PathChildrenCache> INTERFACE_OVERRIDE_CACHE = new ConcurrentHashMap<String, PathChildrenCache>();

    /**
     * 配置项：是否本地优先
     */
    public final static String                                    PARAM_PREFER_LOCAL_FILE  = "preferLocalFile";

    /**
     * 配置项：是否使用临时节点。<br>
     * 如果使用临时节点：那么断开连接的时候，将zookeeper将自动消失。好处是如果服务端异常关闭，也不会有垃圾数据。<br>
     * 坏处是如果和zookeeper的网络闪断也通知客户端，客户端以为是服务端下线<br>
     * 如果使用永久节点：好处：网络闪断时不会影响服务端，而是由客户端进行自己判断长连接<br>
     * 坏处：服务端如果是异常关闭（无反注册），那么数据里就由垃圾节点，得由另外的哨兵程序进行判断
     */
    public final static String                                    PARAM_CREATE_EPHEMERAL   = "createEphemeral";

    public ZookeeperDynamicConfiger(RegistryConfig registryConfig) {
        if (zkClient != null) {
            return;
        }
        String addressInput = registryConfig.getAddress(); // xxx:2181,yyy:2181/path1/paht2
        if (StringUtils.isEmpty(addressInput)) {
            throw new SofaRpcRuntimeException("Address of zookeeper registry is empty.");
        }
        int idx = addressInput.indexOf(CONTEXT_SEP);
        String address; // IP地址
        if (idx > 0) {
            address = addressInput.substring(0, idx);
            rootPath = addressInput.substring(idx);
            if (!rootPath.endsWith(CONTEXT_SEP)) {
                rootPath += CONTEXT_SEP; // 保证以"/"结尾
            }
        } else {
            address = addressInput;
            rootPath = CONTEXT_SEP;
        }
        boolean preferLocalFile = !CommonUtils.isFalse(registryConfig.getParameter(PARAM_PREFER_LOCAL_FILE));
        boolean ephemeralNode = !CommonUtils.isFalse(registryConfig.getParameter(PARAM_CREATE_EPHEMERAL));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "Init ZookeeperRegistry with address {}, root path is {}. preferLocalFile:{}, ephemeralNode:{}",
                address, rootPath, preferLocalFile, ephemeralNode);
        }
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        zkClient = CuratorFrameworkFactory.builder()
            .connectString(address)
            .sessionTimeoutMs(registryConfig.getConnectTimeout() * 3)
            .connectionTimeoutMs(registryConfig.getConnectTimeout())
            .canBeReadOnly(false)
            .retryPolicy(retryPolicy)
            .defaultData(null)
            .build();

        zkClient.start();

    }

    /**
     * 订阅接口级配置
     *
     * @param config   provider/consumer config
     * @param listener config listener
     */
    @Override
    public void subscribeInterfaceConfig(final AbstractInterfaceConfig config, ConfigListener listener) {
        try {

            if (INTERFACE_CONFIG_CACHE.containsKey(buildConfigPath(rootPath, config))) {
                return;
            }

            if (configObserver == null) { // 初始化
                configObserver = new ZookeeperConfigObserver();
            }

            configObserver.addConfigListener(config, listener);
            final String configPath = buildConfigPath(rootPath, config);
            // 监听配置节点下 子节点增加、子节点删除、子节点Data修改事件
            PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, configPath, true);
            pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client1, PathChildrenCacheEvent event) throws Exception {
                    if (LOGGER.isDebugEnabled(config.getAppName())) {
                        LOGGER.debug("Receive zookeeper event: " + "type=[" + event.getType() + "]");
                    }
                    switch (event.getType()) {
                        case CHILD_ADDED: //新增接口级配置
                            configObserver.addConfig(config, configPath, event.getData());
                            break;
                        case CHILD_REMOVED: //删除接口级配置
                            configObserver.removeConfig(config, configPath, event.getData());
                            break;
                        case CHILD_UPDATED:// 更新接口级配置
                            configObserver.updateConfig(config, configPath, event.getData());
                            break;
                        default:
                            break;
                    }
                }
            });
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            INTERFACE_CONFIG_CACHE.put(configPath, pathChildrenCache);
            configObserver.updateConfigAll(config, configPath, pathChildrenCache.getCurrentData());
        } catch (Exception e) {
            throw new SofaRpcRuntimeException("Failed to subscribe provider config from zookeeperRegistry!", e);
        }
    }

    @Override
    public void unSubscribeConfig(final AbstractInterfaceConfig config) {
        try {
            if (null != configObserver) {
                configObserver.removeConfigListener(config);
            }

            //FIXME maybe we also need unsub config consumer override
            if (config instanceof ProviderConfig) {
                if (null != overrideObserver) {
                    overrideObserver.removeConfigListener(config);
                }
            }
        } catch (Exception e) {
            if (!RpcRunningState.isShuttingDown()) {
                throw new SofaRpcRuntimeException("Failed to unsubscribe provider config from zookeeperRegistry!",
                    e);
            }
        }
    }

    /**
     * 订阅IP级配置（服务发布暂时不支持动态配置,暂时支持订阅ConsumerConfig参数设置）
     *
     * @param config   consumer config
     * @param listener config listener
     */
    @Override
    public void subscribeOverride(ConcurrentMap<ConsumerConfig, String> consumerUrls, final ConsumerConfig config,
                                  ConfigListener listener) {
        try {

            if (INTERFACE_OVERRIDE_CACHE.containsKey(buildOverridePath(rootPath, config))) {
                //订阅IP级配置
                return;
            }

            if (overrideObserver == null) { // 初始化
                overrideObserver = new ZookeeperOverrideObserver();
            }
            overrideObserver.addConfigListener(config, listener);
            final String overridePath = buildOverridePath(rootPath, config);
            final AbstractInterfaceConfig registerConfig = getRegisterConfig(consumerUrls, config);
            // 监听配置节点下 子节点增加、子节点删除、子节点Data修改事件
            PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, overridePath, true);
            pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client1, PathChildrenCacheEvent event) throws Exception {
                    if (LOGGER.isDebugEnabled(config.getAppName())) {
                        LOGGER.debug("Receive zookeeper event: " + "type=[" + event.getType() + "]");
                    }
                    switch (event.getType()) {
                        case CHILD_ADDED: //新增IP级配置
                            overrideObserver.addConfig(config, overridePath, event.getData());
                            break;
                        case CHILD_REMOVED: //删除IP级配置
                            overrideObserver.removeConfig(config, overridePath, event.getData(), registerConfig);
                            break;
                        case CHILD_UPDATED:// 更新IP级配置
                            overrideObserver.updateConfig(config, overridePath, event.getData());
                            break;
                        default:
                            break;
                    }
                }
            });
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            INTERFACE_OVERRIDE_CACHE.put(overridePath, pathChildrenCache);
            overrideObserver.updateConfigAll(config, overridePath, pathChildrenCache.getCurrentData());
        } catch (Exception e) {
            throw new SofaRpcRuntimeException("Failed to subscribe provider config from zookeeperRegistry!", e);
        }
    }

    @Override
    public void clearConfigCache() {

        for (Map.Entry<String, PathChildrenCache> entry : INTERFACE_CONFIG_CACHE.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                LOGGER.error("Close PathChildrenCache error!", e);
            }
        }

        for (Map.Entry<String, PathChildrenCache> entry : INTERFACE_OVERRIDE_CACHE.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                LOGGER.error("Close PathChildrenCache error!", e);
            }
        }
    }

    /**
     * 获取注册配置
     *
     * @param config consumer config
     * @return
     */
    private AbstractInterfaceConfig getRegisterConfig(ConcurrentMap<ConsumerConfig, String> consumerUrls,
                                                      ConsumerConfig config) {
        String url = ZookeeperRegistryHelper.convertConsumerToUrl(config);
        String addr = url.substring(0, url.indexOf("?"));
        for (Map.Entry<ConsumerConfig, String> consumerUrl : consumerUrls.entrySet()) {
            if (consumerUrl.getValue().contains(addr)) {
                return consumerUrl.getKey();
            }
        }
        return null;
    }

}