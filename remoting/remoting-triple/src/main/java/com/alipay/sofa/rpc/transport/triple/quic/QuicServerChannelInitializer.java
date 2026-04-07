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
package com.alipay.sofa.rpc.transport.triple.quic;

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * QUIC server channel initializer for HTTP/3.
 * Sets up the QUIC connection and HTTP/3 protocol handlers.
 *
 * <p>This initializer creates the HTTP/3 server pipeline:
 * <ol>
 *   <li>HTTP/3 server connection handler</li>
 *   <li>Per-stream HTTP/3 request handler</li>
 * </ol>
 *
 * <p>Each HTTP/3 stream is handled independently by {@link Http3ServerHandler}.
 */
public class QuicServerChannelInitializer extends ChannelInitializer<QuicChannel> {

    private static final Logger                          LOGGER = LoggerFactory
                                                                    .getLogger(QuicServerChannelInitializer.class);

    private final ServerConfig                           serverConfig;
    private final Supplier<Map<String, UniqueIdInvoker>> invokerMapSupplier;
    private final Supplier<Map<String, UniqueIdInvoker>> grpcServiceNameInvokerMapSupplier;
    private final Executor                               bizExecutor;
    private final QuicSslContext                         sslContext;

    /**
     * Create QUIC server channel initializer.
     *
     * @param serverConfig                      server configuration
     * @param invokerMapSupplier                supplier for interface-based invoker map
     * @param grpcServiceNameInvokerMapSupplier supplier for gRPC service name invoker map
     * @param bizExecutor                       business thread pool executor
     * @param sslContext                        QUIC SSL context (TLS 1.3 required)
     */
    public QuicServerChannelInitializer(ServerConfig serverConfig,
                                        Supplier<Map<String, UniqueIdInvoker>> invokerMapSupplier,
                                        Supplier<Map<String, UniqueIdInvoker>> grpcServiceNameInvokerMapSupplier,
                                        Executor bizExecutor,
                                        QuicSslContext sslContext) {
        this.serverConfig = serverConfig;
        this.invokerMapSupplier = invokerMapSupplier;
        this.grpcServiceNameInvokerMapSupplier = grpcServiceNameInvokerMapSupplier;
        this.bizExecutor = bizExecutor;
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(QuicChannel ch) {
        ChannelPipeline p = ch.pipeline();

        LOGGER.debug("Initializing QUIC channel for HTTP/3: {}", ch);

        // Add HTTP/3 server connection handler
        // This automatically handles HTTP/3 control streams and request streams
        Http3ServerConnectionHandler connectionHandler = new Http3ServerConnectionHandler(
            new ChannelInitializer<io.netty.channel.Channel>() {
                @Override
                protected void initChannel(io.netty.channel.Channel streamChannel) {
                    // Each HTTP/3 stream gets its own handler
                    streamChannel.pipeline().addLast(new Http3ServerHandler(
                        serverConfig,
                        invokerMapSupplier,
                        grpcServiceNameInvokerMapSupplier,
                        bizExecutor
                        ));
                }
            }
                );

        p.addLast("http3-connection", connectionHandler);

        LOGGER.debug("QUIC channel initialized for HTTP/3");
    }

    /**
     * Get the SSL context.
     *
     * @return QUIC SSL context
     */
    public QuicSslContext getSslContext() {
        return sslContext;
    }
}