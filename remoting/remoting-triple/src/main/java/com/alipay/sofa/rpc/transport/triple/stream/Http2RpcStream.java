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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Netty HTTP/2 implementation of {@link RpcStream}.
 *
 * <p>Each instance corresponds to a single HTTP/2 stream, identified by {@code streamId}.
 * Write operations are performed through the {@link Http2ConnectionEncoder}, which must be
 * accessed from within the Netty EventLoop. This class is designed to be used from the
 * Netty pipeline and delegates all EventLoop scheduling to the caller.
 *
 * <p>This class has no dependency on {@code grpc-netty-shaded}.
 *
 * <p>Typical usage (server-side, from Netty handler):
 * <pre>
 *   Http2RpcStream stream = new Http2RpcStream(ctx, encoder, streamId);
 *   stream.setListener(myListener);
 *   // ... listener receives onHeaders / onMessage / onComplete
 *   // Send response:
 *   stream.writeHeaders(responseHeaders, false);
 *   stream.writeMessage(responseBytes, false);
 *   stream.halfClose();
 * </pre>
 */
public class Http2RpcStream implements RpcStream {

    private final ChannelHandlerContext  ctx;
    private final Http2ConnectionEncoder encoder;
    private final int                    streamId;
    private final AtomicBoolean          halfClosed = new AtomicBoolean(false);
    private final AtomicBoolean          cancelled  = new AtomicBoolean(false);

    private volatile RpcStreamListener   listener;

    /**
     * Creates a new {@code Http2RpcStream}.
     *
     * @param ctx      the Netty channel handler context (must not be null)
     * @param encoder  the HTTP/2 connection encoder for writing frames (must not be null)
     * @param streamId the HTTP/2 stream ID for this stream
     */
    public Http2RpcStream(ChannelHandlerContext ctx, Http2ConnectionEncoder encoder, int streamId) {
        this.ctx = ctx;
        this.encoder = encoder;
        this.streamId = streamId;
    }

    @Override
    public void writeMessage(byte[] data, boolean compressed) {
        if (cancelled.get() || halfClosed.get()) {
            return;
        }
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        ChannelPromise promise = ctx.newPromise();
        encoder.writeData(ctx, streamId, buf, 0, false, promise);
        ctx.flush();
    }

    @Override
    public void writeHeaders(Map<String, String> headers, boolean endStream) {
        if (cancelled.get()) {
            return;
        }
        Http2Headers http2Headers = toHttp2Headers(headers);
        ChannelPromise promise = ctx.newPromise();
        encoder.writeHeaders(ctx, streamId, http2Headers, 0, endStream, promise);
        ctx.flush();
    }

    @Override
    public void halfClose() {
        if (halfClosed.compareAndSet(false, true) && !cancelled.get()) {
            ChannelPromise promise = ctx.newPromise();
            encoder.writeData(ctx, streamId, Unpooled.EMPTY_BUFFER, 0, true, promise);
            ctx.flush();
        }
    }

    @Override
    public void cancel(Throwable cause) {
        if (cancelled.compareAndSet(false, true)) {
            ctx.channel().close();
        }
    }

    @Override
    public void setListener(RpcStreamListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.listener = listener;
    }

    @Override
    public void request(int count) {
        // Flow control: update HTTP/2 window.
        // For now this is a no-op placeholder — full flow control implementation
        // belongs in a future Sprint when streaming is integrated end-to-end.
        // The Http2Connection local flow controller handles window updates automatically
        // in most Netty configurations.
    }

    @Override
    public boolean isWritable() {
        return ctx.channel().isWritable();
    }

    /**
     * Returns the HTTP/2 stream ID associated with this {@code RpcStream}.
     *
     * @return the stream ID
     */
    public int getStreamId() {
        return streamId;
    }

    /**
     * Returns the listener registered via {@link #setListener(RpcStreamListener)}.
     * May be {@code null} if no listener has been set.
     *
     * @return the registered listener, or null
     */
    public RpcStreamListener getListener() {
        return listener;
    }

    // ── Inbound event dispatch (called by the Netty handler) ──────────────────

    /**
     * Delivers received headers to the registered listener.
     * Must be called from the Netty EventLoop thread.
     *
     * @param headers   the received HTTP/2 headers
     * @param endStream true if this is also the terminal frame
     */
    public void onHeadersReceived(Map<String, String> headers, boolean endStream) {
        RpcStreamListener l = listener;
        if (l != null) {
            l.onHeaders(headers, endStream);
        }
    }

    /**
     * Delivers a received data frame to the registered listener.
     * Must be called from the Netty EventLoop thread.
     *
     * @param data raw payload bytes (will be copied from the ByteBuf)
     */
    public void onDataReceived(byte[] data) {
        RpcStreamListener l = listener;
        if (l != null) {
            l.onMessage(data);
        }
    }

    /**
     * Notifies the listener that the remote peer has half-closed the stream.
     * Must be called from the Netty EventLoop thread.
     */
    public void onStreamComplete() {
        RpcStreamListener l = listener;
        if (l != null) {
            l.onComplete();
        }
    }

    /**
     * Notifies the listener that the stream has been terminated with an error.
     * Must be called from the Netty EventLoop thread.
     *
     * @param cause the error that terminated the stream
     */
    public void onStreamError(Throwable cause) {
        RpcStreamListener l = listener;
        if (l != null) {
            l.onError(cause);
        }
    }

    /**
     * Notifies the listener that the channel writability has changed.
     * Must be called from the Netty EventLoop thread.
     */
    public void onWritabilityChanged() {
        RpcStreamListener l = listener;
        if (l != null) {
            l.onWritabilityChanged();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Http2Headers toHttp2Headers(Map<String, String> headers) {
        DefaultHttp2Headers http2Headers = new DefaultHttp2Headers();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            http2Headers.add(entry.getKey().toLowerCase(), entry.getValue());
        }
        return http2Headers;
    }
}
