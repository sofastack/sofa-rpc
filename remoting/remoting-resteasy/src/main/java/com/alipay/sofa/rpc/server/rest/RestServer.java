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
package com.alipay.sofa.rpc.server.rest;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.JAXRSProviderManager;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.proxy.ProxyFactory;
import com.alipay.sofa.rpc.server.Server;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rest server base on resteasy.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("rest")
public class RestServer implements Server {

    /**
     * Logger
     */
    private static final Logger    LOGGER     = LoggerFactory.getLogger(RestServer.class);

    /**
     * 是否已经启动
     */
    protected volatile boolean     started;

    /**
     * Rest服务端
     */
    protected SofaNettyJaxrsServer httpServer;

    /**
     * 服务端配置
     */
    protected ServerConfig         serverConfig;

    /**
     * invoker数量
     */
    protected AtomicInteger        invokerCnt = new AtomicInteger();

    @Override
    public void init(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        httpServer = buildServer();
    }

    protected SofaNettyJaxrsServer buildServer() {
        // 生成Server对象
        SofaNettyJaxrsServer httpServer = new SofaNettyJaxrsServer(serverConfig);

        int bossThreads = serverConfig.getIoThreads();
        if (bossThreads > 0) {
            httpServer.setIoWorkerCount(bossThreads); // 其实是boss+worker线程 默认cpu*2
        }
        httpServer.setExecutorThreadCount(serverConfig.getMaxThreads()); // 业务线程
        httpServer.setMaxRequestSize(serverConfig.getPayload());
        httpServer.setHostname(serverConfig.getBoundHost());
        httpServer.setPort(serverConfig.getPort());

        ResteasyDeployment resteasyDeployment = httpServer.getDeployment();
        resteasyDeployment.start();

        ResteasyProviderFactory providerFactory = resteasyDeployment.getProviderFactory();
        registerProvider(providerFactory);

        return httpServer;
    }

    protected void registerProvider(ResteasyProviderFactory providerFactory) {
        // 注册内置
        Set<Class> internalProviderClasses = JAXRSProviderManager.getInternalProviderClasses();
        if (CommonUtils.isNotEmpty(internalProviderClasses)) {
            for (Class providerClass : internalProviderClasses) {
                providerFactory.register(providerClass);
            }
        }

        // 注册cors filter
        Map<String, String> parameters = serverConfig.getParameters();
        if (CommonUtils.isNotEmpty(parameters)) {
            String crossDomainStr = parameters.get(RpcConstants.ALLOWED_ORIGINS);
            if (StringUtils.isNotBlank(crossDomainStr)) {
                final CorsFilter corsFilter = new CorsFilter();
                String[] domains = StringUtils.splitWithCommaOrSemicolon(crossDomainStr);
                for (String allowDomain : domains) {
                    corsFilter.getAllowedOrigins().add(allowDomain);
                }
                JAXRSProviderManager.registerCustomProviderInstance(corsFilter);
            }
        }

        // 注册自定义
        Set<Object> customProviderInstances = JAXRSProviderManager.getCustomProviderInstances();
        if (CommonUtils.isNotEmpty(customProviderInstances)) {
            for (Object provider : customProviderInstances) {
                PropertyInjector propertyInjector = providerFactory.getInjectorFactory()
                    .createPropertyInjector(
                        JAXRSProviderManager.getTargetClass(provider), providerFactory);
                propertyInjector.inject(provider);
                providerFactory.registerProviderInstance(provider);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            Set pcs = providerFactory.getProviderClasses();
            StringBuilder sb = new StringBuilder();
            sb.append("\ndefault-providers:\n");

            for (Object provider : pcs) {
                sb.append("  ").append(provider).append("\n");
            }
            LOGGER.debug(sb.toString());
        }
    }

    @Override
    public void start() {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            // 绑定到端口
            try {
                httpServer.start();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Start the http rest server at port {}", serverConfig.getPort());
                }
            } catch (SofaRpcRuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_START_SERVER_WITH_PORT, "rest",
                    serverConfig.getPort()), e);
            }
            started = true;
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean hasNoEntry() {
        return false;
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        try {
            // 关闭端口，不关闭线程池
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Stop the http rest server at port {}", serverConfig.getPort());
            }
            httpServer.stop();
        } catch (Exception e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_STOP_SERVER_WITH_PORT, serverConfig.getPort()), e);
        }
        started = false;
    }

    @Override
    public void registerProcessor(ProviderConfig providerConfig, Invoker instance) {
        if (!isStarted()) {
            start();
        }
        // 在httpserver中注册此jaxrs服务
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Register jaxrs service to base url http://" + serverConfig.getHost() + ":"
                + serverConfig.getPort() + serverConfig.getContextPath());
        }
        Object obj = null;
        try {
            obj = ProxyFactory.buildProxy(providerConfig.getProxy(), providerConfig.getProxyClass(), instance);
            httpServer.getDeployment().getRegistry()
                .addResourceFactory(new SofaResourceFactory(providerConfig, obj), serverConfig.getContextPath());

            invokerCnt.incrementAndGet();
        } catch (SofaRpcRuntimeException e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_REGISTER_PROCESSOR_TO_SERVER, "restServer"), e);
            throw e;
        } catch (Exception e) {
            throw new SofaRpcRuntimeException(
                LogCodes.getLog(LogCodes.ERROR_REGISTER_PROCESSOR_TO_SERVER, "restServer"), e);
        }
    }

    @Override
    public void unRegisterProcessor(ProviderConfig providerConfig, boolean closeIfNoEntry) {
        if (!isStarted()) {
            return;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Unregister jaxrs service to port {} and base path is {}", serverConfig.getPort(),
                serverConfig.getContextPath());
        }
        try {
            httpServer.getDeployment().getRegistry()
                .removeRegistrations(providerConfig.getRef().getClass(), serverConfig.getContextPath());
            invokerCnt.decrementAndGet();
        } catch (Exception e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_UNREG_PROCESSOR, "jaxrs"), e);
        }
        // 如果最后一个需要关闭，则关闭
        if (closeIfNoEntry && invokerCnt.get() == 0) {
            stop();
        }
    }

    @Override
    public void destroy() {
        stop();
        httpServer = null;
    }

    @Override
    public void destroy(DestroyHook hook) {
        if (hook != null) {
            hook.preDestroy();
        }
        destroy();
        if (hook != null) {
            hook.postDestroy();
        }
    }
}
