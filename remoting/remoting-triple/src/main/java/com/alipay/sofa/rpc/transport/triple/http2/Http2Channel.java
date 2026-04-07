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
package com.alipay.sofa.rpc.transport.triple.http2;

import com.alipay.sofa.rpc.transport.triple.http.HttpChannel;
import com.alipay.sofa.rpc.transport.triple.http.HttpHeaders;
import com.alipay.sofa.rpc.transport.triple.http.HttpMetadata;
import com.alipay.sofa.rpc.transport.triple.http.HttpOutputMessage;
import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;
import io.grpc.netty.shaded.io.netty.buffer.ByteBuf;
import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2DataFrame;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Headers;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2HeadersFrame;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP/2 implementation of HttpChannel.
 * Provides HTTP/2 response writing capabilities including support for
 * gRPC trailers and streaming responses.
 *
 * <p>Key features:
 * <ul>
 *   <li>HTTP/2 frame writing</li>
 *   <li>gRPC trailers support (grpc-status, grpc-message)</li>
 *   <li>Streaming response support</li>
 *   <li>Connection lifecycle management</li>
 * </ul>
 */
public class Http2Channel implements HttpChannel {

    /**
     * The underlying Netty channel
     */
    private final io.grpc.netty.shaded.io.netty.channel.Channel nettyChannel;

    /**
     * HTTP/2 stream ID
     */
    private final int streamId;

    /**
     * Channel attributes
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Response headers
     */
    private Http2Headers responseHeaders;

    /**
     * Whether headers have been sent (atomic for thread safety)
     */
    private final AtomicBoolean headersSent = new AtomicBoolean(false);

    /**
     * Whether the stream is complete (atomic for thread safety)
     */
    private final AtomicBoolean streamComplete = new AtomicBoolean(false);

    /**
     * Lock for synchronizing header and data writes
     */
    private final Object writeLock = new Object();

