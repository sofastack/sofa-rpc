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

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.http.HttpServerHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsciiString;
import io.netty.util.NetUtil;
import io.netty.util.ReferenceCountUtil;

/**
 * 如果是有ssl，那么由ssl决定是 h2 还是 https <br>
 * 如果没有ssl，那么先放一个协商机制
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class Http2ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * Logger for Http2ServerInitializer
     **/
    private static final Logger     LOGGER = LoggerFactory.getLogger(Http2ServerChannelInitializer.class);

    private final EventLoopGroup    bizGroup;
    private final HttpServerHandler serverHandler;
    private final SslContext        sslCtx;
    private final int               maxHttpContentLength;

    public Http2ServerChannelInitializer(EventLoopGroup bizGroup, SslContext sslCtx,
                                         HttpServerHandler serverHandler, int maxHttpContentLength) {
        if (maxHttpContentLength < 0) {
            throw new IllegalArgumentException("maxHttpContentLength (expected >= 0): " + maxHttpContentLength);
        }
        this.bizGroup = bizGroup;
        this.sslCtx = sslCtx;
        this.maxHttpContentLength = maxHttpContentLength;
        this.serverHandler = serverHandler;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        // 版本协商，参见：https://imququ.com/post/protocol-negotiation-in-http2.html
        if (sslCtx != null) {
            configureSSL(ch);
        } else {
            configureClearText(ch);
        }
    }

    /**
     * Configure the pipeline for TLS NPN negotiation to HTTP/2.
     */
    private void configureSSL(SocketChannel ch) {
        final ChannelPipeline p = ch.pipeline();
        // 先通过 SSL/TLS 协商版本
        p.addLast(sslCtx.newHandler(ch.alloc()));
        // 根据版本加载不同的 ChannelHandler
        p.addLast(new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {
            @Override
            protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
                if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                    ctx.pipeline().addLast(bizGroup, "Http2ChannelHandler",
                        new Http2ChannelHandlerBuilder(serverHandler).build());
                    return;
                }

                if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
                    ctx.pipeline().addLast("HttpServerCodec", new HttpServerCodec());
                    ctx.pipeline().addLast("HttpObjectAggregator", new HttpObjectAggregator(maxHttpContentLength));
                    ctx.pipeline().addLast(bizGroup, "Http1ChannelHandler",
                        new Http1ServerChannelHandler(serverHandler));
                    return;
                }

                throw new IllegalStateException("unknown protocol: " + protocol);
            }
        });
    }

    /**
     * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.0
     */
    private void configureClearText(final SocketChannel ch) {
        final ChannelPipeline p = ch.pipeline();
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec,
            new UpgradeCodecFactory() {
                @Override
                public HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence protocol) {
                    if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                        return new Http2ServerUpgradeCodec(new Http2ChannelHandlerBuilder(serverHandler).build());
                    } else {
                        return null;
                    }
                }
            });
        final Http2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler =
                new Http2ServerUpgradeHandler(bizGroup, sourceCodec, upgradeHandler,
                    new Http2ChannelHandlerBuilder(serverHandler).build());

        // 先通过 HTTP Upgrade 协商版本
        p.addLast("Http2ServerUpgradeHandler", cleartextHttp2ServerUpgradeHandler);
        // 如果没有升级，那就是HTTP/1.1的请求，降级
        p.addLast("HttpDirectTalkingHandler", new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Directly talking: {} (no upgrade was attempted) from {}", msg.protocolVersion(),
                        NetUtil.toSocketAddressString(ch.remoteAddress()));
                }
                ChannelPipeline pipeline = ctx.pipeline();
                ChannelHandlerContext thisCtx = pipeline.context(this);
                // 不需要了
                pipeline.addAfter(bizGroup, thisCtx.name(), "Http1ChannelHandler",
                    new Http1ServerChannelHandler(serverHandler));
                pipeline.replace(this, "HttpObjectAggregator",
                    new HttpObjectAggregator(maxHttpContentLength));
                // HttpServerUpgradeHandler -> HttpServerCodec ->  HttpObjectAggregator -> Http1ChannelHandler, 
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            }
        });
    }

    /**
     * Class that logs any User Events triggered on this channel.
     */
    private static class UserEventLogger extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("User Event Triggered: {}", evt);
            }
            ctx.fireUserEventTriggered(evt);
        }
    }
}
