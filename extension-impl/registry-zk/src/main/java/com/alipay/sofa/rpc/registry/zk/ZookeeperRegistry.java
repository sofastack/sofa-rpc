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
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.AuthInfo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;
import static com.alipay.sofa.rpc.registry.zk.ZookeeperRegistryHelper.buildConfigPath;
import static com.alipay.sofa.rpc.registry.zk.ZookeeperRegistryHelper.buildConsumerPath;
import static com.alipay.sofa.rpc.registry.zk.ZookeeperRegistryHelper.buildOverridePath;
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
 *             |       |-configs （接口级配置）
 *             |       |     |--invoke.blacklist ["xxxx"]
 *             |       |     └--monitor.open ["true"]
 *             |       └overrides （IP级配置）
 *             |       |     └--bolt://192.168.3.100?xxx=yyy []
 *             |--com.alipay.sofa.rpc.example.EchoService （下一个服务）
 *             | ......
 *  </pre>
 * </p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension("zookeeper")
public class ZookeeperRegistry extends Registry {

    public static final String  EXT_NAME = "ZookeeperRegistry";

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER   = LoggerFactory.getLogger(ZookeeperRegistry.class);

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
    public final static String                          PARAM_PREFER_LOCAL_FILE = "preferLocalFile";

    /**
     * 配置项：是否使用临时节点。<br>
     * 如果使用临时节点：那么断开连接的时候，将zookeeper将自动消失。好处是如果服务端异常关闭，也不会有垃圾数据。<br>
     * 坏处是如果和zookeeper的网络闪断也通知客户端，客户端以为是服务端下线<br>
     * 如果使用永久节点：好处：网络闪断时不会影响服务端，而是由客户端进行自己判断长连接<br>
     * 坏处：服务端如果是异常关闭（无反注册），那么数据里就由垃圾节点，得由另外的哨兵程序进行判断
     */
    public final static String                          PARAM_CREATE_EPHEMERAL  = "createEphemeral";
    /**
     * 服务被下线
     */
    private final static byte[]                         PROVIDER_OFFLINE        = new byte[] { 0 };
    /**
     * 正常在线服务
     */
    private final static byte[]                         PROVIDER_ONLINE         = new byte[] { 1 };

    /**
     * Zookeeper zkClient
     */
    private CuratorFramework                            zkClient;

    /**
     * Root path of registry data
     */
    private String                                      rootPath;

    /**
     * Prefer get data from local file to remote zk cluster.
     *
     * @see ZookeeperRegistry#PARAM_PREFER_LOCAL_FILE
     */
    private boolean                                     preferLocalFile         = false;

    /**
     * Create EPHEMERAL node when true, otherwise PERSISTENT
     *
     * @see ZookeeperRegistry#PARAM_CREATE_EPHEMERAL
     * @see CreateMode#PERSISTENT
     * @see CreateMode#EPHEMERAL
     */
    private boolean                                     ephemeralNode           = true;

    /**
     * 接口级配置项观察者
     */
    private ZookeeperConfigObserver                     configObserver;

    /**
     * IP级配置项观察者
     */
    private ZookeeperOverrideObserver                   overrideObserver;

    /**
     * 服务列表观察者
     */
    private ZookeeperProviderObserver                   providerObserver;

    /**
     * 保存服务发布者的url
     */
    private ConcurrentMap<ProviderConfig, List<String>> providerUrls            = new ConcurrentHashMap<ProviderConfig, List<String>>();

    /**
     * 保存服务消费者的url
     */
    private ConcurrentMap<ConsumerConfig, String>       consumerUrls            = new ConcurrentHashMap<ConsumerConfig, String>();

