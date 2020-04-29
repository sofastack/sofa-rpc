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
package com.alipay.sofa.rpc.transport.http;

import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.http.HttpServerHandler;
import com.alipay.sofa.rpc.transport.ServerTransport;
import com.alipay.sofa.rpc.transport.ServerTransportConfig;
import com.alipay.sofa.rpc.transport.netty.NettyHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

import java.net.InetSocketAddress;

/**
 * h2和h2c通用的服务端端传输层
 * 
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public abstract class AbstractHttp2ServerTransport extends ServerTransport {

    /**
     * Logger for Http2ServerTransport
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttp2ServerTransport.class);

    /**
     * 构造函数
     *
     * @param transportConfig 服务端配置
     */
    protected AbstractHttp2ServerTransport(ServerTransportConfig transportConfig) {
        super(transportConfig);
    }

    /**
     * ServerBootstrap
     */
    private volatile ServerBootstrap serverBootstrap;

    /**
     * 业务线程池
     */
    private EventLoopGroup           bizGroup;

    @Override
    public boolean start() {
        if (serverBootstrap != null) {
            return true;
        }
        synchronized (this) {
            if (serverBootstrap != null) {
                return true;
            }
            boolean flag = false;
            SslContext sslCtx = SslContextBuilder.build();

            // Configure the server.
            EventLoopGroup bossGroup = NettyHelper.getServerBossEventLoopGroup(transportConfig);

            HttpServerHandler httpServerHandler = (HttpServerHandler) transportConfig.getServerHandler();
            bizGroup = NettyHelper.getServerBizEventLoopGroup(transportConfig, httpServerHandler.getBizThreadPool());

            serverBootstrap = new ServerBootstrap();

            serverBootstrap.group(bossGroup, bizGroup)
                .channel(transportConfig.isUseEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, transportConfig.getBacklog())
                .option(ChannelOption.SO_REUSEADDR, transportConfig.isReuseAddr())
                .option(ChannelOption.RCVBUF_ALLOCATOR, NettyHelper.getRecvByteBufAllocator())
                .option(ChannelOption.ALLOCATOR, NettyHelper.getByteBufAllocator())
                .childOption(ChannelOption.SO_KEEPALIVE, transportConfig.isKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, transportConfig.isTcpNoDelay())
                .childOption(ChannelOption.SO_RCVBUF, 8192 * 128)
                .childOption(ChannelOption.SO_SNDBUF, 8192 * 128)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childOption(ChannelOption.ALLOCATOR, NettyHelper.getByteBufAllocator())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(
                    transportConfig.getBufferMin(), transportConfig.getBufferMax()))
                .childHandler(new Http2ServerChannelInitializer(bizGroup, sslCtx,
                    httpServerHandler, transportConfig.getPayload()));

            // 绑定到全部网卡 或者 指定网卡
            ChannelFuture future = serverBootstrap.bind(
                new InetSocketAddress(transportConfig.getHost(), transportConfig.getPort()));
            ChannelFuture channelFuture = future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("HTTP/2 Server bind to {}:{} success!",
                                transportConfig.getHost(), transportConfig.getPort());
                        }
                    } else {
                        LOGGER.error(LogCodes.getLog(LogCodes.ERROR_HTTP2_BIND, transportConfig.getHost(),
                            transportConfig.getPort()));
                        stop();
                    }
                }
            });

            try {
                channelFuture.await();
                if (channelFuture.isSuccess()) {
                    flag = Boolean.TRUE;
                } else {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_START_SERVER, "HTTP/2"),
                        future.cause());
                }
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
            return flag;
        }
    }

    @Override
    public void stop() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Shutdown the SOFA RPC HTTP/2 server transport now...");
        }
        NettyHelper.closeServerBossEventLoopGroup(transportConfig);
        if (bizGroup != null) {
            bizGroup.shutdownGracefully();
        }
        serverBootstrap = null;
    }
}
