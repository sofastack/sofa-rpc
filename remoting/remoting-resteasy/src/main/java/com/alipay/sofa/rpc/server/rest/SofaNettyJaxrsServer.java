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

import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutor;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedJaxrsServer;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.plugins.server.netty.RequestDispatcher;
import org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder;
import org.jboss.resteasy.plugins.server.netty.RestEasyHttpResponseEncoder;
import org.jboss.resteasy.spi.ResteasyDeployment;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder.Protocol.HTTP;
import static org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder.Protocol.HTTPS;

/**
 * 参考NettyJaxrsServer的实现，增加了自定义功能，区别搜索 CHANGE<br>
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @see org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer
 */
public class SofaNettyJaxrsServer implements EmbeddedJaxrsServer {

    private final ServerConfig         serverConfig;
    protected ServerBootstrap          bootstrap           = null;
    protected String                   hostname            = null;
    protected int                      port                = 8080;
    protected ResteasyDeployment       deployment          = new SofaResteasyDeployment(); // CHANGE: 使用sofa的类
    protected String                   root                = "";
    protected SecurityDomain           domain;
    private EventLoopGroup             eventLoopGroup;
    private EventLoopGroup             eventExecutor;
    private int                        ioWorkerCount       = SystemInfo.getCpuCores() * 2; // CHANGE:cpu计算修改
    private int                        executorThreadCount = 16;
    private SSLContext                 sslContext;
    private int                        maxRequestSize      = 1024 * 1024 * 10;
    private int                        backlog             = 128;
    private List<ChannelHandler>       channelHandlers     = Collections.emptyList();
    private Map<ChannelOption, Object> channelOptions      = Collections.emptyMap();
    private Map<ChannelOption, Object> childChannelOptions = Collections.emptyMap();
    private List<ChannelHandler>       httpChannelHandlers = Collections.emptyList();

    /**
     * Build SofaNettyJaxrsServer
     *
     * @param serverConfig ServerConfig
     */
    public SofaNettyJaxrsServer(ServerConfig serverConfig) {
        if (serverConfig == null) {
            throw new IllegalArgumentException("server config is null");
        }
        this.serverConfig = serverConfig;
    }

    public void setSSLContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * Specify the worker count to use. For more information about this please see the javadocs of {@link EventLoopGroup}
     *
     * @param ioWorkerCount ioWorkerCount
     */
    public void setIoWorkerCount(int ioWorkerCount) {
        this.ioWorkerCount = ioWorkerCount;
    }

    /**
     * Set the number of threads to use for the EventExecutor. For more information please see the javadocs of {@link EventExecutor}.
     * If you want to disable the use of the {@link EventExecutor} specify a value <= 0.  This should only be done if you are 100% sure that you don't have any blocking
     * code in there.
     *
     * @param executorThreadCount executorThreadCount
     */
    public void setExecutorThreadCount(int executorThreadCount) {
        this.executorThreadCount = executorThreadCount;
    }

    /**
     * Set the max. request size in bytes. If this size is exceed we will send a "413 Request Entity Too Large" to the client.
     *
     * @param maxRequestSize the max request size. This is 10mb by default.
     */
    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    /**
     * Add additional {@link io.netty.channel.ChannelHandler}s to the {@link io.netty.bootstrap.ServerBootstrap}.
     * <p>The additional channel handlers are being added <em>before</em> the HTTP handling.</p>
     *
     * @param channelHandlers the additional {@link io.netty.channel.ChannelHandler}s.
     */
    public void setChannelHandlers(final List<ChannelHandler> channelHandlers) {
        this.channelHandlers = channelHandlers == null ? Collections.<ChannelHandler> emptyList() : channelHandlers;
    }

    /**
     * Add additional {@link io.netty.channel.ChannelHandler}s to the {@link io.netty.bootstrap.ServerBootstrap}.
     * <p>The additional channel handlers are being added <em>after</em> the HTTP handling.</p>
     *
     * @param httpChannelHandlers the additional {@link io.netty.channel.ChannelHandler}s.
     */
    public void setHttpChannelHandlers(final List<ChannelHandler> httpChannelHandlers) {
        this.httpChannelHandlers = httpChannelHandlers == null ? Collections.<ChannelHandler> emptyList()
            : httpChannelHandlers;
    }

    /**
     * Add Netty {@link io.netty.channel.ChannelOption}s to the {@link io.netty.bootstrap.ServerBootstrap}.
     *
     * @param channelOptions the additional {@link io.netty.channel.ChannelOption}s.
     * @see io.netty.bootstrap.ServerBootstrap#option(io.netty.channel.ChannelOption, Object)
     */
    public void setChannelOptions(final Map<ChannelOption, Object> channelOptions) {
        this.channelOptions = channelOptions == null ? Collections.<ChannelOption, Object> emptyMap() : channelOptions;
    }

