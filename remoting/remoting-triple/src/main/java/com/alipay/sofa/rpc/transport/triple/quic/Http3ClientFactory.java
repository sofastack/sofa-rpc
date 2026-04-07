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

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicClientCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

/**
 * Factory for creating HTTP/3 client connections.
 * Handles QUIC connection setup and SSL context configuration.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class Http3ClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http3ClientFactory.class);

    /**
     * Create HTTP/3 client connection.
     *
     * @param host server host
     * @param port server port (UDP)
     * @param providerInfo provider info
     * @return HTTP/3 client connection
     * @throws Exception if connection fails
     */
    public static Http3ClientConnection createConnection(String host, int port, ProviderInfo providerInfo)
        throws Exception {
        return createConnection(host, port, providerInfo, true);
    }

    /**
     * Create HTTP/3 client connection.
     *
     * @param host server host
     * @param port server port (UDP)
     * @param providerInfo provider info
     * @param insecure skip certificate verification (for testing)
     * @return HTTP/3 client connection
     * @throws Exception if connection fails
     */
    public static Http3ClientConnection createConnection(String host, int port, ProviderInfo providerInfo,
                                                         boolean insecure) throws Exception {
        LOGGER.info("Creating HTTP/3 connection to {}:{}", host, port);

        // Create SSL context
        QuicSslContext sslContext = createSslContext(insecure);

        // Create response handler
        Http3ClientHandler responseHandler = new Http3ClientHandler();

        // Create EventLoopGroup for QUIC (UDP)
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // Create QUIC client codec (0.0.20.Final API)
            ChannelHandler codecHandler = new QuicClientCodecBuilder()
                .sslContext(sslContext)
                .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(100)
                .build();

            // Create Bootstrap for UDP
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .handler(codecHandler);

            // Bind to local port
            Channel channel = bootstrap.bind(0).sync().channel();

            // Create QUIC channel
            QuicChannel quicChannel = QuicChannel.newBootstrap(channel)
                .handler(new QuicClientChannelInitializer(responseHandler))
                .remoteAddress(new InetSocketAddress(host, port))
                .connect()
                .get(10, TimeUnit.SECONDS);

            LOGGER.info("HTTP/3 connection established to {}:{}", host, port);

            return new Http3ClientConnection(quicChannel, providerInfo, responseHandler, group);
        } catch (Exception e) {
            group.shutdownGracefully();
            throw e;
        }
    }

    /**
     * Create SSL context for QUIC.
     *
     * @param insecure skip certificate verification
     * @return QUIC SSL context
     */
    private static QuicSslContext createSslContext(boolean insecure) {
        try {
            if (insecure) {
                // Create insecure trust manager for testing
                X509TrustManager trustAllManager = new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                };

                // Use the trustManager method that accepts a single TrustManager
                return QuicSslContextBuilder.forClient()
                    .trustManager(trustAllManager)
                    .applicationProtocols("h3")
                    .build();
            } else {
                // Use default trust manager
                return QuicSslContextBuilder.forClient()
                    .applicationProtocols("h3")
                    .build();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create SSL context", e);
            throw new SofaRpcException("Failed to create SSL context: " + e.getMessage(), e);
        }
    }

    /**
     * Check if QUIC is available.
     *
     * @return true if QUIC classes are available
     */
    public static boolean isQuicAvailable() {
        try {
            Class.forName("io.netty.incubator.codec.quic.QuicChannel");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}