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

import static com.alipay.sofa.rpc.common.RpcConfigs.getBooleanValue;
import static com.alipay.sofa.rpc.common.RpcOptions.TRANSPORT_CLIENT_H2C_USE_PRIOR_KNOWLEDGE;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2ClientUpgradeCodec;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionPrefaceAndSettingsFrameWrittenEvent;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;

/**
 * Configures the client pipeline to support HTTP/2 frames.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class Http2ClientInitializer extends ChannelInitializer<SocketChannel> {

    private final ClientTransportConfig  transportConfig;
    private HttpToHttp2ConnectionHandler connectionHandler;
    private Http2ClientChannelHandler    responseHandler;
    private Http2SettingsHandler         settingsHandler;

    /**
     * Does the H2C Protocol Use the Prior-Knowledge Method to Start Http2
     */
    private boolean                      useH2cPriorKnowledge = getBooleanValue(TRANSPORT_CLIENT_H2C_USE_PRIOR_KNOWLEDGE);

    public Http2ClientInitializer(ClientTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        final Http2Connection connection = new DefaultHttp2Connection(false);
        connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
            .frameListener(
                new DelegatingDecompressorFrameListener(connection, new InboundHttp2ToHttpAdapterBuilder(connection)
                    .maxContentLength(transportConfig.getPayload()).propagateSettings(true).build()))
            .connection(connection).build();
        responseHandler = new Http2ClientChannelHandler();
        settingsHandler = new Http2SettingsHandler(ch.newPromise());
        String protocol = transportConfig.getProviderInfo().getProtocolType();
        if (RpcConstants.PROTOCOL_TYPE_H2.equals(protocol)) {
            configureSsl(ch);
        } else if (RpcConstants.PROTOCOL_TYPE_H2C.equals(protocol)) {
            if (!useH2cPriorKnowledge) {
                configureClearTextWithHttpUpgrade(ch);
            } else {
                configureClearTextWithPriorKnowledge(ch);
            }
        }
    }

    public Http2ClientChannelHandler responseHandler() {
        return responseHandler;
    }

    public Http2SettingsHandler settingsHandler() {
        return settingsHandler;
    }

    protected void configureEndOfPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(settingsHandler, responseHandler);
    }

    /**
     * Configure the pipeline for TLS NPN negotiation to HTTP/2.
     */
    private void configureSsl(SocketChannel ch) {
        SslContext sslCtx = SslContextBuilder.buildForClient();
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        // We must wait for the handshake to finish and the protocol to be negotiated
        // before configuring
        // the HTTP/2 components of the pipeline.
        pipeline.addLast(new ApplicationProtocolNegotiationHandler("") {
            @Override
            protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
                if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                    ChannelPipeline p = ctx.pipeline();
                    p.addLast(connectionHandler);
                    configureEndOfPipeline(p);
                    return;
                }
                ctx.close();
                throw new IllegalStateException("unknown protocol: " + protocol);
            }
        });
    }

    /**
     * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.
     */
    private void configureClearTextWithHttpUpgrade(SocketChannel ch) {
        HttpClientCodec sourceCodec = new HttpClientCodec();
        Http2ClientUpgradeCodec upgradeCodec = new Http2ClientUpgradeCodec(connectionHandler);
        HttpClientUpgradeHandler upgradeHandler = new HttpClientUpgradeHandler(sourceCodec, upgradeCodec, 65536);

        ch.pipeline().addLast(sourceCodec, upgradeHandler, new UpgradeRequestHandler(), new UserEventLogger());
    }

    /**
     * A handler that triggers the cleartext upgrade to HTTP/2 by sending an initial
     * HTTP request.
     */
    private final class UpgradeRequestHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            DefaultFullHttpRequest upgradeRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                "/");
            ctx.writeAndFlush(upgradeRequest);

            ctx.fireChannelActive();

            // Done with this handler, remove it from the pipeline.
            ctx.pipeline().remove(this);

            configureEndOfPipeline(ctx.pipeline());
        }
    }

    /**
     * Class that logs any User Events triggered on this channel.
     */
    private static class UserEventLogger extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            ctx.fireUserEventTriggered(evt);
        }
    }

    /**
     * Configure the pipeline for a cleartext useing Prior-Knowledge method to start
     * http2.
     */
    private void configureClearTextWithPriorKnowledge(SocketChannel ch) {
        ch.pipeline().addLast(connectionHandler, new PrefaceFrameWrittenEventHandler(), new UserEventLogger());
        configureEndOfPipeline(ch.pipeline());
    }

    /**
     * Class that capture Prior-Knowledge method's Preface frame written Events
     * triggered on this channel, and Flush Channel for send preface frame to remote
     * realy
     */
    private static class PrefaceFrameWrittenEventHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof Http2ConnectionPrefaceAndSettingsFrameWrittenEvent) {
                ctx.flush();
            }
            ctx.fireUserEventTriggered(evt);
        }
    }
}
