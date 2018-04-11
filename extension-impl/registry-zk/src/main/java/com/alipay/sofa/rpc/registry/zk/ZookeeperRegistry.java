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

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;
import static com.alipay.sofa.rpc.registry.zk.ZookeeperRegistryHelper.buildConfigPath;
import static com.alipay.sofa.rpc.registry.zk.ZookeeperRegistryHelper.buildConsumerPath;
import static com.alipay.sofa.rpc.registry.zk.ZookeeperRegistryHelper.buildProviderPath;

/**
 * <p>简单的Zookeeper注册中心,具有如下特性：<br>
 * 1.可以设置优先读取远程，还是优先读取本地备份文件<br>
 * 2.如果zk不可用，自动读取本地备份文件<br>
 * 3.可以设置使用临时节点还是永久节点<br>
 * 4.断线了会自动重连，并且自动recover数据<br><br>
 * <pre>
 *  在zookeeper上存放的数据结构为：
 *  -$rootPath (根路径)
 *         └--sofa-rpc
 *             |--com.alipay.sofa.rpc.example.HelloService （服务）
 *             |       |-providers （服务提供者列表）
 *             |       |     |--bolt://192.168.1.100:22000?xxx=yyy [1]
 *             |       |     |--bolt://192.168.1.110:22000?xxx=yyy [1]
 *             |       |     └--bolt://192.168.1.120?xxx=yyy [1]
 *             |       |-consumers （服务调用者列表）
 *             |       |     |--bolt://192.168.3.100?xxx=yyy []
 *             |       |     |--bolt://192.168.3.110?xxx=yyy []
 *             |       |     └--bolt://192.168.3.120?xxx=yyy []
 *             |       └-configs (接口级配置）
 *             |            |--invoke.blacklist ["xxxx"]
 *             |            └--monitor.open ["true"]
 *             |--com.alipay.sofa.rpc.example.EchoService （下一个服务）
 *             | ......
 *  </pre>
 * </p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension("zookeeper")
public class ZookeeperRegistry extends Registry {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegistry.class);

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected ZookeeperRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    /**
     * 配置项：是否本地优先
     */
    public final static String        PARAM_PREFER_LOCAL_FILE = "preferLocalFile";

    /**
     * 配置项：是否使用临时节点。<br>
     * 如果使用临时节点：那么断开连接的时候，将zookeeper将自动消失。好处是如果服务端异常关闭，也不会有垃圾数据。<br>
     * 坏处是如果和zookeeper的网络闪断也通知客户端，客户端以为是服务端下线<br>
     * 如果使用永久节点：好处：网络闪断时不会影响服务端，而是由客户端进行自己判断长连接<br>
     * 坏处：服务端如果是异常关闭（无反注册），那么数据里就由垃圾节点，得由另外的哨兵程序进行判断
     */
    public final static String        PARAM_CREATE_EPHEMERAL  = "createEphemeral";
    /**
     * 服务被下线
     */
    private final static byte[]       PROVIDER_OFFLINE        = new byte[] { 0 };
    /**
     * 正常在线服务
     */
    private final static byte[]       PROVIDER_ONLINE         = new byte[] { 1 };

    /**
     * Zookeeper zkClient
     */
    private CuratorFramework          zkClient;

    /**
     * Root path of registry data
     */
    private String                    rootPath;

    /**
     * Prefer get data from local file to remote zk cluster.
     *
     * @see ZookeeperRegistry#PARAM_PREFER_LOCAL_FILE
     */
    private boolean                   preferLocalFile         = false;

    /**
     * Create EPHEMERAL node when true, otherwise PERSISTENT
     *
     * @see ZookeeperRegistry#PARAM_CREATE_EPHEMERAL
     * @see CreateMode#PERSISTENT
     * @see CreateMode#EPHEMERAL
     */
    private boolean                   ephemeralNode           = true;

    /**
     * 配置项观察者
     */
    private ZookeeperConfigObserver   configObserver;

    /**
     * 配置项观察者
     */
    private ZookeeperProviderObserver providerObserver;

    @Override
    public synchronized void init() {
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
        preferLocalFile = !CommonUtils.isFalse(registryConfig.getParameter(PARAM_PREFER_LOCAL_FILE));
        ephemeralNode = !CommonUtils.isFalse(registryConfig.getParameter(PARAM_CREATE_EPHEMERAL));
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
    }

    @Override
    public synchronized boolean start() {
        if (zkClient == null) {
            LOGGER.warn("Start zookeeper registry must be do init first!");
            return false;
        }
        if (zkClient.getState() == CuratorFrameworkState.STARTED) {
            return true;
        }
        try {
            zkClient.start();
        } catch (Exception e) {
            throw new SofaRpcRuntimeException("Failed to start zookeeper zkClient", e);
        }
        return zkClient.getState() == CuratorFrameworkState.STARTED;
    }

    @Override
    public void destroy() {
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            zkClient.close();
        }
    }

    @Override
    public void destroy(DestroyHook hook) {
        hook.postDestroy();
        destroy();
        hook.postDestroy();
    }

    /**
     * 接口配置{接口配置路径：PathChildrenCache} <br>
     * 例如：{/sofa-rpc/com.alipay.sofa.rpc.example/configs ： PathChildrenCache }
     */
    private static final ConcurrentHashMap<String, PathChildrenCache> INTERFACE_CONFIG_CACHE = new ConcurrentHashMap<String, PathChildrenCache>();

    @Override
    public void register(ProviderConfig config) {
        if (config.isRegister()) {
            // 注册服务端节点
            try {
                List<String> urls = ZookeeperRegistryHelper.convertProviderToUrls(config);
                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = buildProviderPath(rootPath, config);
                    for (String url : urls) {
                        url = URLEncoder.encode(url, "UTF-8");
                        getAndCheckZkClient().create().creatingParentContainersIfNeeded()
                            .withMode(ephemeralNode ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT) // 是否永久节点
                            .forPath(providerPath + CONTEXT_SEP + url,
                                config.isDynamic() ? PROVIDER_ONLINE : PROVIDER_OFFLINE); // 是否默认上下线
                    }
                }
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to register provider to zookeeperRegistry!", e);
            }
        }

        if (config.isSubscribe()) {
            // 订阅配置节点
            ConfigListener listener = config.getConfigListener();
            String configPath = buildConfigPath(rootPath, config);
            if (!INTERFACE_CONFIG_CACHE.containsKey(configPath)) {
                subscribeConfig(config, listener);
            }
        }
    }

    protected void subscribeConfig(final AbstractInterfaceConfig config, ConfigListener listener) {
        String configPath = buildConfigPath(rootPath, config);
        try {
            if (configObserver == null) { // 初始化
                configObserver = new ZookeeperConfigObserver();
            }
            configObserver.addConfigListener(config, listener);
            // 监听配置节点下 子节点增加、子节点删除、子节点Data修改事件
            PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, configPath, true);
            pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client1, PathChildrenCacheEvent event) throws Exception {
                    if (LOGGER.isDebugEnabled(config.getAppName())) {
                        LOGGER.debug("Receive zookeeper event: " + "type=[" + event.getType() + "]");
                    }
                    switch (event.getType()) {
                        case CHILD_ADDED: //加了一个配置
                            configObserver.addConfig(config, event.getData());
                            break;
                        case CHILD_REMOVED: //删了一个配置
                            configObserver.removeConfig(config, event.getData());
                            break;
                        case CHILD_UPDATED:
                            configObserver.updateConfig(config, event.getData());
                            break;
                        default:
                            break;
                    }
                }
            });
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            INTERFACE_CONFIG_CACHE.put(configPath, pathChildrenCache);
            configObserver.updateConfigAll(config, pathChildrenCache.getCurrentData());
        } catch (Exception e) {
            throw new SofaRpcRuntimeException("Failed to subscribe provider config from zookeeperRegistry!", e);
        }
    }

    @Override
    public void unRegister(ProviderConfig config) {
        // 反注册服务端节点
        if (config.isRegister()) {
            try {
                List<String> urls = ZookeeperRegistryHelper.convertProviderToUrls(config);
                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = buildProviderPath(rootPath, config);
                    for (String url : urls) {
                        url = URLEncoder.encode(url, "UTF-8");
                        getAndCheckZkClient().delete().forPath(providerPath + CONTEXT_SEP + url);
                    }
                }
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException("Failed to unregister provider to zookeeperRegistry!", e);
                }
            }
        }
        // 反订阅配置节点
        if (config.isSubscribe()) {
            try {
                configObserver.removeConfigListener(config);
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException("Failed to unsubscribe provider config from zookeeperRegistry!",
                        e);
                }
            }
        }
    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        // 一个一个来，后续看看要不要使用curator的事务
        for (ProviderConfig config : configs) {
            unRegister(config);
        }
    }

    @Override
    public List<ProviderGroup> subscribe(final ConsumerConfig config) {
        // 注册Consumer节点
        if (config.isRegister()) {
            try {
                String consumerPath = buildConsumerPath(rootPath, config);
                String url = ZookeeperRegistryHelper.convertConsumerToUrl(config);
                url = URLEncoder.encode(url, "UTF-8");
                getAndCheckZkClient().create().creatingParentContainersIfNeeded()
                    .withMode(CreateMode.EPHEMERAL) // Consumer临时节点
                    .forPath(consumerPath + CONTEXT_SEP + url);
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to register consumer to zookeeperRegistry!", e);
            }
        }
        if (config.isSubscribe()) {
            // 订阅配置
            final String configPath = buildConfigPath(rootPath, config);
            if (!INTERFACE_CONFIG_CACHE.containsKey(configPath)) {
                subscribeConfig(config, config.getConfigListener());
            }

            // 订阅Providers节点
            try {
                if (providerObserver == null) { // 初始化
                    providerObserver = new ZookeeperProviderObserver();
                }
                final String providerPath = buildProviderPath(rootPath, config);

                // 监听配置节点下 子节点增加、子节点删除、子节点Data修改事件
                ProviderInfoListener providerInfoListener = config.getProviderInfoListener();
                providerObserver.addProviderListener(config, providerInfoListener);
                // TODO 换成监听父节点变化（只是监听变化了，而不通知变化了什么，然后客户端自己来拉数据的）
                PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, providerPath, true);
                pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                    @Override
                    public void childEvent(CuratorFramework client1, PathChildrenCacheEvent event) throws Exception {
                        if (LOGGER.isDebugEnabled(config.getAppName())) {
                            LOGGER.debugWithApp(config.getAppName(),
                                "Receive zookeeper event: " + "type=[" + event.getType() + "]");
                        }
                        switch (event.getType()) {
                            case CHILD_ADDED: //加了一个provider
                                providerObserver.addProvider(config, providerPath, event.getData());
                                break;
                            case CHILD_REMOVED: //删了一个provider
                                providerObserver.removeProvider(config, providerPath, event.getData());
                                break;
                            case CHILD_UPDATED: // 更新一个Provider
                                providerObserver.updateProvider(config, providerPath, event.getData());
                                break;
                            default:
                                break;
                        }
                    }
                });
                pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
                List<ProviderInfo> providerInfos = ZookeeperRegistryHelper.convertUrlsToProviders(
                    providerPath, pathChildrenCache.getCurrentData());

                return Collections.singletonList(new ProviderGroup().addAll(providerInfos));
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to subscribe provider from zookeeperRegistry!", e);
            }
        }
        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        // 反注册服务端节点
        if (config.isRegister()) {
            try {
                String consumerPath = buildConsumerPath(rootPath, config);
                String url = ZookeeperRegistryHelper.convertConsumerToUrl(config);
                url = URLEncoder.encode(url, "UTF-8");
                getAndCheckZkClient().delete().forPath(consumerPath + CONTEXT_SEP + url);
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException("Failed to unregister consumer to zookeeperRegistry!", e);
                }
            }
        }
        // 反订阅配置节点
        if (config.isSubscribe()) {
            try {
                providerObserver.removeProviderListener(config);
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException("Failed to unsubscribe provider from zookeeperRegistry!", e);
                }
            }
            try {
                configObserver.removeConfigListener(config);
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException("Failed to unsubscribe consumer config from zookeeperRegistry!",
                        e);
                }
            }
        }
    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {
        // 一个一个来，后续看看要不要使用curator的事务
        for (ConsumerConfig config : configs) {
            unSubscribe(config);
        }
    }

    protected CuratorFramework getZkClient() {
        return zkClient;
    }

    private CuratorFramework getAndCheckZkClient() {
        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            throw new SofaRpcRuntimeException("Zookeeper client is not available");
        }
        return zkClient;
    }
}
