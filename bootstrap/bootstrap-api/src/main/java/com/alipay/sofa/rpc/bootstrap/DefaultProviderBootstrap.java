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
package com.alipay.sofa.rpc.bootstrap;

import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default provider bootstrap.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension("sofa")
public class DefaultProviderBootstrap<T> extends ProviderBootstrap<T> {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultProviderBootstrap.class);

    /**
     * 构造函数
     *
     * @param providerConfig 服务发布者配置
     */
    protected DefaultProviderBootstrap(ProviderConfig<T> providerConfig) {
        super(providerConfig);
    }

    /**
     * 是否已发布
     */
    protected transient volatile boolean                        exported;

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
        if (providerConfig.getDelay() > 0) { // 延迟加载,单位毫秒
            Thread thread = factory.newThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(providerConfig.getDelay());
                    } catch (Throwable ignore) { // NOPMD
                    }
                    doExport();
                }
            });
            thread.start();
        } else {
            doExport();
        }
    }

    private void doExport() {
        if (exported) {
            return;
        }

        // 检查参数
        checkParameters();

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
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_DUPLICATE_PROVIDER_CONFIG, key,
                        maxProxyCount));
                } else if (c > 1) {
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.WARN_DUPLICATE_PROVIDER_CONFIG, key, c));
                    }
                }
            }

        }

        try {
            // 构造请求调用器
            providerProxyInvoker = new ProviderProxyInvoker(providerConfig);

            preProcessProviderTarget(providerConfig, (ProviderProxyInvoker) providerProxyInvoker);
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
                    if (serverConfig.isAutoStart()) {
                        server.start();
                    }

                } catch (SofaRpcRuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.errorWithApp(appName,
                        LogCodes.getLog(LogCodes.ERROR_REGISTER_PROCESSOR_TO_SERVER, serverConfig.getId()), e);
                }
            }

            // 注册到注册中心
            providerConfig.setConfigListener(new ProviderAttributeListener());
            register();
        } catch (Exception e) {
            decrementCounter(hasExportedInCurrent);
            if (e instanceof SofaRpcRuntimeException) {
                throw e;
            }
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_BUILD_PROVIDER_PROXY), e);
        }

        // 记录一些缓存数据
        RpcRuntimeContext.cacheProviderConfig(this);
        exported = true;
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
     * for check fields and parameters of consumer config
     */
    protected void checkParameters() {
        // 检查注入的ref是否接口实现类
        Class proxyClass = providerConfig.getProxyClass();
        String key = providerConfig.buildKey();
        T ref = providerConfig.getRef();
        if (!proxyClass.isInstance(ref)) {
            String name = ref == null ? "null" : ref.getClass().getName();
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_REFERENCE_AND_INTERFACE, name,
                providerConfig.getInterfaceId(), key));
        }
        // server 不能为空
        if (CommonUtils.isEmpty(providerConfig.getServer())) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_SERVER_EMPTY, key));
        }
        checkMethods(proxyClass);
    }

    /**
     * 检查方法，例如方法名、多态（重载）方法
     *
     * @param itfClass 接口类
     */
    protected void checkMethods(Class<?> itfClass) {
        ConcurrentHashMap<String, Boolean> methodsLimit = new ConcurrentHashMap<String, Boolean>();
        for (Method method : itfClass.getMethods()) {
            String methodName = method.getName();
            if (methodsLimit.containsKey(methodName)) {
                // 重名的方法
                if (LOGGER.isWarnEnabled(providerConfig.getAppName())) {
                    // TODO WARN
                    LOGGER.warnWithApp(providerConfig.getAppName(), "Method with same name \"" + itfClass.getName()
                        + "." + methodName + "\" exists ! The usage of overloading method in rpc is deprecated.");
                }
            }
            // 判断服务下方法的黑白名单
            Boolean include = methodsLimit.get(methodName);
            if (include == null) {
                include = inList(providerConfig.getInclude(), providerConfig.getExclude(), methodName); // 检查是否在黑白名单中
                methodsLimit.putIfAbsent(methodName, include);
            }
        }
        providerConfig.setMethodsLimit(methodsLimit);
    }

    @Override
    public void unExport() {
        if (!exported) {
            return;
        }
        synchronized (this) {
            if (!exported) {
                return;
            }
            String appName = providerConfig.getAppName();

            List<ServerConfig> serverConfigs = providerConfig.getServer();
            for (ServerConfig serverConfig : serverConfigs) {
                String protocol = serverConfig.getProtocol();
                String key = providerConfig.buildKey() + ":" + protocol;
                if (LOGGER.isInfoEnabled(appName)) {
                    LOGGER.infoWithApp(appName, "Unexport provider config : {} {}", key, providerConfig.getId() != null
                        ? "with bean id " + providerConfig.getId() : "");
                }
            }

            // 取消注册到注册中心
            unregister();

            providerProxyInvoker = null;

            // 取消将处理器注册到server
            if (serverConfigs != null) {
                for (ServerConfig serverConfig : serverConfigs) {
                    Server server = serverConfig.getServer();
                    if (server != null) {
                        try {
                            server.unRegisterProcessor(providerConfig, serverConfig.isAutoStart());
                        } catch (Exception e) {
                            if (LOGGER.isWarnEnabled(appName)) {
                                // TODO WARN
                                LOGGER.warnWithApp(appName, "Catch exception when unRegister processor to server: " +
                                    serverConfig.getId()
                                    + ", but you can ignore if it's called by JVM shutdown hook", e);
                            }
                        }
                    }
                }
            }

            providerConfig.setConfigListener(null);

            // 清除缓存状态
            for (ServerConfig serverConfig : serverConfigs) {
                String protocol = serverConfig.getProtocol();
                String key = providerConfig.buildKey() + ":" + protocol;
                AtomicInteger cnt = EXPORTED_KEYS.get(key);
                if (cnt != null && cnt.decrementAndGet() <= 0) {
                    EXPORTED_KEYS.remove(key);
                }
            }

            RpcRuntimeContext.invalidateProviderConfig(this);
            exported = false;
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
                            LOGGER.errorWithApp(appName,
                                LogCodes.getLog(LogCodes.ERROR_REGISTER_TO_REGISTRY, registryConfig.getId()), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 反注册服务
     */
    protected void unregister() {
        if (providerConfig.isRegister()) {
            List<RegistryConfig> registryConfigs = providerConfig.getRegistry();
            if (registryConfigs != null) {
                for (RegistryConfig registryConfig : registryConfigs) {
                    Registry registry = RegistryFactory.getRegistry(registryConfig);
                    try {
                        registry.unRegister(providerConfig);
                    } catch (Exception e) {
                        String appName = providerConfig.getAppName();
                        if (LOGGER.isWarnEnabled(appName)) {
                            // TODO WARN
                            LOGGER.warnWithApp(appName, "Catch exception when unRegister from registry: " +
                                registryConfig.getId()
                                + ", but you can ignore if it's called by JVM shutdown hook", e);
                        }
                    }
                }
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
                LOGGER.errorWithApp(appName, LogCodes.getLog(LogCodes.ERROR_PROVIDER_ATTRIBUTE_COMPARE), e);
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
     * 接口可以按方法发布,不在黑名单里且在白名单里,*算在白名单
     *
     * @param includeMethods 包含的方法列表
     * @param excludeMethods 不包含的方法列表
     * @param methodName     方法名
     * @return 方法
     */
    protected boolean inList(String includeMethods, String excludeMethods, String methodName) {
        //判断是否在白名单中
        if (!StringUtils.ALL.equals(includeMethods)) {
            if (!inMethodConfigs(includeMethods, methodName)) {
                return false;
            }
        }
        //判断是否在黑白单中
        if (inMethodConfigs(excludeMethods, methodName)) {
            return false;
        }
        //默认还是要发布
        return true;

    }

    /**
     * 否则存在method configs 字符串中
     *
     * @param methodConfigs
     * @param methodName
     * @return
     */
    private boolean inMethodConfigs(String methodConfigs, String methodName) {
        String[] excludeMethodCollections = StringUtils.splitWithCommaOrSemicolon(methodConfigs);
        for (String excludeMethodName : excludeMethodCollections) {
            boolean exist = StringUtils.equals(excludeMethodName, methodName);
            if (exist) {
                return true;
            }
        }
        return false;
    }

    /**
     * make other provider bootstrap can do extra work
     * @param providerConfig
     */
    protected void preProcessProviderTarget(ProviderConfig providerConfig, ProviderProxyInvoker providerProxyInvoker) {
        return;
    }
}
