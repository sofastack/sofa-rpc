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
package com.alipay.sofa.rpc.transport.triple;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.triple.http1.Http1ServerHandler;
import io.grpc.netty.shaded.io.netty.buffer.ByteBuf;
import io.grpc.netty.shaded.io.netty.buffer.ByteBufUtil;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.ChannelInitializer;
import io.grpc.netty.shaded.io.netty.channel.ChannelPipeline;
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.SocketChannel;
import io.grpc.netty.shaded.io.netty.handler.codec.ByteToMessageDecoder;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpObjectAggregator;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpServerCodec;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2MultiplexHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static io.grpc.netty.shaded.io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2CodecUtil.connectionPrefaceBuf;

/**
 * Channel initializer for Triple server that supports both HTTP/1.1 and HTTP/2.
 * Uses protocol detection to route requests to appropriate handlers.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class TripleServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger                          LOGGER             = LoggerFactory
                                                                                .getLogger(TripleServerChannelInitializer.class);

    /**
     * HTTP/2 connection preface buffer
     */
    private static final ByteBuf                         CONNECTION_PREFACE = unreleasableBuffer(connectionPrefaceBuf());

    /**
     * Server configuration
     */
    private final ServerConfig                           serverConfig;

    /**
     * Supplier for invoker map (to support dynamic service registration)
     */
    private final Supplier<Map<String, UniqueIdInvoker>> invokerMapSupplier;

    /**
     * Supplier for gRPC service name to invoker map
     */
    private final Supplier<Map<String, UniqueIdInvoker>> grpcServiceNameInvokerMapSupplier;

    /**
     * Business thread pool executor
     */
    private final Executor                               bizExecutor;

    /**
     * Event loop group for business operations
     */
    private final EventLoopGroup                         bizGroup;

    /**
     * Maximum HTTP content length
     */
    private final int                                    maxHttpContentLength;

    /**
     * Whether HTTP/1.1 is enabled
     */
    private final boolean                                http1Enabled;

    /**
     * Create a new channel initializer.
     *
     * @param serverConfig server configuration
     * @param invokerMapSupplier supplier for invoker map
     * @param grpcServiceNameInvokerMapSupplier supplier for gRPC service name to invoker map
     * @param bizExecutor business thread pool executor
     * @param bizGroup event loop group for business operations
     */
    public TripleServerChannelInitializer(ServerConfig serverConfig,
                                          Supplier<Map<String, UniqueIdInvoker>> invokerMapSupplier,
                                          Supplier<Map<String, UniqueIdInvoker>> grpcServiceNameInvokerMapSupplier,
                                          Executor bizExecutor, EventLoopGroup bizGroup) {
        this.serverConfig = serverConfig;
        this.invokerMapSupplier = invokerMapSupplier;
        this.grpcServiceNameInvokerMapSupplier = grpcServiceNameInvokerMapSupplier;
        this.bizExecutor = bizExecutor;
        this.bizGroup = bizGroup;
        this.maxHttpContentLength = RpcConfigs.getIntValue(RpcOptions.TRANSPORT_GRPC_MAX_INBOUND_MESSAGE_SIZE);
        this.http1Enabled = isHttp1Enabled(serverConfig);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        // Add protocol detection handler
        p.addLast("protocolNegotiator", new ProtocolDetectionHandler());
    }

    /**
     * Check if HTTP/1.1 is enabled in server config.
     *
     * @param serverConfig server configuration
     * @return true if HTTP/1.1 is enabled
     */
    private boolean isHttp1Enabled(ServerConfig serverConfig) {
        if (serverConfig.getParameters() == null) {
            return true; // Default enabled
        }
        String http1Enabled = serverConfig.getParameters().get("triple.http1.enabled");
        return http1Enabled == null || !"false".equals(http1Enabled);
    }

    /**
     * Configure HTTP/1.1 handlers.
     *
     * @param ctx channel handler context
     */
    private void configureHttp1(ChannelHandlerContext ctx) {
        if (!http1Enabled) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("HTTP/1.1 request received but HTTP/1.1 support is disabled. Closing connection from {}",
                    ctx.channel().remoteAddress());
            }
            ctx.close();
            return;
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Configuring HTTP/1.1 handlers for {}", ctx.channel().remoteAddress());
        }

        ChannelPipeline pipeline = ctx.pipeline();

        // Add HTTP/1.1 codec
        pipeline.addLast("httpServerCodec", new HttpServerCodec());

        // Add HTTP object aggregator for full HTTP messages
        pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(maxHttpContentLength));

        // Add HTTP/1.1 request handler with dynamic invoker lookup
        pipeline.addLast(bizGroup, "http1Handler",
            new Http1ServerHandler(serverConfig, invokerMapSupplier, bizExecutor));
    }

    /**
     * Configure HTTP/2 handlers.
     *
     * @param ctx channel handler context
     */
    private void configureHttp2(ChannelHandlerContext ctx) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Configuring HTTP/2 handlers for {}", ctx.channel().remoteAddress());
        }

        ChannelPipeline pipeline = ctx.pipeline();

        // Add HTTP/2 frame codec
        // Note: Http2FrameCodec must use the channel's event loop, not a custom executor
        Http2FrameCodecBuilder frameCodecBuilder = Http2FrameCodecBuilder.forServer();
        pipeline.addLast("http2FrameCodec", frameCodecBuilder.build());

        // Add HTTP/2 multiplex handler for streaming
        // Note: Http2MultiplexHandler requires a ChannelInitializer that creates a new handler for each stream
        pipeline.addLast("http2Multiplex", new Http2MultiplexHandler(
            new ChannelInitializer<io.grpc.netty.shaded.io.netty.channel.Channel>() {
                @Override
                protected void initChannel(io.grpc.netty.shaded.io.netty.channel.Channel ch) {
                    // Create a new Http2StreamHandler for each HTTP/2 stream
                    ch.pipeline().addLast(new Http2StreamHandler(serverConfig, invokerMapSupplier,
                        grpcServiceNameInvokerMapSupplier, bizExecutor));
                }
            }));
    }

    /**
     * Protocol detection handler that detects HTTP/1.1 vs HTTP/2.
     */
    private class ProtocolDetectionHandler extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            int prefaceLength = CONNECTION_PREFACE.readableBytes();
            int bytesRead = Math.min(in.readableBytes(), prefaceLength);

            if (bytesRead == 0) {
                // No data yet, wait for more
                return;
            }

            // Check if this is HTTP/2 connection preface
            if (ByteBufUtil.equals(CONNECTION_PREFACE, CONNECTION_PREFACE.readerIndex(),
                in, in.readerIndex(), bytesRead)) {
                if (bytesRead == prefaceLength) {
                    // Full HTTP/2 preface match - configure HTTP/2 handlers
                    configureHttp2(ctx);
                    ctx.pipeline().remove(this);
                }
                // Otherwise wait for more data
            } else {
                // Not HTTP/2 preface - configure HTTP/1.1 handlers
                configureHttp1(ctx);
                ctx.pipeline().remove(this);
            }
        }
    }
}