    /**
     * Create a new Http2Channel.
     *
     * @param nettyChannel the underlying Netty channel
     * @param streamId the HTTP/2 stream ID
     */
    public Http2Channel(io.grpc.netty.shaded.io.netty.channel.Channel nettyChannel, int streamId) {
        this.nettyChannel = nettyChannel;
        this.streamId = streamId;
        this.responseHeaders = new DefaultHttp2Headers();
    }

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_2;
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata metadata) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        // Convert metadata headers to HTTP/2 headers
        Http2Headers http2Headers = new DefaultHttp2Headers();
        HttpHeaders headers = metadata.headers();
        if (headers != null) {
            for (String name : headers.names()) {
                http2Headers.set(name, headers.get(name));
            }
        }

        // Set default content type for gRPC
        if (!http2Headers.contains("content-type")) {
            http2Headers.set("content-type", "application/grpc");
        }

        // Set status (200 OK for successful responses)
        http2Headers.status("200");

        // Store headers for later use when writing response
        // Note: This does NOT set headersSent - that happens when headers are actually written to network
        this.responseHeaders = http2Headers;
        future.complete(null);
        return future;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage message) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (!nettyChannel.isActive()) {
            future.completeExceptionally(new IllegalStateException("Channel is not active"));
            return future;
        }

        synchronized (writeLock) {
            // Ensure response headers are sent before data
            writeResponseHeadersOnce();

            byte[] body = message.getBody();
            ByteBuf content = body != null ? Unpooled.wrappedBuffer(body) : Unpooled.EMPTY_BUFFER;

            // Write HTTP/2 data frame (properly wrapped)
            Http2DataFrame dataFrame = new DefaultHttp2DataFrame(content, false);

            // Write data and flush together
            nettyChannel.write(dataFrame);
            nettyChannel.flush();
        }

        future.complete(null);
        return future;
    }

    /**
     * Write response data using gRPC frame format.
     * Note: The data should already be gRPC-framed (1 byte compression + 4 bytes length + payload).
     *
     * @param grpcFramedData already gRPC-framed response data
     */
    public void writeResponse(byte[] grpcFramedData) {
        if (!nettyChannel.isActive() || streamComplete.get()) {
            return;
        }

        synchronized (writeLock) {
            // Send response headers first if not sent
            writeResponseHeadersOnce();

            // Data is already gRPC-framed, write directly
            ByteBuf content = Unpooled.wrappedBuffer(grpcFramedData);
            Http2DataFrame dataFrame = new DefaultHttp2DataFrame(content, false);
            nettyChannel.write(dataFrame);
            nettyChannel.flush();
        }
    }

    /**
     * Write response headers for gRPC - ensures headers are only sent once.
     * Uses compareAndSet for thread-safe atomic operation.
     * Uses stored responseHeaders if available, otherwise creates default headers.
     * IMPORTANT: This method must be called within synchronized(writeLock) block.
     */
    private void writeResponseHeadersOnce() {
        if (headersSent.compareAndSet(false, true)) {
            // Use stored response headers if available, otherwise create defaults
            Http2Headers headers = responseHeaders;
            if (headers == null) {
                headers = new DefaultHttp2Headers();
                headers.status("200");
                headers.set("content-type", "application/grpc");
            } else {
                // Ensure status is set even if responseHeaders was provided
                if (!headers.contains(":status")) {
                    headers.status("200");
                }
                if (!headers.contains("content-type")) {
                    headers.set("content-type", "application/grpc");
                }
            }

            Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, false);
            // Write and flush headers immediately
            nettyChannel.writeAndFlush(headersFrame);
        }
    }

    /**
     * Encode data in gRPC frame format.
     * gRPC frame format: 1 byte compression flag + 4 byte length + data
     *
     * @param data the data to encode
     * @return gRPC framed data
     */
    protected byte[] encodeGrpcFrame(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }

        // gRPC frame: 1 byte compression (0 = no compression) + 4 bytes length + data
        byte[] frame = new byte[5 + data.length];
        frame[0] = 0; // No compression

        // Write length as 4 bytes big-endian
        frame[1] = (byte) ((data.length >> 24) & 0xFF);
        frame[2] = (byte) ((data.length >> 16) & 0xFF);
        frame[3] = (byte) ((data.length >> 8) & 0xFF);
        frame[4] = (byte) (data.length & 0xFF);

        // Copy data
        System.arraycopy(data, 0, frame, 5, data.length);

        return frame;
    }

    /**
     * Write gRPC trailers (used for error status).
     *
     * @param status gRPC status code
     * @param message error message (optional)
     */
    public void writeGrpcTrailers(int status, String message) {
        if (!nettyChannel.isActive() || streamComplete.getAndSet(true)) {
            return;
        }

        synchronized (writeLock) {
            // Send response headers first if not sent
            writeResponseHeadersOnce();

            // gRPC trailers are sent as HTTP/2 headers with end-stream flag
            Http2Headers trailers = new DefaultHttp2Headers();
            trailers.set("grpc-status", String.valueOf(status));
            if (message != null && !message.isEmpty()) {
                trailers.set("grpc-message", message);
            }

            // Write trailers with END_STREAM flag
            Http2HeadersFrame trailersFrame = new DefaultHttp2HeadersFrame(trailers, true);
            nettyChannel.write(trailersFrame);
            nettyChannel.flush();
        }
    }

    /**
     * Write response and complete the stream.
     * Note: The data should already be gRPC-framed.
     *
     * @param grpcFramedData already gRPC-framed response data
     */
    public void writeResponseAndComplete(byte[] grpcFramedData) {
        if (!nettyChannel.isActive() || streamComplete.get()) {
            return;
        }

        synchronized (writeLock) {
            // Use atomic operation to ensure this only executes once
            if (streamComplete.getAndSet(true)) {
                return;
            }

            // Send response headers first if not sent
            writeResponseHeadersOnce();

            // Data is already gRPC-framed, write directly
            ByteBuf content = Unpooled.wrappedBuffer(grpcFramedData);
            Http2DataFrame dataFrame = new DefaultHttp2DataFrame(content, false);
            nettyChannel.write(dataFrame);

            // Write trailers with END_STREAM flag
            Http2Headers trailers = new DefaultHttp2Headers();
            trailers.set("grpc-status", "0"); // 0 = OK
            Http2HeadersFrame trailersFrame = new DefaultHttp2HeadersFrame(trailers, true);
            nettyChannel.write(trailersFrame);

            // Flush headers, data, and trailers together
            nettyChannel.flush();
        }
    }

    /**
     * Write error response.
     *
     * @param status gRPC status code
     * @param message error message
     */
    public void writeError(int status, String message) {
        writeGrpcTrailers(status, message);
        close();
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage message, boolean endOfStream) {
        CompletableFuture<Void> future = writeMessage(message);

        if (endOfStream) {
            future = future.thenRun(this::complete);
        }

        return future;
    }

    /**
     * Complete the HTTP/2 stream.
     */
    public void complete() {
        if (streamComplete.compareAndSet(false, true)) {
            synchronized (writeLock) {
                // Write gRPC trailers with OK status if headers were sent
                if (headersSent.get() && nettyChannel.isActive()) {
                    Http2Headers trailers = new DefaultHttp2Headers();
                    trailers.set("grpc-status", "0"); // 0 = OK
                    Http2HeadersFrame trailersFrame = new DefaultHttp2HeadersFrame(trailers, true);
                    nettyChannel.writeAndFlush(trailersFrame);
                }
            }
        }
    }

    @Override
    public HttpOutputMessage newOutputMessage() {
        return new Http2OutputMessage();
    }

    @Override
    public SocketAddress remoteAddress() {
        return nettyChannel.remoteAddress();
    }

    @Override
    public SocketAddress localAddress() {
        return nettyChannel.localAddress();
    }

    @Override
    public void flush() {
        if (nettyChannel.isActive()) {
            nettyChannel.flush();
        }
    }

    @Override
    public boolean isActive() {
        return nettyChannel.isActive();
    }

    @Override
    public boolean isWritable() {
        return nettyChannel.isWritable();
    }

    @Override
    public void close() {
        if (nettyChannel.isActive()) {
            nettyChannel.close();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    @Override
    public <T> void setAttribute(String key, T value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
    }

    /**
     * Get the HTTP/2 stream ID.
     *
     * @return stream ID
     */
    public int getStreamId() {
        return streamId;
    }

    /**
     * Get the underlying Netty channel.
     *
     * @return Netty channel
     */
    public io.grpc.netty.shaded.io.netty.channel.Channel getNettyChannel() {
        return nettyChannel;
    }

    /**
     * Get response headers.
     *
     * @return response headers
     */
    public Http2Headers getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Check if headers have been sent.
     *
     * @return true if headers have been sent
     */
    public boolean isHeadersSent() {
        return headersSent.get();
    }

    /**
     * Check if the stream is complete.
     *
     * @return true if stream is complete
     */
    public boolean isStreamComplete() {
        return streamComplete.get();
    }
}