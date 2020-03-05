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
package com.alipay.sofa.rpc.bootstrap.grpc;

import com.alipay.sofa.rpc.bootstrap.ProviderBootstrap;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.Version;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.server.ProviderProxyInvoker;
import com.alipay.sofa.rpc.server.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// import io.grpc.Server;
// import io.grpc.ServerBuilder;
// import io.grpc.BindableService;

/**
 * Provider bootstrap for grpc
 *
 * @author <a href=mailto:yqluan@gmail.com>Yanqiang Oliver Luan (neokidd)</a>
 */
@Extension("grpc")
public class GrpcProviderBootstrap<T> extends ProviderBootstrap<T> {

    /**
     * 构造函数
     *
     * @param providerConfig 服务发布者配置
     */
    protected GrpcProviderBootstrap(ProviderConfig<T> providerConfig) {
        super(providerConfig);

    }

    /**
     * 是否已发布
     */
    protected transient volatile boolean                        exported;

    private Server                                              server;
    private String                                              host;
    private int                                                 port;
    private final static Logger                                 LOGGER        = LoggerFactory
                                                                                  .getLogger(GrpcProviderBootstrap.class);

    /**
    * 服务端Invoker对象
    */
    protected transient Invoker                                 providerProxyInvoker;

    /**
     * 发布的服务配置
     */
    protected final static ConcurrentMap<String, AtomicInteger> EXPORTED_KEYS = new ConcurrentHashMap<String, AtomicInteger>();

    /**
     * 延迟加载的线程名工厂
     */
    private final ThreadFactory                                 factory       = new NamedThreadFactory(
                                                                                  "DELAY-EXPORT",
                                                                                  true);

    @Override
    public void export() {
        if (exported) {
            return;
        }

        String interfaceType = providerConfig.getInterfaceId();
        Object ref = providerConfig.getRef();
        if (ref instanceof String) {
            try {
                ref = getInterfaceClass(interfaceType).newInstance();
            } catch (Exception e) {
                //TODO: handle exception
            }
        }

        // try {
        //     port = (providerConfig.getServer().get(0)).getPort();
        //     server = ServerBuilder.forPort(port)
        //         .addService((io.grpc.BindableService) ref)
        //         // .addService((io.grpc.BindableService) providerConfig.getRef())
        //         .build()
        //         .start();
        //     exported = true;
        // } catch (IOException e) {
        //     LOGGER.error("starting GPRC server fails.");
        // }

        //检查参数
        // checkParameters();

        String appName = providerConfig.getAppName();

        //key  is the protocol of server,for concurrent safe
        Map<String, Boolean> hasExportedInCurrent = new ConcurrentHashMap<String, Boolean>();
        // 将处理器注册到server
        List<ServerConfig> serverConfigs = providerConfig.getServer();
        for (ServerConfig serverConfig : serverConfigs) {
            String protocol = serverConfig.getProtocol();

            String key = providerConfig.buildKey() + ":" + protocol;

            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, "Export provider config : {} with bean id {}", key, providerConfig.getId());
            }