    /**
     * Add child options to the {@link io.netty.bootstrap.ServerBootstrap}.
     *
     * @param channelOptions the additional child {@link io.netty.channel.ChannelOption}s.
     * @see io.netty.bootstrap.ServerBootstrap#childOption(io.netty.channel.ChannelOption, Object)
     */
    public void setChildChannelOptions(final Map<ChannelOption, Object> channelOptions) {
        this.childChannelOptions = channelOptions == null ? Collections.<ChannelOption, Object> emptyMap()
            : channelOptions;
    }

    @Override
    public void setDeployment(ResteasyDeployment deployment) {
        this.deployment = deployment;
    }

    @Override
    public void setRootResourcePath(String rootResourcePath) {
        root = rootResourcePath;
        if (root != null && StringUtils.CONTEXT_SEP.equals(root)) {
            root = "";
        }
    }

    @Override
    public ResteasyDeployment getDeployment() {
        return deployment;
    }

    @Override
    public void setSecurityDomain(SecurityDomain sc) {
        this.domain = sc;
    }

    protected RequestDispatcher createRequestDispatcher() {
        return new RequestDispatcher((SynchronousDispatcher) deployment.getDispatcher(),
            deployment.getProviderFactory(), domain);
    }

    @Override
    public void start() {
        // CHANGE: 增加线程名字
        boolean daemon = serverConfig.isDaemon();
        boolean isEpoll = serverConfig.isEpoll();
        NamedThreadFactory ioFactory = new NamedThreadFactory("SEV-REST-IO-" + port, daemon);
        NamedThreadFactory bizFactory = new NamedThreadFactory("SEV-REST-BIZ-" + port, daemon);
        eventLoopGroup = isEpoll ? new EpollEventLoopGroup(ioWorkerCount, ioFactory)
            : new NioEventLoopGroup(ioWorkerCount, ioFactory);
        eventExecutor = isEpoll ? new EpollEventLoopGroup(executorThreadCount, bizFactory)
            : new NioEventLoopGroup(executorThreadCount, bizFactory);
        // Configure the server.
        bootstrap = new ServerBootstrap()
            .group(eventLoopGroup)
            .channel(isEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
            .childHandler(createChannelInitializer())
            .option(ChannelOption.SO_BACKLOG, backlog)
            .childOption(ChannelOption.SO_KEEPALIVE, serverConfig.isKeepAlive()); // CHANGE: setKeepAlive

        for (Map.Entry<ChannelOption, Object> entry : channelOptions.entrySet()) {
            bootstrap.option(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<ChannelOption, Object> entry : childChannelOptions.entrySet()) {
            bootstrap.childOption(entry.getKey(), entry.getValue());
        }

        final InetSocketAddress socketAddress;
        if (null == hostname || hostname.isEmpty()) {
            socketAddress = new InetSocketAddress(port);
        } else {
            socketAddress = new InetSocketAddress(hostname, port);
        }

        bootstrap.bind(socketAddress).syncUninterruptibly();
    }

    private ChannelInitializer<SocketChannel> createChannelInitializer() {
        final RequestDispatcher dispatcher = createRequestDispatcher();
        if (sslContext == null) {
            return new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    setupHandlers(ch, dispatcher, HTTP);
                }
            };
        } else {
            final SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            return new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addFirst(new SslHandler(engine));
                    setupHandlers(ch, dispatcher, HTTPS);
                }
            };
        }
    }

    private void setupHandlers(SocketChannel ch, RequestDispatcher dispatcher,
                               RestEasyHttpRequestDecoder.Protocol protocol) {
        ChannelPipeline channelPipeline = ch.pipeline();
        channelPipeline.addLast(channelHandlers.toArray(new ChannelHandler[channelHandlers.size()]));
        channelPipeline.addLast(new HttpRequestDecoder());
        channelPipeline.addLast(new HttpObjectAggregator(maxRequestSize));
        channelPipeline.addLast(new HttpResponseEncoder());
        channelPipeline.addLast(httpChannelHandlers.toArray(new ChannelHandler[httpChannelHandlers.size()]));
        channelPipeline.addLast(new RestEasyHttpRequestDecoder(dispatcher.getDispatcher(), root, protocol));
        channelPipeline.addLast(new RestEasyHttpResponseEncoder());
        channelPipeline.addLast(eventExecutor, new SofaRestRequestHandler(dispatcher)); // CHANGE: 用sofa的处理类
    }

    @Override
    public void stop() {
        try {
            eventLoopGroup.shutdownGracefully().sync();
            eventExecutor.shutdownGracefully().sync();
        } catch (Exception ignore) { // NOPMD
        }
        bootstrap = null;
    }
}
