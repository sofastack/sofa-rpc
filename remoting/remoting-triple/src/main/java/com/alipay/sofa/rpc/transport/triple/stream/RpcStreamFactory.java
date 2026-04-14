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
package com.alipay.sofa.rpc.transport.triple.stream;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;

/**
 * Factory for creating {@link RpcStream} instances.
 *
 * <p>This SPI-friendly interface allows the transport layer to be replaced or extended
 * without modifying upper layers. For example, an HTTP/3 implementation would provide
 * an {@code Http3RpcStreamFactory} that returns {@code Http3RpcStream} instances,
 * while the codec and call layers remain unchanged.
 *
 * <p>The default implementation creates {@link Http2RpcStream} instances.
 * Future implementations may create HTTP/3 streams, WebSocket streams, etc.
 */
public interface RpcStreamFactory {

    /**
     * The default factory, which creates {@link Http2RpcStream} instances
     * backed by a Netty {@link Http2ConnectionEncoder}.
     */
    RpcStreamFactory HTTP2 = new RpcStreamFactory() {
                               @Override
                               public RpcStream create(ChannelHandlerContext ctx, Http2ConnectionEncoder encoder,
                                                       int streamId) {
                                   return new Http2RpcStream(ctx, encoder, streamId);
                               }
                           };

    /**
     * Creates a new {@link RpcStream} for the given channel context and stream ID.
     *
     * @param ctx      the Netty channel handler context
     * @param encoder  the HTTP/2 encoder (may be null for non-HTTP/2 transports)
     * @param streamId the stream identifier
     * @return a new {@code RpcStream} instance
     */
    RpcStream create(ChannelHandlerContext ctx, Http2ConnectionEncoder encoder, int streamId);
}
