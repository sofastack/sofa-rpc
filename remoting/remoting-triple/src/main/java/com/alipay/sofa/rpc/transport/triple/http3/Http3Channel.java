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
package com.alipay.sofa.rpc.transport.triple.http3;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.triple.http.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP/3 channel implementation for writing responses over QUIC.
 * HTTP/3 uses QUIC transport and has similar semantics to HTTP/2.
 *
 * <h3>Architecture:</h3>
 * <pre>
 *  +------------------+
 *  |   Http3Channel   |
 *  +------------------+
 *          |
 *          +-- writeHeader() -> Write HTTP/3 HEADERS frame
 *          +-- writeMessage() -> Write HTTP/3 DATA frame
 *          +-- writeGrpcTrailers() -> Write gRPC trailers (HEADERS with END_STREAM)
 *          +-- writeError() -> Write error response
 * </pre>
 *
 * <h3>HTTP/3 vs HTTP/2 Differences:</h3>
 * <ul>
 *   <li>HTTP/3 uses QPACK for header compression (instead of HPACK)</li>
 *   <li>HTTP/3 runs over QUIC (UDP) instead of TCP</li>
 *   <li>Stream IDs are 64-bit in QUIC (vs 32-bit in HTTP/2)</li>
 * </ul>
 *
 * <h3>Note:</h3>
 * This implementation uses netty-incubator-codec-http3 classes.
 * When QUIC is not available, this class will fail gracefully.
 */
public class Http3Channel implements HttpChannel {

    // ==================== Constants ====================

    private static final Logger            LOGGER              = LoggerFactory.getLogger(Http3Channel.class);

    /** HTTP status OK */
    private static final String            HTTP_STATUS_OK      = "200";

    /** gRPC content type */
    private static final String            GRPC_CONTENT_TYPE   = "application/grpc";

    /** gRPC status header */
    private static final String            GRPC_STATUS_HEADER  = "grpc-status";

    /** gRPC message header */
    private static final String            GRPC_MESSAGE_HEADER = "grpc-message";

    /** Content-Type header */
    private static final String            CONTENT_TYPE_HEADER = "content-type";

    /** Status header */
    private static final String            STATUS_HEADER       = ":status";

    // ==================== Fields ====================

    /** Underlying Netty channel (HTTP/3 stream channel) */
    private final io.netty.channel.Channel streamChannel;

    /** Whether the channel is active */
    private volatile boolean               active              = true;

    /** Whether headers have been sent (atomic for thread safety) */
    private final AtomicBoolean            headersSent         = new AtomicBoolean(false);

    /** Whether trailers have been sent (atomic for thread safety) */
    private final AtomicBoolean            trailersSent        = new AtomicBoolean(false);

    /** Whether stream is complete (atomic for thread safety) */
    private final AtomicBoolean            streamComplete      = new AtomicBoolean(false);

    // ==================== Constructor ====================

    /**
     * Create a new HTTP/3 channel.
     *
     * @param streamChannel underlying HTTP/3 stream channel
     */
    public Http3Channel(io.netty.channel.Channel streamChannel) {
        this.streamChannel = streamChannel;
    }