    @Override
    public synchronized void init() {
        if (zkClient != null) {
            return;
        }
        String addressInput = registryConfig.getAddress(); // xxx:2181,yyy:2181/path1/paht2
        if (StringUtils.isEmpty(addressInput)) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_EMPTY_ADDRESS, EXT_NAME));
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
        CuratorFrameworkFactory.Builder zkClientuilder = CuratorFrameworkFactory.builder()
            .connectString(address)
            .sessionTimeoutMs(registryConfig.getConnectTimeout() * 3)
            .connectionTimeoutMs(registryConfig.getConnectTimeout())
            .canBeReadOnly(false)
            .retryPolicy(retryPolicy)
            .defaultData(null);

        //是否需要添加zk的认证信息
        List<AuthInfo> authInfos = buildAuthInfo();
        if (CommonUtils.isNotEmpty(authInfos)) {
            zkClientuilder = zkClientuilder.aclProvider(getDefaultAclProvider())
                .authorization(authInfos);
        }

        zkClient = zkClientuilder.build();

        zkClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("reconnect to zookeeper,recover provider and consumer data");
                }
                if (newState == ConnectionState.RECONNECTED) {
                    recoverRegistryData();
                }
            }
        });
    }

    //recover data when connect with zk again.

    protected void recoverRegistryData() {

        for (ProviderConfig providerConfig : providerUrls.keySet()) {
            registerProviderUrls(providerConfig);
        }

        for (ConsumerConfig consumerConfig : consumerUrls.keySet()) {
            subscribeConsumerUrls(consumerConfig);
        }

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
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_ZOOKEEPER_CLIENT_START), e);
        }
        return zkClient.getState() == CuratorFrameworkState.STARTED;
    }

    @Override
    public void destroy() {
        closePathChildrenCache(INTERFACE_CONFIG_CACHE);
        closePathChildrenCache(INTERFACE_OVERRIDE_CACHE);
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            zkClient.close();
        }
        providerUrls.clear();
        consumerUrls.clear();
    }

    @Override
    public void destroy(DestroyHook hook) {
        hook.preDestroy();
        destroy();
        hook.postDestroy();
    }

    /**
     * 接口配置{ConsumerConfig：PathChildrenCache} <br>
     * 例如：{ConsumerConfig ： PathChildrenCache }
     */
    private static final ConcurrentMap<ConsumerConfig, PathChildrenCache> INTERFACE_PROVIDER_CACHE = new ConcurrentHashMap<ConsumerConfig, PathChildrenCache>();

    /**
     * 接口配置{接口配置路径：PathChildrenCache} <br>
     * 例如：{/sofa-rpc/com.alipay.sofa.rpc.example/configs ： PathChildrenCache }
     */
    private static final ConcurrentMap<String, PathChildrenCache>         INTERFACE_CONFIG_CACHE   = new ConcurrentHashMap<String, PathChildrenCache>();

    /**
     * IP配置{接口配置路径：PathChildrenCache} <br>
     * 例如：{/sofa-rpc/com.alipay.sofa.rpc.example/overrides ： PathChildrenCache }
     */
    private static final ConcurrentMap<String, PathChildrenCache>         INTERFACE_OVERRIDE_CACHE = new ConcurrentHashMap<String, PathChildrenCache>();

    @Override
    public void register(ProviderConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isRegister()) {
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }

        //发布
        if (config.isRegister()) {
            registerProviderUrls(config);
        }

        if (config.isSubscribe()) {
            // 订阅配置节点
            if (!INTERFACE_CONFIG_CACHE.containsKey(buildConfigPath(rootPath, config))) {
                //订阅接口级配置
                subscribeConfig(config, config.getConfigListener());
            }
        }
    }

    /***
     * 注册 服务信息
     * @param config
     * @return
     * @throws Exception
     */
    protected void registerProviderUrls(ProviderConfig config) {
        String appName = config.getAppName();

        // 注册服务端节点
        try {
            // 避免重复计算
            List<String> urls;
            if (providerUrls.containsKey(config)) {
                urls = providerUrls.get(config);
            } else {
                urls = ZookeeperRegistryHelper.convertProviderToUrls(config);
                providerUrls.put(config, urls);
            }
            if (CommonUtils.isNotEmpty(urls)) {

                String providerPath = buildProviderPath(rootPath, config);
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName,
                        LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, providerPath));
                }
                for (String url : urls) {
                    url = URLEncoder.encode(url, "UTF-8");
                    String providerUrl = providerPath + CONTEXT_SEP + url;

                    try {
                        getAndCheckZkClient().create().creatingParentContainersIfNeeded()
                            .withMode(ephemeralNode ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT) // 是否永久节点
                            .forPath(providerUrl, config.isDynamic() ? PROVIDER_ONLINE : PROVIDER_OFFLINE); // 是否默认上下线
                        if (LOGGER.isInfoEnabled(appName)) {
                            LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB, providerUrl));
                        }
                    } catch (KeeperException.NodeExistsException nodeExistsException) {
                        if (LOGGER.isWarnEnabled(appName)) {
                            LOGGER.warnWithApp(appName,
                                "provider has exists in zookeeper, provider=" + providerUrl);
                        }
                    }
                }

                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName,
                        LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, providerPath));
                }

            }
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_REG_PROVIDER, "zookeeperRegistry",
                config.buildKey()), e);
        }
        if (EventBus.isEnable(ProviderPubEvent.class)) {
            ProviderPubEvent event = new ProviderPubEvent(config);
            EventBus.post(event);
        }
    }

    /**
     * 订阅接口级配置
     *
     * @param config   provider/consumer config
     * @param listener config listener
     */
    protected void subscribeConfig(final AbstractInterfaceConfig config, ConfigListener listener) {
        try {
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
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_SUB_PROVIDER_CONFIG, EXT_NAME), e);
        }
    }

    /**
     * 订阅IP级配置（服务发布暂时不支持动态配置,暂时支持订阅ConsumerConfig参数设置）
     *
     * @param config   consumer config
     * @param listener config listener
     */
    protected void subscribeOverride(final ConsumerConfig config, ConfigListener listener) {
        try {
            if (overrideObserver == null) { // 初始化
                overrideObserver = new ZookeeperOverrideObserver();
            }
            overrideObserver.addConfigListener(config, listener);
            final String overridePath = buildOverridePath(rootPath, config);
            final AbstractInterfaceConfig registerConfig = getRegisterConfig(config);
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
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_SUB_PROVIDER_OVERRIDE, EXT_NAME), e);
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
        if (config.isRegister()) {
            try {
                List<String> urls = providerUrls.remove(config);
                if (CommonUtils.isNotEmpty(urls)) {
                    String providerPath = buildProviderPath(rootPath, config);
                    for (String url : urls) {
                        url = URLEncoder.encode(url, "UTF-8");
                        getAndCheckZkClient().delete().forPath(providerPath + CONTEXT_SEP + url);
                    }
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB,
                            providerPath, "1"));
                    }
                }
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_UNREG_PROVIDER, EXT_NAME), e);
                }
            }
        }
        // 反订阅配置节点
        if (config.isSubscribe()) {
            try {
                if (null != configObserver) {
                    configObserver.removeConfigListener(config);
                }
                if (null != overrideObserver) {
                    overrideObserver.removeConfigListener(config);
                }
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_UNSUB_PROVIDER_CONFIG, EXT_NAME),
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
        String appName = config.getAppName();
        if (!registryConfig.isSubscribe()) {
            // 注册中心不订阅
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return null;
        }

        //订阅如果有必要
        subscribeConsumerUrls(config);

        if (config.isSubscribe()) {

            List<ProviderInfo> matchProviders;
            // 订阅配置
            if (!INTERFACE_CONFIG_CACHE.containsKey(buildConfigPath(rootPath, config))) {
                //订阅接口级配置
                subscribeConfig(config, config.getConfigListener());
            }
            if (!INTERFACE_OVERRIDE_CACHE.containsKey(buildOverridePath(rootPath, config))) {
                //订阅IP级配置
                subscribeOverride(config, config.getConfigListener());
            }

            // 订阅Providers节点
            try {
                if (providerObserver == null) { // 初始化
                    providerObserver = new ZookeeperProviderObserver();
                }
                final String providerPath = buildProviderPath(rootPath, config);
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_SUB, providerPath));
                }
                PathChildrenCache pathChildrenCache = INTERFACE_PROVIDER_CACHE.get(config);
                if (pathChildrenCache == null) {
                    // 监听配置节点下 子节点增加、子节点删除、子节点Data修改事件
                    ProviderInfoListener providerInfoListener = config.getProviderInfoListener();
                    providerObserver.addProviderListener(config, providerInfoListener);
                    // TODO 换成监听父节点变化（只是监听变化了，而不通知变化了什么，然后客户端自己来拉数据的）
                    pathChildrenCache = new PathChildrenCache(zkClient, providerPath, true);
                    final PathChildrenCache finalPathChildrenCache = pathChildrenCache;
                    pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                        @Override
                        public void childEvent(CuratorFramework client1, PathChildrenCacheEvent event) throws Exception {
                            if (LOGGER.isDebugEnabled(config.getAppName())) {
                                LOGGER.debugWithApp(config.getAppName(),
                                    "Receive zookeeper event: " + "type=[" + event.getType() + "]");
                            }
                            switch (event.getType()) {
                                case CHILD_ADDED: //加了一个provider
                                    providerObserver.addProvider(config, providerPath, event.getData(),
                                        finalPathChildrenCache.getCurrentData());
                                    break;
                                case CHILD_REMOVED: //删了一个provider
                                    providerObserver.removeProvider(config, providerPath, event.getData(),
                                        finalPathChildrenCache.getCurrentData());
                                    break;
                                case CHILD_UPDATED: // 更新一个Provider
                                    providerObserver.updateProvider(config, providerPath, event.getData(),
                                        finalPathChildrenCache.getCurrentData());
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                    pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
                    INTERFACE_PROVIDER_CACHE.put(config, pathChildrenCache);
                }
                List<ProviderInfo> providerInfos = ZookeeperRegistryHelper.convertUrlsToProviders(
                    providerPath, pathChildrenCache.getCurrentData());
                matchProviders = ZookeeperRegistryHelper.matchProviderInfos(config, providerInfos);
            } catch (Exception e) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_SUB_PROVIDER, EXT_NAME), e);
            }

            if (EventBus.isEnable(ConsumerSubEvent.class)) {
                ConsumerSubEvent event = new ConsumerSubEvent(config);
                EventBus.post(event);
            }

            return Collections.singletonList(new ProviderGroup().addAll(matchProviders));

        }
        return null;
    }

    /***
     * 订阅
     * @param config
     */
    protected void subscribeConsumerUrls(ConsumerConfig config) {
        // 注册Consumer节点
        String url = null;
        if (config.isRegister()) {
            try {
                String consumerPath = buildConsumerPath(rootPath, config);
                if (consumerUrls.containsKey(config)) {
                    url = consumerUrls.get(config);
                } else {
                    url = ZookeeperRegistryHelper.convertConsumerToUrl(config);
                    consumerUrls.put(config, url);
                }
                String encodeUrl = URLEncoder.encode(url, "UTF-8");
                getAndCheckZkClient().create().creatingParentContainersIfNeeded()
                    .withMode(CreateMode.EPHEMERAL) // Consumer临时节点
                    .forPath(consumerPath + CONTEXT_SEP + encodeUrl);

            } catch (KeeperException.NodeExistsException nodeExistsException) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("consumer has exists in zookeeper, consumer=" + url);
                }
            } catch (SofaRpcRuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_REG_CONSUMER_CONFIG, EXT_NAME), e);
            }
        }
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        // 反注册服务端节点
        if (config.isRegister()) {
            try {
                String url = consumerUrls.remove(config);
                if (url != null) {
                    String consumerPath = buildConsumerPath(rootPath, config);
                    url = URLEncoder.encode(url, "UTF-8");
                    getAndCheckZkClient().delete().forPath(consumerPath + CONTEXT_SEP + url);
                }
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_UNREG_CONSUMER_CONFIG, EXT_NAME),
                        e);
                }
            }
        }
        // 反订阅配置节点
        if (config.isSubscribe()) {
            try {
                providerObserver.removeProviderListener(config);
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_UNSUB_PROVIDER_CONFIG, EXT_NAME),
                        e);
                }
            }
            try {
                configObserver.removeConfigListener(config);
            } catch (Exception e) {
                if (!RpcRunningState.isShuttingDown()) {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_UNSUB_CONSUMER_CONFIG, EXT_NAME),
                        e);
                }
            }
            PathChildrenCache childrenCache = INTERFACE_PROVIDER_CACHE.remove(config);
            if (childrenCache != null) {
                try {
                    childrenCache.close();
                } catch (Exception e) {
                    if (!RpcRunningState.isShuttingDown()) {
                        throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_UNSUB_CONSUMER_CONFIG,
                            EXT_NAME), e);
                    }
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
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_ZOOKEEPER_CLIENT_UNAVAILABLE));
        }
        return zkClient;
    }

    /**
     * 获取注册配置
     *
     * @param config consumer config
     * @return
     */
    private AbstractInterfaceConfig getRegisterConfig(ConsumerConfig config) {
        String url = ZookeeperRegistryHelper.convertConsumerToUrl(config);
        String addr = url.substring(0, url.indexOf("?"));
        for (Map.Entry<ConsumerConfig, String> consumerUrl : consumerUrls.entrySet()) {
            if (consumerUrl.getValue().contains(addr)) {
                return consumerUrl.getKey();
            }
        }
        return null;
    }

    private void closePathChildrenCache(Map<String, PathChildrenCache> map) {
        for (Map.Entry<String, PathChildrenCache> entry : map.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                LOGGER.error(LogCodes.getLog(LogCodes.ERROR_CLOSE_PATH_CACHE), e);
            }
        }
    }

    /**
     * 获取默认的AclProvider
     * @return
     */
    private ACLProvider getDefaultAclProvider() {
        return new ACLProvider() {
            @Override
            public List<ACL> getDefaultAcl() {
                return ZooDefs.Ids.CREATOR_ALL_ACL;
            }

            @Override
            public List<ACL> getAclForPath(String path) {
                return ZooDefs.Ids.CREATOR_ALL_ACL;
            }
        };
    }

    /**
     * 创建认证信息
     * @return
     */
    private List<AuthInfo> buildAuthInfo() {
        List<AuthInfo> info = new ArrayList<AuthInfo>();

        String scheme = registryConfig.getParameter("scheme");

        //如果存在多个认证信息，则在参数形式为为addAuth=user1:paasswd1,user2:passwd2
        String addAuth = registryConfig.getParameter("addAuth");

        if (StringUtils.isNotEmpty(addAuth)) {
            String[] addAuths = addAuth.split(",");
            for (String singleAuthInfo : addAuths) {
                info.add(new AuthInfo(scheme, singleAuthInfo.getBytes()));
            }
        }

        return info;
    }
}