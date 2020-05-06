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

import com.alipay.sofa.rpc.server.http.HttpServerHandler;
import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public final class Http2ChannelHandlerBuilder
                                             extends
                                             AbstractHttp2ConnectionHandlerBuilder<Http2ServerChannelHandler, Http2ChannelHandlerBuilder> {

    private static final Http2FrameLogger LOGGER = new Http2FrameLogger(LogLevel.DEBUG, HttpServerHandler.class);

    private final HttpServerHandler       serverHandler;

    public Http2ChannelHandlerBuilder(HttpServerHandler serverHandler) {
        frameLogger(LOGGER);
        this.serverHandler = serverHandler;
    }

    @Override
    public Http2ServerChannelHandler build() {
        return super.build();
    }

    @Override
    protected Http2ServerChannelHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                              Http2Settings initialSettings) {
        Http2ServerChannelHandler handler = new Http2ServerChannelHandler(serverHandler, decoder, encoder,
            initialSettings);
        frameListener(handler);
        return handler;
    }
}
