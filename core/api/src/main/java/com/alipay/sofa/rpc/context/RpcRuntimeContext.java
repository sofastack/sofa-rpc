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
package com.alipay.sofa.rpc.context;

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.bootstrap.ProviderBootstrap;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.Version;
import com.alipay.sofa.rpc.common.cache.RpcCacheManager;
import com.alipay.sofa.rpc.common.struct.ConcurrentHashSet;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.module.ModuleFactory;
import com.alipay.sofa.rpc.registry.Registry;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.server.ServerFactory;
import com.alipay.sofa.rpc.transport.ClientTransportFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 全局的运行时上下文
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class RpcRuntimeContext {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                               LOGGER                    = LoggerFactory
                                                                                            .getLogger(RpcRuntimeContext.class);

    /**
     * 上下文信息，例如instancekey，本机ip等信息
     */
    private final static ConcurrentHashMap                    CONTEXT                   = new ConcurrentHashMap();

    /**
     * 当前进程Id
     */
    public static final String                                PID                       = ManagementFactory
                                                                                            .getRuntimeMXBean()
                                                                                            .getName().split("@")[0];

    /**
     * 当前应用启动时间（用这个类加载时间为准）
     */
    public static final long                                  START_TIME                = now();

    /**
     * 发布的服务配置
     */
    private final static ConcurrentHashSet<ProviderBootstrap> EXPORTED_PROVIDER_CONFIGS = new ConcurrentHashSet<ProviderBootstrap>();

    /**
     * 发布的订阅配置
     */
    private final static ConcurrentHashSet<ConsumerBootstrap> REFERRED_CONSUMER_CONFIGS = new ConcurrentHashSet<ConsumerBootstrap>();

    /**
     * 关闭资源的钩子
     */
    private final static List<Destroyable.DestroyHook>        DESTROY_HOOKS             = new CopyOnWriteArrayList<Destroyable.DestroyHook>();

    static {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Welcome! Loading SOFA RPC Framework : {}, PID is:{}", Version.BUILD_VERSION, PID);
        }
        put(RpcConstants.CONFIG_KEY_RPC_VERSION, Version.RPC_VERSION);
        // 初始化一些上下文
        initContext();
        // 初始化其它模块
        ModuleFactory.installModules();
        // 增加jvm关闭事件
        if (RpcConfigs.getOrDefaultValue(RpcOptions.JVM_SHUTDOWN_HOOK, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("SOFA RPC Framework catch JVM shutdown event, Run shutdown hook now.");
                    }
                    destroy(false);
                }
            }, "SOFA-RPC-ShutdownHook"));
        }
    }

    /**
     * 初始化一些上下文
     */
    private static void initContext() {
        putIfAbsent(KEY_APPID, RpcConfigs.getOrDefaultValue(APP_ID, null));
        putIfAbsent(KEY_APPNAME, RpcConfigs.getOrDefaultValue(APP_NAME, null));
        putIfAbsent(KEY_APPINSID, RpcConfigs.getOrDefaultValue(INSTANCE_ID, null));
        putIfAbsent(KEY_APPAPTH, System.getProperty("user.dir"));
    }

    /**
     * 主动销毁全部SOFA RPC运行相关环境
     */
    public static void destroy() {
        destroy(true);
    }

    /**
     * 销毁方法
     *
     * @param active 是否主动销毁
     */
    private static void destroy(boolean active) {
        // TODO 检查是否有其它需要释放的资源
        RpcRunningState.setShuttingDown(true);
        for (Destroyable.DestroyHook destroyHook : DESTROY_HOOKS) {
            destroyHook.preDestroy();
        }
        List<ProviderConfig> providerConfigs = new ArrayList<ProviderConfig>();
        for (ProviderBootstrap bootstrap : EXPORTED_PROVIDER_CONFIGS) {
            providerConfigs.add(bootstrap.getProviderConfig());
        }
        // 先反注册服务端
        List<Registry> registries = RegistryFactory.getRegistries();
        if (CommonUtils.isNotEmpty(registries) && CommonUtils.isNotEmpty(providerConfigs)) {
            for (Registry registry : registries) {
                registry.batchUnRegister(providerConfigs);
            }
        }
        // 关闭启动的端口
        ServerFactory.destroyAll();
        // 关闭发布的服务
        for (ProviderBootstrap bootstrap : EXPORTED_PROVIDER_CONFIGS) {
            bootstrap.unExport();
        }
        // 关闭调用的服务
        for (ConsumerBootstrap bootstrap : REFERRED_CONSUMER_CONFIGS) {
            ConsumerConfig config = bootstrap.getConsumerConfig();
            if (!CommonUtils.isFalse(config.getParameter(RpcConstants.HIDDEN_KEY_DESTROY))) { // 除非不让主动unrefer
                bootstrap.unRefer();
            }
        }
        // 关闭注册中心
        RegistryFactory.destroyAll();
        // 关闭客户端的一些公共资源
        ClientTransportFactory.closeAll();
        // 卸载模块
        if (!RpcRunningState.isUnitTestMode()) {
            ModuleFactory.uninstallModules();
        }
        // 卸载钩子
        for (Destroyable.DestroyHook destroyHook : DESTROY_HOOKS) {
            destroyHook.postDestroy();
        }
        // 清理缓存
        RpcCacheManager.clearAll();
        RpcRunningState.setShuttingDown(false);
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("SOFA RPC Framework has been release all resources {}...",
                active ? "actively " : "");
        }
    }

    /**
     * 注册销毁器
     *
     * @param destroyHook 结果
     */
    public static void registryDestroyHook(Destroyable.DestroyHook destroyHook) {
        DESTROY_HOOKS.add(destroyHook);
    }

    /**
     * 获取当前时间，此处可以做优化
     *
     * @return 当前时间
     */
    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * 增加缓存ConsumerConfig
     *
     * @param consumerConfig the consumer config
     */
    public static void cacheConsumerConfig(ConsumerBootstrap consumerConfig) {
        REFERRED_CONSUMER_CONFIGS.add(consumerConfig);
    }

    /**
     * 缓存的ConsumerConfig失效
     *
     * @param consumerConfig the consumer config
     */
    public static void invalidateConsumerConfig(ConsumerBootstrap consumerConfig) {
        REFERRED_CONSUMER_CONFIGS.remove(consumerConfig);
    }

    /**
     * 增加缓存ProviderConfig
     *
     * @param providerConfig the provider config
     */
    public static void cacheProviderConfig(ProviderBootstrap providerConfig) {
        EXPORTED_PROVIDER_CONFIGS.add(providerConfig);
    }

    /**
     * 缓存的ProviderConfig失效
     *
     * @param providerConfig the provider config
     */
    public static void invalidateProviderConfig(ProviderBootstrap providerConfig) {
        EXPORTED_PROVIDER_CONFIGS.remove(providerConfig);
    }

    /**
     * 得到已发布的全部ProviderConfig
     *
     * @return the provider configs
     */
    public static List<ProviderBootstrap> getProviderConfigs() {
        return new ArrayList<ProviderBootstrap>(EXPORTED_PROVIDER_CONFIGS);
    }

    /**
     * 得到已调用的全部ConsumerConfig
     *
     * @return the consumer configs
     */
    public static List<ConsumerBootstrap> getConsumerConfigs() {
        return new ArrayList<ConsumerBootstrap>(REFERRED_CONSUMER_CONFIGS);
    }

    /**
     * 得到上下文信息
     *
     * @param key the key
     * @return the object
     * @see ConcurrentHashMap#get(Object)
     */
    public static Object get(String key) {
        return CONTEXT.get(key);
    }

    /**
     * 设置上下文信息（不存在才设置成功）
     *
     * @param key   the key
     * @param value the value
     * @return the object
     * @see ConcurrentHashMap#putIfAbsent(Object, Object)
     */
    public static Object putIfAbsent(String key, Object value) {
        return value == null ? CONTEXT.remove(key) : CONTEXT.putIfAbsent(key, value);
    }

    /**
     * 设置上下文信息
     *
     * @param key   the key
     * @param value the value
     * @return the object
     * @see ConcurrentHashMap#put(Object, Object)
     */
    public static Object put(String key, Object value) {
        return value == null ? CONTEXT.remove(key) : CONTEXT.put(key, value);
    }

    /**
     * 得到全部上下文信息
     *
     * @return the CONTEXT
     */
    public static ConcurrentHashMap getContext() {
        return new ConcurrentHashMap(CONTEXT);
    }

    /**
     * 当前所在文件夹地址
     */
    public static final String KEY_APPAPTH  = "appPath";

    /**
     * 应用Id
     */
    public static final String APP_ID       = "sofa.app.id";
    /**
     * 应用名称
     */
    public static final String APP_NAME     = "sofa.app.name";
    /**
     * 应用实例Id
     */
    public static final String INSTANCE_ID  = "sofa.instance.id";

    /**
     * 自动部署的appId
     */
    public static final String KEY_APPID    = "appId";

    /**
     * 自动部署的appName
     */
    public static final String KEY_APPNAME  = "appName";

    /**
     * 自动部署的appInsId
     */
    public static final String KEY_APPINSID = "appInsId";

    /**
     * 按应用名卸载RPC相关服务<br>
     * 会卸载应用名下的ProviderConfig和ConsumerConfig
     *
     * @param appName 应用名
     */
    public static void unload(String appName) {
        //TODO
    }
}