            // 注意同一interface，同一uniqueId，不同server情况
            AtomicInteger cnt = EXPORTED_KEYS.get(key); // 计数器
            if (cnt == null) { // 没有发布过
                cnt = CommonUtils.putToConcurrentMap(EXPORTED_KEYS, key, new AtomicInteger(0));
            }
            int c = cnt.incrementAndGet();
            hasExportedInCurrent.put(serverConfig.getProtocol(), true);
            int maxProxyCount = providerConfig.getRepeatedExportLimit();
            if (maxProxyCount > 0) {
                if (c > maxProxyCount) {
                    decrementCounter(hasExportedInCurrent);
                    // 超过最大数量，直接抛出异常
                    throw new SofaRpcRuntimeException("Duplicate provider config with key " + key
                        + " has been exported more than " + maxProxyCount + " times!"
                        + " Maybe it's wrong config, please check it."
                        + " Ignore this if you did that on purpose!");
                } else if (c > 1) {
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, "Duplicate provider config with key {} has been exported!"
                            + " Maybe it's wrong config, please check it."
                            + " Ignore this if you did that on purpose!", key);
                    }
                }
            }

        }

        try {
            // 构造请求调用器
            providerProxyInvoker = new ProviderProxyInvoker(providerConfig);
            // 初始化注册中心
            if (providerConfig.isRegister()) {
                List<RegistryConfig> registryConfigs = providerConfig.getRegistry();
                if (CommonUtils.isNotEmpty(registryConfigs)) {
                    for (RegistryConfig registryConfig : registryConfigs) {
                        RegistryFactory.getRegistry(registryConfig); // 提前初始化Registry
                    }
                }
            }
            // 将处理器注册到server
            for (ServerConfig serverConfig : serverConfigs) {
                try {
                    Server server = serverConfig.buildIfAbsent();
                    // 注册请求调用器
                    server.registerProcessor(providerConfig, providerProxyInvoker);
                    // if (serverConfig.isAutoStart()) {
                    if (true) {
                        server.start();
                    }

                } catch (SofaRpcRuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.errorWithApp(appName, "Catch exception when register processor to server: "
                        + serverConfig.getId(), e);
                }
            }

            // 注册到注册中心
            providerConfig.setConfigListener(new ProviderAttributeListener());
            register();
        } catch (Exception e) {
            decrementCounter(hasExportedInCurrent);

            if (e instanceof SofaRpcRuntimeException) {
                throw (SofaRpcRuntimeException) e;
            } else {
                throw new SofaRpcRuntimeException("Build provider proxy error!", e);
            }
        }

        // 记录一些缓存数据
        RpcRuntimeContext.cacheProviderConfig(this);
        exported = true;
        port = (providerConfig.getServer().get(0)).getPort();

        LOGGER.info("GRPC server starts successfully, port: {}", port);

        return;
    }

    public Class<?> getInterfaceClass(String interfaceType) {

        try {
            Class<?> interfaceClass = this.getClass().getClassLoader().loadClass(interfaceType);
            return interfaceClass;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            // No type found for shortcut FactoryBean instance:
            // fall back to full creation of the FactoryBean instance
            return null;
        } finally {
            return null;
        }
    }

    /**
     * 取消发布（从server里取消注册）
     */
    @Override
    public synchronized void unExport() {
        if (!exported) {
            return;
        }
        // serviceConfig.unexport();
        exported = false;
    }

    /**
     * 得到已发布的全部list
     *
     * @return urls urls
     */
    public List<String> buildUrls() {
        if (exported) {
            List<ServerConfig> servers = providerConfig.getServer();
            if (servers != null && !servers.isEmpty()) {
                List<String> urls = new ArrayList<String>();
                for (ServerConfig server : servers) {
                    StringBuilder sb = new StringBuilder(200);
                    sb.append(server.getProtocol()).append("://").append(server.getHost())
                        .append(":").append(server.getPort()).append(server.getContextPath())
                        .append(providerConfig.getInterfaceId())
                        .append("?uniqueId=").append(providerConfig.getUniqueId())
                        .append(getKeyPairs("version", "1.0"))
                        .append(getKeyPairs("delay", providerConfig.getDelay()))
                        .append(getKeyPairs("weight", providerConfig.getWeight()))
                        .append(getKeyPairs("register", providerConfig.isRegister()))
                        .append(getKeyPairs("maxThreads", server.getMaxThreads()))
                        .append(getKeyPairs("ioThreads", server.getIoThreads()))
                        .append(getKeyPairs("threadPoolType", server.getThreadPoolType()))
                        .append(getKeyPairs("accepts", server.getAccepts()))
                        .append(getKeyPairs("dynamic", providerConfig.isDynamic()))
                        .append(getKeyPairs(RpcConstants.CONFIG_KEY_RPC_VERSION, Version.RPC_VERSION));
                    urls.add(sb.toString());
                }
                return urls;
            }
        }
        return null;
    }

    /**
     * Gets key pairs.
     *
     * @param key   the key
     * @param value the value
     * @return the key pairs
     */
    private String getKeyPairs(String key, Object value) {
        if (value != null) {
            return "&" + key + "=" + value.toString();
        } else {
            return "";
        }
    }

    /**
    * decrease counter
    *
    * @param hasExportedInCurrent
    */
    private void decrementCounter(Map<String, Boolean> hasExportedInCurrent) {
        //once error, we decrementAndGet the counter
        for (Map.Entry<String, Boolean> entry : hasExportedInCurrent.entrySet()) {
            String protocol = entry.getKey();
            String key = providerConfig.buildKey() + ":" + protocol;
            AtomicInteger cnt = EXPORTED_KEYS.get(key); // 计数器
            if (cnt != null && cnt.get() > 0) {
                cnt.decrementAndGet();
            }
        }
    }

    /**
    * Provider配置发生变化监听器
    */
    private class ProviderAttributeListener implements ConfigListener {

        @Override
        public void configChanged(Map newValue) {
        }

        @Override
        public synchronized void attrUpdated(Map newValueMap) {
            String appName = providerConfig.getAppName();
            // 可以改变的配置 例如tag concurrents等
            Map<String, String> newValues = (Map<String, String>) newValueMap;
            Map<String, String> oldValues = new HashMap<String, String>();
            boolean reexport = false;

            // TODO 可能需要处理ServerConfig的配置变化
            try { // 检查是否有变化
                  // 是否过滤map?
                for (Map.Entry<String, String> entry : newValues.entrySet()) {
                    String newValue = entry.getValue();
                    String oldValue = providerConfig.queryAttribute(entry.getKey());
                    boolean changed = oldValue == null ? newValue != null : !oldValue.equals(newValue);
                    if (changed) {
                        oldValues.put(entry.getKey(), oldValue);
                    }
                    reexport = reexport || changed;
                }
            } catch (Exception e) {
                LOGGER.errorWithApp(appName, "Catch exception when provider attribute compare", e);
                return;
            }

            // 需要重新发布
            if (reexport) {
                try {
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, "Reexport service {}", providerConfig.buildKey());
                    }
                    unExport();
                    // change attrs
                    for (Map.Entry<String, String> entry : newValues.entrySet()) {
                        providerConfig.updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                    export();
                } catch (Exception e) {
                    LOGGER.errorWithApp(appName, LogCodes.getLog(LogCodes.ERROR_PROVIDER_ATTRIBUTE_CHANGE), e);
                    //rollback old attrs
                    for (Map.Entry<String, String> entry : oldValues.entrySet()) {
                        providerConfig.updateAttribute(entry.getKey(), entry.getValue(), true);
                    }
                    export();
                }
            }

        }
    }

    /**
    * 注册服务
    */
    protected void register() {
        if (providerConfig.isRegister()) {
            List<RegistryConfig> registryConfigs = providerConfig.getRegistry();
            if (registryConfigs != null) {
                for (RegistryConfig registryConfig : registryConfigs) {
                    Registry registry = RegistryFactory.getRegistry(registryConfig);
                    registry.init();
                    registry.start();
                    try {
                        registry.register(providerConfig);
                    } catch (SofaRpcRuntimeException e) {
                        throw e;
                    } catch (Throwable e) {
                        String appName = providerConfig.getAppName();
                        if (LOGGER.isWarnEnabled(appName)) {
                            LOGGER.warnWithApp(appName, "Catch exception when register to registry: "
                                + registryConfig.getId(), e);
                        }
                    }
                }
            }
        }
    }
}