    // ==================== HttpChannel Implementation ====================

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_3;
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata metadata) {
        if (!active || headersSent.get()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            io.netty.incubator.codec.http3.Http3Headers http3Headers = convertMetadataToHttp3Headers(metadata);
            ensureContentType(http3Headers);

            io.netty.incubator.codec.http3.Http3HeadersFrame headersFrame =
                new io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame(http3Headers);

            streamChannel.writeAndFlush(headersFrame).addListener(f -> {
                if (f.isSuccess()) {
                    headersSent.set(true);
                    logDebug("HTTP/3 headers written");
                    future.complete(null);
                } else {
                    handleWriteFailure("headers", f.cause(), future);
                }
            });
        } catch (Exception e) {
            handleWriteFailure("headers", e, future);
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage message) {
        return writeMessage(message, false);
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage message, boolean endOfStream) {
        if (!active || streamComplete.get()) {
            return CompletableFuture.completedFuture(null);
        }

        byte[] data = message.getBody();
        if (data == null || data.length == 0) {
            return CompletableFuture.completedFuture(null);
        }

        // Ensure response headers are sent before data
        writeHeadersIfNeeded();

        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            ByteBuf content = Unpooled.wrappedBuffer(data);
            io.netty.incubator.codec.http3.Http3DataFrame dataFrame =
                new io.netty.incubator.codec.http3.DefaultHttp3DataFrame(content);

            if (endOfStream) {
                streamChannel.writeAndFlush(dataFrame).addListener(f -> {
                    if (f.isSuccess()) {
                        logDebug("HTTP/3 data written with END_STREAM, {} bytes", data.length);
                        streamComplete.set(true);
                        future.complete(null);
                    } else {
                        handleWriteFailure("data", f.cause(), future);
                    }
                });
                // Close the stream to signal END_STREAM
                streamChannel.close();
            } else {
                streamChannel.writeAndFlush(dataFrame).addListener(f -> {
                    if (f.isSuccess()) {
                        logDebug("HTTP/3 data written, {} bytes", data.length);
                        future.complete(null);
                    } else {
                        handleWriteFailure("data", f.cause(), future);
                    }
                });
            }
        } catch (Exception e) {
            handleWriteFailure("data", e, future);
        }
        return future;
    }

    @Override
    public HttpOutputMessage newOutputMessage() {
        return new Http3OutputMessage();
    }

    @Override
    public SocketAddress remoteAddress() {
        return streamChannel.remoteAddress();
    }

    @Override
    public SocketAddress localAddress() {
        return streamChannel.localAddress();
    }

    @Override
    public void flush() {
        if (active && streamChannel.isActive()) {
            streamChannel.flush();
        }
    }

    @Override
    public boolean isActive() {
        return active && streamChannel.isActive();
    }

    @Override
    public boolean isWritable() {
        return active && streamChannel.isWritable();
    }

    @Override
    public void close() {
        if (!active) {
            return;
        }

        active = false;
        try {
            streamChannel.close();
            logDebug("HTTP/3 channel closed");
        } catch (Exception e) {
            LOGGER.error("Failed to close HTTP/3 channel", e);
        }
    }

    @Override
    public <T> T getAttribute(String key) {
        return null;
    }

    @Override
    public <T> void setAttribute(String key, T value) {
        // No-op for now
    }

    // ==================== gRPC Specific Methods ====================

    /**
     * Write gRPC trailers.
     * HTTP/3 uses HEADERS frame with END_STREAM flag for trailers.
     *
     * @param status gRPC status code
     * @param message error message (nullable)
     * @return CompletableFuture that completes when trailers are written
     */
    public CompletableFuture<Void> writeGrpcTrailers(int status, String message) {
        if (!active || trailersSent.get() || streamComplete.get()) {
            return CompletableFuture.completedFuture(null);
        }

        // Use atomic operation to ensure trailers are only sent once
        if (trailersSent.getAndSet(true)) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            // Ensure headers are sent before trailers
            writeHeadersIfNeeded();

            io.netty.incubator.codec.http3.Http3Headers trailers = buildGrpcTrailers(status, message);

            // Trailers are sent with END_STREAM flag
            io.netty.incubator.codec.http3.Http3HeadersFrame trailersFrame =
                new io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame(trailers);

            streamChannel.writeAndFlush(trailersFrame).addListener(f -> {
                if (f.isSuccess()) {
                    streamComplete.set(true);
                    logDebug("HTTP/3 gRPC trailers written, status={}", status);
                    // Close stream to signal END_STREAM
                    streamChannel.close();
                    future.complete(null);
                } else {
                    handleWriteFailure("trailers", f.cause(), future);
                }
            });
        } catch (Exception e) {
            handleWriteFailure("trailers", e, future);
        }
        return future;
    }

    /**
     * Write error response.
     *
     * @param status gRPC status code
     * @param message error message
     * @return CompletableFuture that completes when error is written
     */
    public CompletableFuture<Void> writeError(int status, String message) {
        if (!active) {
            return CompletableFuture.completedFuture(null);
        }

        if (headersSent.get()) {
            return writeGrpcTrailers(status, message);
        }

        return writeHeadersThenTrailers(status, message);
    }

    /**
     * Write response data (simplified sync version).
     *
     * @param grpcFramedData gRPC framed data
     */
    public void writeResponse(byte[] grpcFramedData) {
        if (!active) {
            return;
        }

        writeHeadersIfNeeded();
        writeDataSync(grpcFramedData);
    }

    /**
     * Write response and complete the stream.
     *
     * @param grpcFramedData gRPC framed data
     */
    public void writeResponseAndComplete(byte[] grpcFramedData) {
        if (!active) {
            return;
        }

        writeHeadersIfNeeded();
        writeDataSync(grpcFramedData);
        writeGrpcTrailers(0, null);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Convert HttpMetadata to HTTP/3 headers.
     */
    private io.netty.incubator.codec.http3.Http3Headers convertMetadataToHttp3Headers(HttpMetadata metadata) {
        io.netty.incubator.codec.http3.Http3Headers http3Headers =
                new io.netty.incubator.codec.http3.DefaultHttp3Headers();

        // Set status
        http3Headers.status(HTTP_STATUS_OK);

        if (metadata != null && metadata.headers() != null) {
            HttpHeaders headers = metadata.headers();
            for (Map.Entry<String, String> entry : headers) {
                http3Headers.set(entry.getKey(), entry.getValue());
            }
        }
        return http3Headers;
    }

    /**
     * Ensure content-type is set.
     */
    private void ensureContentType(io.netty.incubator.codec.http3.Http3Headers headers) {
        if (!headers.contains(CONTENT_TYPE_HEADER)) {
            headers.set(CONTENT_TYPE_HEADER, GRPC_CONTENT_TYPE);
        }
    }

    /**
     * Build gRPC trailers.
     */
    private io.netty.incubator.codec.http3.Http3Headers buildGrpcTrailers(int status, String message) {
        io.netty.incubator.codec.http3.Http3Headers trailers =
                new io.netty.incubator.codec.http3.DefaultHttp3Headers();
        trailers.set(GRPC_STATUS_HEADER, String.valueOf(status));
        if (message != null) {
            trailers.set(GRPC_MESSAGE_HEADER, message);
        }
        return trailers;
    }

    /**
     * Write headers then trailers (for error response without data).
     */
    private CompletableFuture<Void> writeHeadersThenTrailers(int status, String message) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        io.netty.incubator.codec.http3.Http3Headers headers =
            new io.netty.incubator.codec.http3.DefaultHttp3Headers();
        headers.status(HTTP_STATUS_OK);
        headers.set(CONTENT_TYPE_HEADER, GRPC_CONTENT_TYPE);

        io.netty.incubator.codec.http3.Http3HeadersFrame headersFrame =
            new io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame(headers);

        streamChannel.writeAndFlush(headersFrame).addListener(f -> {
            if (f.isSuccess()) {
                headersSent.set(true);
                writeGrpcTrailers(status, message).whenComplete((v, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        future.complete(null);
                    }
                });
            } else {
                handleWriteFailure("error headers", f.cause(), future);
            }
        });

        return future;
    }

    /**
     * Write headers if not already sent (sync version).
     * Uses compareAndSet for thread-safe atomic operation.
     */
    private void writeHeadersIfNeeded() {
        if (headersSent.compareAndSet(false, true)) {
            io.netty.incubator.codec.http3.Http3Headers headers =
                    new io.netty.incubator.codec.http3.DefaultHttp3Headers();
            headers.status(HTTP_STATUS_OK);
            headers.set(CONTENT_TYPE_HEADER, GRPC_CONTENT_TYPE);

            io.netty.incubator.codec.http3.Http3HeadersFrame headersFrame =
                    new io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame(headers);

            streamChannel.writeAndFlush(headersFrame);
        }
    }

    /**
     * Write data synchronously.
     */
    private void writeDataSync(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }

        ByteBuf content = Unpooled.wrappedBuffer(data);
        io.netty.incubator.codec.http3.Http3DataFrame dataFrame =
                new io.netty.incubator.codec.http3.DefaultHttp3DataFrame(content);
        streamChannel.writeAndFlush(dataFrame);
    }

    /**
     * Handle write failure.
     */
    private void handleWriteFailure(String operation, Throwable cause, CompletableFuture<Void> future) {
        LOGGER.error("Failed to write HTTP/3 {}", operation, cause);
        active = false;
        future.completeExceptionally(cause);
    }

    /**
     * Log debug message.
     */
    private void logDebug(String format, Object... args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format, args);
        }
    }
}