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

import com.alipay.sofa.rpc.transport.triple.stream.Http2RpcStream;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStream;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStreamFactory;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStreamListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty {@link Http2ConnectionHandler} that routes inbound HTTP/2 frames to the
 * {@link RpcStream} / {@link RpcStreamListener} abstraction layer.
 *
 * <p>For each new inbound HTTP/2 stream, this handler:
 * <ol>
 *   <li>Creates an {@link Http2RpcStream} via the {@link RpcStreamFactory}</li>
 *   <li>Asks the {@link HttpServerTransportListenerFactory} to create a listener for it</li>
 *   <li>Registers the listener on the stream</li>
 *   <li>Dispatches subsequent data frames and stream events to the stream</li>
 * </ol>
 *
 * <p>The factory pattern allows the upper layers (invocation, codec) to be injected
 * at construction time, making this handler independent of the concrete RPC framework.
 *
 * <p>This class has no dependency on {@code grpc-netty-shaded}.
 *
 * @see HttpServerTransportListenerFactory
 * @see Http2RpcStream
 * @see RpcStreamFactory
 */
public class Http2StreamHandler extends Http2ConnectionHandler implements Http2FrameListener {

    private final HttpServerTransportListenerFactory listenerFactory;
    private final RpcStreamFactory                   streamFactory;

    /**
     * Active streams: streamId → Http2RpcStream.
     * Accessed only from the Netty EventLoop, so ConcurrentHashMap is used
     * as a safety measure when diagnostics code accesses from other threads.
     */
    private final Map<Integer, Http2RpcStream>       activeStreams = new ConcurrentHashMap<>();

    /**
     * Creates a new {@code Http2StreamHandler}.
     *
     * @param decoder         the HTTP/2 connection decoder (from the builder)
     * @param encoder         the HTTP/2 connection encoder (from the builder)
     * @param initialSettings the initial HTTP/2 settings
     * @param listenerFactory factory for creating server-side stream listeners
     * @param streamFactory   factory for creating {@link RpcStream} instances
     */
    public Http2StreamHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                              Http2Settings initialSettings,
                              HttpServerTransportListenerFactory listenerFactory,
                              RpcStreamFactory streamFactory) {
        super(decoder, encoder, initialSettings);
        if (listenerFactory == null) {
            throw new IllegalArgumentException("listenerFactory must not be null");
        }
        if (streamFactory == null) {
            throw new IllegalArgumentException("streamFactory must not be null");
        }
        this.listenerFactory = listenerFactory;
        this.streamFactory = streamFactory;
        decoder.frameListener(this);
    }

    /**
     * Convenience constructor using the default {@link RpcStreamFactory#HTTP2} factory.
     *
     * @param decoder         the HTTP/2 connection decoder
     * @param encoder         the HTTP/2 connection encoder
     * @param initialSettings the initial HTTP/2 settings
     * @param listenerFactory factory for creating server-side stream listeners
     */
    public Http2StreamHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                              Http2Settings initialSettings,
                              HttpServerTransportListenerFactory listenerFactory) {
        this(decoder, encoder, initialSettings, listenerFactory, RpcStreamFactory.HTTP2);
    }

    // ── Http2FrameListener callbacks ──────────────────────────────────────────

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                              int padding, boolean endStream) throws Http2Exception {
        Http2RpcStream stream = activeStreams.get(streamId);
        if (stream == null) {
            // First headers frame: create the stream and register a listener
            stream = (Http2RpcStream) streamFactory.create(ctx, encoder(), streamId);
            activeStreams.put(streamId, stream);
            RpcStreamListener listener = listenerFactory.newListener(stream);
            stream.setListener(listener);
        }
        stream.onHeadersReceived(toStringMap(headers), endStream);
        if (endStream) {
            stream.onStreamComplete();
            activeStreams.remove(streamId);
        }
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                              int streamDependency, short weight, boolean exclusive, int padding,
                              boolean endStream) throws Http2Exception {
        onHeadersRead(ctx, streamId, headers, padding, endStream);
    }

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding,
                          boolean endStream) throws Http2Exception {
        Http2RpcStream stream = activeStreams.get(streamId);
        int processed = data.readableBytes() + padding;
        if (stream != null) {
            byte[] bytes = new byte[data.readableBytes()];
            data.readBytes(bytes);
            stream.onDataReceived(bytes);
            if (endStream) {
                stream.onStreamComplete();
                activeStreams.remove(streamId);
            }
        }
        return processed;
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
        Http2RpcStream stream = activeStreams.remove(streamId);
        if (stream != null) {
            stream.onStreamError(new Http2Exception(
                io.netty.handler.codec.http2.Http2Error.valueOf(errorCode),
                "RST_STREAM received on stream " + streamId));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        for (Http2RpcStream stream : activeStreams.values()) {
            stream.onStreamError(cause);
        }
        activeStreams.clear();
        ctx.close();
    }

    // ── Http2FrameListener no-ops ─────────────────────────────────────────────

    @Override
    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency,
                               short weight, boolean exclusive) {
    }

    @Override
    public void onSettingsAckRead(ChannelHandlerContext ctx) {
    }

    @Override
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
    }

    @Override
    public void onPingRead(ChannelHandlerContext ctx, long data) {
    }

    @Override
    public void onPingAckRead(ChannelHandlerContext ctx, long data) {
    }

    @Override
    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
                                  Http2Headers headers, int padding) {
    }

    @Override
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode,
                             ByteBuf debugData) {
    }

    @Override
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {
    }

    @Override
    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId,
                               Http2Flags flags, ByteBuf payload) {
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts Netty {@link Http2Headers} to a plain {@link Map}.
     * Header names are lowercased as required by HTTP/2 (RFC 7540 §8.1.2).
     */
    private static Map<String, String> toStringMap(Http2Headers headers) {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<CharSequence, CharSequence> entry : headers) {
            map.put(entry.getKey().toString().toLowerCase(), entry.getValue().toString());
        }
        return map;
    }

    /**
     * Returns the number of currently active streams (for diagnostics).
     *
     * @return active stream count
     */
    public int activeStreamCount() {
        return activeStreams.size();
    }
}
