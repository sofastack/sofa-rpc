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
package com.alipay.sofa.rpc.server.triple;

import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.interceptor.ServerReqHeaderInterceptor;
import com.alipay.sofa.rpc.interceptor.ServerResHeaderInterceptor;
import com.alipay.sofa.rpc.interceptor.TripleServerInterceptor;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.proxy.ProxyFactory;
import com.alipay.sofa.rpc.server.BusinessPool;
import com.alipay.sofa.rpc.server.Server;
import com.alipay.sofa.rpc.server.SofaRejectedExecutionHandler;
import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.ServerChannel;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollServerSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.grpc.util.MutableHandlerRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Grpc server base on io.gprc
 *
 * @author LiangEn.LiWei
 * @date 2018.11.20 5:27 PM
 */
@Extension("tri")
public class TripleServer implements Server {

    /**
     * Logger
     */
    private static final Logger                                          LOGGER          = LoggerFactory
                                                                                             .getLogger(TripleServer.class);
    /**
     * server config
     */
    protected ServerConfig                                               serverConfig;

    /**
     * Is it already started
     */
    protected volatile boolean                                           started;

    /**
     * grpc server
     */
    protected io.grpc.Server                                             server;

    /**
     * service registry
     */
    protected MutableHandlerRegistry                                     handlerRegistry = new MutableHandlerRegistry();

    /**
     * The mapping relationship between BindableService and ServerServiceDefinition
     */
    protected ConcurrentHashMap<ProviderConfig, ServerServiceDefinition> serviceInfo     = new ConcurrentHashMap<ProviderConfig,
                                                                                                 ServerServiceDefinition>();

    /**
     * invoker count
     */
    protected AtomicInteger                                              invokerCnt      = new AtomicInteger();

    @Override
    public void init(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        server = NettyServerBuilder.forPort(serverConfig.getPort()).
            fallbackHandlerRegistry(handlerRegistry)
            .bossEventLoopGroup(constructBossEventLoopGroup())
            .workerEventLoopGroup(constructWorkerEventLoopGroup())
            .executor(initThreadPool(serverConfig))
            .channelType(constructChannel())
            .build();
    }

    private Class<? extends ServerChannel> constructChannel() {
        return serverConfig.isEpoll() ?
            EpollServerSocketChannel.class :
            NioServerSocketChannel.class;
    }

    private EventLoopGroup constructWorkerEventLoopGroup() {

        int workerThreads = serverConfig.getIoThreads();
        workerThreads = workerThreads <= 0 ? Runtime.getRuntime().availableProcessors() * 2 : workerThreads;
        NamedThreadFactory threadName =
                new NamedThreadFactory("SEV-WORKER-" + serverConfig.getPort(), serverConfig.isDaemon());
        EventLoopGroup workerGroup = serverConfig.isEpoll() ?
            new EpollEventLoopGroup(workerThreads, threadName) :
            new NioEventLoopGroup(workerThreads, threadName);

        return workerGroup;
    }

    /**
     * default as bolt
     *
     * @return
     */
    private EventLoopGroup constructBossEventLoopGroup() {
        NamedThreadFactory threadName =
                new NamedThreadFactory("SEV-BOSS-" + serverConfig.getPort(), serverConfig.isDaemon());
        EventLoopGroup bossGroup = serverConfig.isEpoll() ?
            new EpollEventLoopGroup(1, threadName) :
            new NioEventLoopGroup(1, threadName);
        return bossGroup;
    }

    protected ThreadPoolExecutor initThreadPool(ServerConfig serverConfig) {
        ThreadPoolExecutor threadPool = BusinessPool.initPool(serverConfig);
        threadPool.setThreadFactory(new NamedThreadFactory(
            "SEV-TRIPLE-BIZ-" + serverConfig.getPort(), serverConfig.isDaemon()));
        threadPool.setRejectedExecutionHandler(new SofaRejectedExecutionHandler());
        if (serverConfig.isPreStartCore()) { // 初始化核心线程池
            threadPool.prestartAllCoreThreads();
        }
        return threadPool;
    }

    @Override
    public void start() {
        if (started) {
            return;
        }
        synchronized (this) {
            try {
                server.start();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Start the triple server at port {}", serverConfig.getPort());
                }
            } catch (SofaRpcRuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_START_SERVER_WITH_PORT, "grpc",
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
                LOGGER.info("Stop the triple server at port {}", serverConfig.getPort());
            }
            server.shutdown();
        } catch (Exception e) {
            LOGGER.error("Stop the triple server at port " + serverConfig.getPort() + " error !", e);
        }
        started = false;
    }

    @Override
    public void registerProcessor(ProviderConfig providerConfig, Invoker instance) {
        Object obj = null;
        try {
            obj = ProxyFactory.buildProxy(providerConfig.getProxy(), BindableService.class, instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        BindableService bindableService = (BindableService) obj;

        try {
            final ServerServiceDefinition serviceDef = bindableService.bindService();
            List<TripleServerInterceptor> interceptorList = new ArrayList<>();
            interceptorList.add(new ServerReqHeaderInterceptor(serviceDef));
            interceptorList.add(new ServerResHeaderInterceptor(serviceDef));
            ServerServiceDefinition serviceDefinition = ServerInterceptors.intercept(
                    serviceDef, interceptorList);
            serviceInfo.put(providerConfig, serviceDefinition);
            handlerRegistry.addService(serviceDefinition);
            invokerCnt.incrementAndGet();
        } catch (Exception e) {
            LOGGER.error("Register triple service error", e);
            serviceInfo.remove(providerConfig);
        }
    }

    @Override
    public void unRegisterProcessor(ProviderConfig providerConfig, boolean closeIfNoEntry) {
        try {
            ServerServiceDefinition serverServiceDefinition = serviceInfo.get(providerConfig);
            handlerRegistry.removeService(serverServiceDefinition);
            invokerCnt.decrementAndGet();
        } catch (Exception e) {
            LOGGER.error("Unregister triple service error", e);
        }
        if (closeIfNoEntry && invokerCnt.get() == 0) {
            stop();
        }
    }

    @Override
    public void destroy() {
        stop();
        server = null;
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