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

import com.alipay.sofa.rpc.common.annotation.Unstable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;

import java.util.List;

import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.handler.codec.http2.Http2CodecUtil.connectionPrefaceBuf;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * 
 * @see CleartextHttp2ServerUpgradeHandler
 * @since 5.4.0
 */
@Unstable
public class Http2ServerUpgradeHandler extends ChannelHandlerAdapter {
    private static final ByteBuf           CONNECTION_PREFACE = unreleasableBuffer(connectionPrefaceBuf());

    private final EventLoopGroup           bizGroup;
    private final HttpServerCodec          httpServerCodec;
    private final HttpServerUpgradeHandler httpServerUpgradeHandler;
    private final ChannelHandler           http2ServerHandler;

    /**
     * Creates the channel handler provide cleartext HTTP/2 upgrade from HTTP
     * upgrade or prior knowledge
     *
     * @param bizGroup                 the EventLoopGroup
     * @param httpServerCodec          the http server codec
     * @param httpServerUpgradeHandler the http server upgrade handler for HTTP/2
     * @param http2ServerHandler       the http2 server handler, will be added into pipeline
     *                                 when starting HTTP/2 by prior knowledge
     */
    public Http2ServerUpgradeHandler(EventLoopGroup bizGroup,
                                     HttpServerCodec httpServerCodec,
                                     HttpServerUpgradeHandler httpServerUpgradeHandler,
                                     ChannelHandler http2ServerHandler) {
        this.bizGroup = checkNotNull(bizGroup, "bizGroup");
        this.httpServerCodec = checkNotNull(httpServerCodec, "httpServerCodec");
        this.httpServerUpgradeHandler = checkNotNull(httpServerUpgradeHandler, "httpServerUpgradeHandler");
        this.http2ServerHandler = checkNotNull(http2ServerHandler, "http2ServerHandler");
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline()
            .addBefore(ctx.name(), null, new Http2ServerUpgradeHandler.PriorKnowledgeHandler())
            .addBefore(ctx.name(), "HttpServerCodec", httpServerCodec)
            .replace(this, "HttpServerUpgradeHandler", httpServerUpgradeHandler);
    }

    /**
     * Peek inbound message to determine current connection wants to start HTTP/2
     * by HTTP upgrade or prior knowledge
     */
    private final class PriorKnowledgeHandler extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            int prefaceLength = CONNECTION_PREFACE.readableBytes();
            int bytesRead = Math.min(in.readableBytes(), prefaceLength);

            if (!ByteBufUtil.equals(CONNECTION_PREFACE, CONNECTION_PREFACE.readerIndex(),
                in, in.readerIndex(), bytesRead)) {
                ctx.pipeline().remove(this);
            } else if (bytesRead == prefaceLength) {
                // Full h2 preface match, removed source codec, using http2 codec to handle
                // following network traffic
                ctx.pipeline()
                    .remove(httpServerCodec)
                    .remove(httpServerUpgradeHandler);
                // 用业务线程池
                ctx.pipeline().addAfter(bizGroup, ctx.name(), null, http2ServerHandler);
                ctx.pipeline().remove(this);

                ctx.fireUserEventTriggered(Http2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent.INSTANCE);
            }
        }
    }

    /**
     * User event that is fired to notify about HTTP/2 protocol is started.
     */
    public static final class PriorKnowledgeUpgradeEvent {
        private static final Http2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent INSTANCE = new Http2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent();

        private PriorKnowledgeUpgradeEvent() {
        }
    }
}
