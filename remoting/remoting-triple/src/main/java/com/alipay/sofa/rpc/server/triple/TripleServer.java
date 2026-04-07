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

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.cache.ReflectCache;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerStartedEvent;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.interceptor.ServerReqHeaderInterceptor;
import com.alipay.sofa.rpc.interceptor.TripleServerInterceptor;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.proxy.ProxyFactory;
import com.alipay.sofa.rpc.server.BusinessPool;
import com.alipay.sofa.rpc.server.Server;
import com.alipay.sofa.rpc.server.SofaRejectedExecutionHandler;
import com.alipay.sofa.rpc.transport.triple.http.HttpServerTransportListenerFactory;
import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;
import com.alipay.sofa.rpc.transport.triple.quic.QuicServerChannelInitializer;
import com.alipay.sofa.rpc.transport.triple.quic.QuicSslContextFactory;
import com.alipay.sofa.rpc.utils.SofaProtoUtils;
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.ServerChannel;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollServerSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.util.MutableHandlerRegistry;
import triple.Request;
import triple.Response;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.grpc.MethodDescriptor.generateFullMethodName;

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
     * Configuration key for HTTP/1.1 support
     */
    private static final String                                          HTTP1_ENABLED_KEY = "triple.http1.enabled";

    /**
     * Configuration key for HTTP/3 support
     */
    private static final String                                          HTTP3_ENABLED_KEY = "triple.http3.enabled";

    /**
     * Configuration key for port unification
     */
    private static final String                                          PORT_UNIFICATION_KEY = "triple.port-unification.enabled";

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
     * Netty server channel for custom HTTP/1.1 + HTTP/2 support
     */
    protected io.grpc.netty.shaded.io.netty.channel.Channel              nettyServerChannel;

    /**
     * Boss event loop group
     */
    protected EventLoopGroup                                             bossGroup;

    /**
     * Worker event loop group
     */
    protected EventLoopGroup                                             workerGroup;

    /**
     * service registry
     */
    protected MutableHandlerRegistry                                     handlerRegistry = new MutableHandlerRegistry();

    /**
     * The mapping relationship between interface BindableService and ServerServiceDefinition
     */
    protected ConcurrentHashMap<String, ServerServiceDefinition> serviceInfo     = new ConcurrentHashMap<>();
    /**
     * The mapping relationship between service name and unique id invoker
     */
    protected Map<String, UniqueIdInvoker>                               invokerMap = new ConcurrentHashMap<>();

    /**
     * The mapping relationship between gRPC service name (from proto definition) and unique id invoker
     * This is needed for pure HTTP/2 implementation to look up invokers by gRPC service name
     */
    protected Map<String, UniqueIdInvoker>                               grpcServiceNameInvokerMap = new ConcurrentHashMap<>();

    /**
     * Thread pool
     * @param serverConfig ServerConfig
     */
    private ThreadPoolExecutor                                           bizThreadPool;

    /**
     * lock
     */
    private Lock                                                         lock;

    /**
     * HTTP/1.1 support enabled
     */
    private boolean                                                      http1Enabled;

    /**
     * HTTP/3 support enabled
     */
    private boolean                                                      http3Enabled;

    /**
     * Port unification enabled (multi-protocol on same port)
     */
    private boolean                                                      portUnificationEnabled;

    /**
     * HTTP transport listener factories
     */
    private List<HttpServerTransportListenerFactory>                     transportListenerFactories;

    /**
     * QUIC server channel for HTTP/3 support (UDP)
     */
    protected io.netty.channel.Channel                                   quicServerChannel;

    /**
     * QUIC event loop group (UDP)
     */
    protected io.netty.channel.EventLoopGroup                            quicGroup;

    @Override
    public void init(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        bizThreadPool = initThreadPool(serverConfig);

        // Load TripleX configuration
        loadTripleXConfig();

        // Load transport listener factories
        loadTransportListenerFactories();

        // Initialize event loop groups
        bossGroup = constructBossEventLoopGroup();
        workerGroup = constructWorkerEventLoopGroup();

        // Build gRPC server for HTTP/2 support
        // When HTTP/1.1 is enabled with port unification, we use a custom Netty server
        // that detects the protocol and routes to appropriate handlers.
        if (http1Enabled && portUnificationEnabled) {
            initCustomNettyServer();
        } else {
            initGrpcServer();
        }

        // Initialize HTTP/3 (QUIC) server if enabled
        if (http3Enabled) {
            initQuicServer();
        }

        this.lock = new ReentrantLock();
    }

    /**
     * Initialize gRPC server (HTTP/2 only).
     */
    private void initGrpcServer() {
        server = NettyServerBuilder.forPort(serverConfig.getPort())
            .fallbackHandlerRegistry(handlerRegistry)
            .bossEventLoopGroup(bossGroup)
            .workerEventLoopGroup(workerGroup)
            .executor(bizThreadPool)
            .channelType(constructChannel())
            .maxInboundMetadataSize(RpcConfigs.getIntValue(RpcOptions.TRANSPORT_GRPC_MAX_INBOUND_METADATA_SIZE))
            .maxInboundMessageSize(RpcConfigs.getIntValue(RpcOptions.TRANSPORT_GRPC_MAX_INBOUND_MESSAGE_SIZE))
            .permitKeepAliveTime(1, TimeUnit.SECONDS)
            .permitKeepAliveWithoutCalls(true)
            .build();
    }

    /**
     * Initialize custom Netty server with HTTP/1.1 and HTTP/2 support.
     */
    private void initCustomNettyServer() {
        try {
            // Create custom Netty server with protocol detection
            // Pass invokerMap as a supplier to support dynamic service registration
            io.grpc.netty.shaded.io.netty.bootstrap.ServerBootstrap bootstrap =
                new io.grpc.netty.shaded.io.netty.bootstrap.ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                .channel(constructChannel())
                .childHandler(new com.alipay.sofa.rpc.transport.triple.TripleServerChannelInitializer(
                    serverConfig, () -> invokerMap, () -> grpcServiceNameInvokerMap, bizThreadPool, workerGroup));

            // Bind to port
            nettyServerChannel = bootstrap.bind(serverConfig.getPort()).syncUninterruptibly().channel();

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Custom Netty server initialized on port {} with HTTP/1.1 and HTTP/2 support",
                    serverConfig.getPort());
            }

            // Note: When using custom Netty server, we don't use gRPC server.
            // Service registration is handled by the custom server.

        } catch (Exception e) {
            LOGGER.error("Failed to initialize custom Netty server", e);
            throw new SofaRpcRuntimeException("Failed to initialize custom Netty server", e);
        }
    }

    /**
     * Initialize QUIC server for HTTP/3.
     * QUIC uses UDP transport and requires TLS 1.3.
     */
    private void initQuicServer() {
        // Check if QUIC classes are available
        if (!isQuicAvailable()) {
            LOGGER.warn("QUIC support not available (netty-incubator-codec-quic not in classpath). " +
                "HTTP/3 will be disabled.");
            this.http3Enabled = false;
            return;
        }

        try {
            // Create QUIC EventLoopGroup (UDP-based)
            quicGroup = createQuicEventLoopGroup();

            // Get HTTP/3 port (default: TCP port + 1)
            int http3Port = getHttp3Port();

            // Create SSL context (QUIC requires TLS 1.3)
            io.netty.incubator.codec.quic.QuicSslContext sslContext =
                QuicSslContextFactory.createServerSslContext(serverConfig);

            // Create QUIC server channel initializer
            QuicServerChannelInitializer initializer = new QuicServerChannelInitializer(
                serverConfig, () -> invokerMap, () -> grpcServiceNameInvokerMap, bizThreadPool, sslContext);

            // Create QUIC server bootstrap
            io.netty.bootstrap.Bootstrap bootstrap = new io.netty.bootstrap.Bootstrap();

            // Configure QUIC server codec
            io.netty.incubator.codec.quic.QuicServerCodecBuilder codecBuilder =
                new io.netty.incubator.codec.quic.QuicServerCodecBuilder();

            codecBuilder.sslContext(sslContext)
                .maxIdleTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(100)
                .handler(initializer);

            bootstrap.group(quicGroup)
                .channel(io.netty.incubator.codec.quic.QuicChannel.class)
                .handler(codecBuilder.build());

            // Bind to UDP port
            quicServerChannel = bootstrap.bind(http3Port).syncUninterruptibly().channel();

            LOGGER.info("QUIC server initialized on UDP port {} for HTTP/3", http3Port);

        } catch (Exception e) {
            LOGGER.error("Failed to initialize QUIC server for HTTP/3", e);
            // Don't throw - gracefully degrade to HTTP/2 only
            this.http3Enabled = false;
            LOGGER.warn("HTTP/3 disabled due to initialization failure. Falling back to HTTP/2 only.");
        }
    }

    /**
     * Check if QUIC support is available.
     *
     * @return true if QUIC classes are available
     */
    private boolean isQuicAvailable() {
        try {
            Class.forName("io.netty.incubator.codec.quic.QuicChannel");
            Class.forName("io.netty.incubator.codec.quic.QuicServerCodecBuilder");
            Class.forName("io.netty.incubator.codec.http3.Http3ServerConnectionHandler");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Create QUIC EventLoopGroup (UDP-based).
     * Uses native Netty EventLoopGroup (not grpc-netty-shaded) for QUIC compatibility.
     *
     * @return EventLoopGroup for QUIC
     */
    private io.netty.channel.EventLoopGroup createQuicEventLoopGroup() {
        int quicThreads = serverConfig.getIoThreads();
        quicThreads = quicThreads <= 0 ? Runtime.getRuntime().availableProcessors() : quicThreads;

        NamedThreadFactory threadName =
            new NamedThreadFactory("SEV-QUIC-" + serverConfig.getPort(), serverConfig.isDaemon());

        // Use native Netty NioEventLoopGroup for QUIC
        // QUIC requires native Netty, not grpc-netty-shaded
        return new io.netty.channel.nio.NioEventLoopGroup(quicThreads, threadName);
    }

    /**
     * Get HTTP/3 port.
     * Uses separate port configuration or TCP port + 1.
     *
     * @return HTTP/3 port
     */
    private int getHttp3Port() {
        Map<String, String> parameters = serverConfig.getParameters();
        if (parameters != null && parameters.containsKey("triple.http3.port")) {
            try {
                return Integer.parseInt(parameters.get("triple.http3.port"));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid triple.http3.port configuration, using default (same as TCP port)");
            }
        }
        return serverConfig.getPort();
    }

    /**
     * Load TripleX configuration from server config.
     */
    protected void loadTripleXConfig() {
        // HTTP/1.1 support - default false (requires explicit configuration)
        Map<String, String> parameters = serverConfig.getParameters();
        this.http1Enabled = parameters != null && "true".equals(parameters.get(HTTP1_ENABLED_KEY));

        // HTTP/3 support - default false (experimental)
        this.http3Enabled = parameters != null && "true".equals(parameters.get(HTTP3_ENABLED_KEY));

        // Port unification - default true
        this.portUnificationEnabled = parameters == null || !"false".equals(parameters.get(PORT_UNIFICATION_KEY));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("TripleX config: http1Enabled={}, http3Enabled={}, portUnificationEnabled={}",
                    http1Enabled, http3Enabled, portUnificationEnabled);
        }
    }

    /**
     * Load HTTP transport listener factories via SPI.
     */
    protected void loadTransportListenerFactories() {
        this.transportListenerFactories = new ArrayList<>();

        try {
            com.alipay.sofa.rpc.ext.ExtensionLoader<HttpServerTransportListenerFactory> loader =
                    ExtensionLoaderFactory.getExtensionLoader(HttpServerTransportListenerFactory.class);
            for (Map.Entry<String, ExtensionClass<HttpServerTransportListenerFactory>> entry : loader.getAllExtensions().entrySet()) {
                HttpServerTransportListenerFactory factory = entry.getValue().getExtInstance();
                if (factory != null) {
                    transportListenerFactories.add(factory);
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Loaded HttpServerTransportListenerFactory: {}", entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load HttpServerTransportListenerFactory extensions", e);
        }
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
            if (started) {
                return;
            }
            try {
                // Start gRPC server if present
                if (server != null) {
                    server.start();
                }

                // Log server status
                if (LOGGER.isInfoEnabled()) {
                    int http3Port = http3Enabled ? getHttp3Port() : -1;
                    LOGGER.info("Start the triple server at port {}, http1Enabled={}, http3Enabled={}, http3Port={}, nettyServerChannel={}, quicServerChannel={}",
                            serverConfig.getPort(), http1Enabled, http3Enabled, http3Port, nettyServerChannel != null, quicServerChannel != null);
                }
                if (EventBus.isEnable(ServerStartedEvent.class)) {
                    EventBus.post(new ServerStartedEvent(serverConfig, bizThreadPool));
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
            // Stop gRPC server if present
            if (server != null) {
                server.shutdown();
            }

            // Stop custom Netty server if present
            if (nettyServerChannel != null) {
                nettyServerChannel.close();
                nettyServerChannel = null;
            }

            // Stop QUIC server if present
            if (quicServerChannel != null) {
                quicServerChannel.close();
                quicServerChannel = null;
            }
            if (quicGroup != null) {
                quicGroup.shutdownGracefully();
                quicGroup = null;
            }
        } catch (Exception e) {
            LOGGER.error("Stop the triple server at port " + serverConfig.getPort() + " error !", e);
        }
        started = false;
    }

    @Override
    public void registerProcessor(ProviderConfig providerConfig, Invoker instance) {
        this.lock.lock();
        try {
            String key = ConfigUniqueNameGenerator.getUniqueName(providerConfig);
            ReflectCache.registerServiceClassLoader(key, providerConfig.getProxyClass().getClassLoader());
            // 缓存接口的方法
            for (Method m : providerConfig.getProxyClass().getMethods()) {
                ReflectCache.putOverloadMethodCache(key, m);
            }
            // wrap invoker to support unique id
            UniqueIdInvoker oldInvoker = this.invokerMap.putIfAbsent(providerConfig.getInterfaceId(), new UniqueIdInvoker());
            if (null != oldInvoker) {
                // this service has already registered into grpc server,
                // we only need register given invoker into unique id invoker.
                if (!oldInvoker.registerInvoker(providerConfig, instance)) {
                    throw new IllegalStateException("Can not expose service with interface:" + providerConfig.getInterfaceId() + " and unique id: " + providerConfig.getUniqueId());
                }
                return;
            }

            UniqueIdInvoker uniqueIdInvoker = this.invokerMap.get(providerConfig.getInterfaceId());
            if (!uniqueIdInvoker.registerInvoker(providerConfig, instance)) {
                throw new IllegalStateException("Can not expose service with interface:" + providerConfig.getInterfaceId() + " and unique id: " + providerConfig.getUniqueId());
            }

            ServerServiceDefinition serviceDefinition = getServerServiceDefinition(providerConfig, uniqueIdInvoker);
            this.serviceInfo.put(providerConfig.getInterfaceId(), serviceDefinition);

            // Also map by gRPC service name for pure HTTP/2 implementation
            String grpcServiceName = serviceDefinition.getServiceDescriptor().getName();
            this.grpcServiceNameInvokerMap.put(grpcServiceName, uniqueIdInvoker);

            ServerServiceDefinition ssd = this.handlerRegistry.addService(serviceDefinition);
            if (ssd != null) {
                throw new IllegalStateException("Can not expose service with same name:" +
                    serviceDefinition.getServiceDescriptor().getName());
            }
        } catch (Exception e) {
            String msg = "Register triple service error";
            LOGGER.error(msg, e);
            throw new SofaRpcRuntimeException(msg, e);
        } finally {
            this.lock.unlock();
        }
    }

    private ServerServiceDefinition getServerServiceDefinition(ProviderConfig providerConfig, UniqueIdInvoker uniqueIdInvoker) {
        // create service definition
        ServerServiceDefinition serviceDef;
        if (SofaProtoUtils.isProtoClass(providerConfig.getRef())) {
            // refer is BindableService
            this.setBindableProxiedImpl(providerConfig, uniqueIdInvoker);
            BindableService bindableService = (BindableService) providerConfig.getRef();
            serviceDef = bindableService.bindService();
        } else {
            GenericServiceImpl genericService = new GenericServiceImpl(uniqueIdInvoker);
            genericService.setProxiedImpl(genericService);
            serviceDef = buildSofaServiceDef(genericService, providerConfig);
        }

        List<TripleServerInterceptor> interceptorList = buildInterceptorChain(serviceDef);
        ServerServiceDefinition serviceDefinition = ServerInterceptors.intercept(
            serviceDef, interceptorList);
        return serviceDefinition;
    }

    private void setBindableProxiedImpl(ProviderConfig providerConfig, Invoker invoker) {
        Class<?> implClass = providerConfig.getRef().getClass();
        try {
            Method method = implClass.getMethod("setProxiedImpl", providerConfig.getProxyClass());
            Object obj = ProxyFactory.buildProxy(providerConfig.getProxy(), providerConfig.getProxyClass(), invoker);
            method.invoke(providerConfig.getRef(), obj);
        } catch (NoSuchMethodException e) {
            LOGGER
                    .info(
                            "{} don't hava method setProxiedImpl, will treated as origin provider service instead of grpc service.",
                            implClass);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to set sofa proxied service impl to stub, please make sure your stub "
                            + "was generated by the sofa-protoc-compiler.", e);
        }
    }

    private ServerServiceDefinition buildSofaServiceDef(GenericServiceImpl genericService,
                                                        ProviderConfig providerConfig) {
        ServerServiceDefinition templateDefinition = genericService.bindService();
        List<MethodDescriptor<Request, Response>> methodDescriptor = getMethodDescriptor(providerConfig);
        List<ServerMethodDefinition<Request, Response>> methodDefs = createMethodDefinition(templateDefinition,methodDescriptor);
        // Bind the actual service to a specific method in the generic service
        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(getServiceDescriptor(
            templateDefinition, providerConfig, methodDescriptor));
        for (ServerMethodDefinition<Request, Response> methodDef : methodDefs) {
            builder.addMethod(methodDef);
        }
        return builder.build();
    }

    private List<ServerMethodDefinition<Request,Response>> createMethodDefinition(ServerServiceDefinition geneticServiceDefinition, List<MethodDescriptor<Request, Response>> serviceMethods){
            Collection<ServerMethodDefinition<?, ?>> genericServiceMethods = geneticServiceDefinition.getMethods();
            List<ServerMethodDefinition<Request,Response>> serverMethodDefinitions = new ArrayList<>();
            //Map ture service method to certain generic service method.
            for (ServerMethodDefinition<?,?> genericMethods : genericServiceMethods){
                for(MethodDescriptor<Request,Response> methodDescriptor : serviceMethods){

                    if(methodDescriptor.getType().equals(genericMethods.getMethodDescriptor().getType())){

                        ServerMethodDefinition<Request,Response> genericMeth = (ServerMethodDefinition<Request, Response>) genericMethods;

                        serverMethodDefinitions.add(
                                ServerMethodDefinition.create(methodDescriptor, genericMeth.getServerCallHandler())
                        );
                    }
                }
            }
            return serverMethodDefinitions;
    }


    private ServiceDescriptor getServiceDescriptor(ServerServiceDefinition template, ProviderConfig providerConfig,
                                                   List<MethodDescriptor<Request, Response>> methodDescriptors) {
        String serviceName = providerConfig.getInterfaceId();
        ServiceDescriptor.Builder builder = ServiceDescriptor.newBuilder(serviceName)
            .setSchemaDescriptor(template.getServiceDescriptor().getSchemaDescriptor());
        for (MethodDescriptor<Request, Response> methodDescriptor : methodDescriptors) {
            builder.addMethod(methodDescriptor);
        }
        return builder.build();

    }

    private List<MethodDescriptor<Request, Response>> getMethodDescriptor(ProviderConfig providerConfig) {
        List<MethodDescriptor<Request, Response>> result = new ArrayList<>();
        Set<String> methodNames = SofaProtoUtils.getMethodNames(providerConfig.getInterfaceId());
        Map<String, String> streamCallTypeMap = SofaProtoUtils.cacheStreamCallType(providerConfig.getProxyClass());
        for (String name : methodNames) {
            MethodDescriptor.MethodType methodType = SofaProtoUtils.mapGrpcCallType(streamCallTypeMap.get(name));
            MethodDescriptor<Request, Response> methodDescriptor = MethodDescriptor.<Request, Response>newBuilder()
                    .setType(methodType)
                    .setFullMethodName(generateFullMethodName(providerConfig.getInterfaceId(), name))
                    .setSampledToLocalTracing(true)
                    .setRequestMarshaller(ProtoUtils.marshaller(
                            Request.getDefaultInstance()))
                    .setResponseMarshaller(ProtoUtils.marshaller(
                            Response.getDefaultInstance()))
                    .build();
            result.add(methodDescriptor);
        }
        return result;
    }

    /**
     * build chain
     *
     * @param serviceDef
     * @return
     */
    protected List<TripleServerInterceptor> buildInterceptorChain(ServerServiceDefinition serviceDef) {
        List<TripleServerInterceptor> interceptorList = new ArrayList<>();
        interceptorList.add(new ServerReqHeaderInterceptor(serviceDef));
        return interceptorList;
    }

    @Override
    public void unRegisterProcessor(ProviderConfig providerConfig, boolean closeIfNoEntry) {
        this.lock.lock();
        cleanReflectCache(providerConfig);
        try {
            ServerServiceDefinition serverServiceDefinition = this.serviceInfo.get(providerConfig.getInterfaceId());
            UniqueIdInvoker uniqueIdInvoker = this.invokerMap.get(providerConfig.getInterfaceId());
            if (null != uniqueIdInvoker) {
                uniqueIdInvoker.unRegisterInvoker(providerConfig);
                if (!uniqueIdInvoker.hasInvoker()) {
                    this.invokerMap.remove(providerConfig.getInterfaceId());
                    this.handlerRegistry.removeService(serverServiceDefinition);
                    this.serviceInfo.remove(providerConfig.getInterfaceId());
                    // Also remove from gRPC service name map
                    if (serverServiceDefinition != null) {
                        String grpcServiceName = serverServiceDefinition.getServiceDescriptor().getName();
                        this.grpcServiceNameInvokerMap.remove(grpcServiceName);
                    }
                }
            } else {
                this.handlerRegistry.removeService(serverServiceDefinition);
            }
        } catch (Exception e) {
            LOGGER.error("Unregister triple service error", e);
        } finally {
            this.lock.unlock();
        }
        if (closeIfNoEntry && invokerMap.isEmpty()) {
            stop();
        }
    }

    public void cleanReflectCache(ProviderConfig providerConfig) {
        String key = ConfigUniqueNameGenerator.getUniqueName(providerConfig);
        ReflectCache.unRegisterServiceClassLoader(key);
        ReflectCache.invalidateMethodCache(key);
        ReflectCache.invalidateMethodSigsCache(key);
        ReflectCache.invalidateOverloadMethodCache(key);
    }

    @Override
    public void destroy() {
        stop();
        server = null;
        nettyServerChannel = null;
        quicServerChannel = null;
        quicGroup = null;
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

    /**
     * 得到业务线程池
     *
     * @return 业务线程池
     */
    public ThreadPoolExecutor getBizThreadPool() {
        return bizThreadPool;
    }

    /**
     * Check if HTTP/1.1 support is enabled.
     *
     * @return true if HTTP/1.1 is enabled
     */
    public boolean isHttp1Enabled() {
        return http1Enabled;
    }

    /**
     * Check if HTTP/3 support is enabled.
     *
     * @return true if HTTP/3 is enabled
     */
    public boolean isHttp3Enabled() {
        return http3Enabled;
    }

    /**
     * Check if port unification is enabled.
     *
     * @return true if port unification is enabled
     */
    public boolean isPortUnificationEnabled() {
        return portUnificationEnabled;
    }

    /**
     * Get the transport listener factories.
     *
     * @return list of transport listener factories
     */
    public List<HttpServerTransportListenerFactory> getTransportListenerFactories() {
        return transportListenerFactories;
    }

    /**
     * Get the invoker map.
     *
     * @return invoker map
     */
    public Map<String, UniqueIdInvoker> getInvokerMap() {
        return invokerMap;
    }

}