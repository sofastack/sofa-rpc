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

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.triple.http3.Http3Channel;
import com.alipay.sofa.rpc.transport.triple.http3.Http3InputMessage;
import com.alipay.sofa.rpc.transport.triple.http3.Http3Metadata;
import com.alipay.sofa.rpc.transport.triple.http3.Http3ServerTransportListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * HTTP/3 request handler for processing gRPC-style requests.
 * Handles HTTP/3 headers and data frames, similar to HTTP/2 stream handler.
 *
 * <p>Each HTTP/3 stream is handled independently by this handler.
 * The handler converts HTTP/3 frames to the common HTTP abstraction
 * and delegates to {@link Http3ServerTransportListener} for request processing.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class Http3ServerHandler extends Http3RequestStreamInboundHandler {

    private static final Logger                          LOGGER = LoggerFactory.getLogger(Http3ServerHandler.class);

    // ==================== Fields ====================

    private final ServerConfig                           serverConfig;
    private final Supplier<Map<String, UniqueIdInvoker>> invokerMapSupplier;
    private final Supplier<Map<String, UniqueIdInvoker>> grpcServiceNameInvokerMapSupplier;
    private final Executor                               bizExecutor;

    /**
     * Transport listener for this stream
     */
    private Http3ServerTransportListener                 transportListener;

    /**
     * HTTP/3 channel for this stream
     */
    private Http3Channel                                 http3Channel;

    /**
     * Current request metadata
     */
    private Http3Metadata                                currentMetadata;

    // ==================== Constructor ====================

    /**
     * Create a new HTTP/3 server handler.
     *
     * @param serverConfig                      server configuration
     * @param invokerMapSupplier                supplier for interface-based invoker map
     * @param grpcServiceNameInvokerMapSupplier supplier for gRPC service name invoker map
     * @param bizExecutor                       business thread pool executor
     */
    public Http3ServerHandler(ServerConfig serverConfig,
                              Supplier<Map<String, UniqueIdInvoker>> invokerMapSupplier,
                              Supplier<Map<String, UniqueIdInvoker>> grpcServiceNameInvokerMapSupplier,
                              Executor bizExecutor) {
        this.serverConfig = serverConfig;
        this.invokerMapSupplier = invokerMapSupplier;
        this.grpcServiceNameInvokerMapSupplier = grpcServiceNameInvokerMapSupplier;
        this.bizExecutor = bizExecutor;
    }

    // ==================== Channel Read Handlers ====================

    /**
     * Handle HTTP/3 headers frame.
     *
     * @param ctx    channel handler context
     * @param frame  headers frame
     * @param isEnd  true if this is the end of the stream
     */
    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame, boolean isEnd) {
        Http3Headers headers = frame.headers();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("HTTP/3 headers received: path={}, method={}",
                headers.path(), headers.method());
        }

        // Create HTTP/3 metadata from headers
        currentMetadata = createMetadata(headers);

        // Create HTTP/3 channel for this stream
        http3Channel = new Http3Channel(ctx.channel());

        // Get the appropriate invoker for this service
        String path = headers.path() != null ? headers.path().toString() : "/";
        UniqueIdInvoker invoker = getInvokerForPath(path);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("HTTP/3 request path: {}, invoker found: {}", path, invoker != null);
        }

        if (invoker != null) {
            // Create and initialize the transport listener
            transportListener = new Http3ServerTransportListener(http3Channel, serverConfig, invoker);
            transportListener.onMetadata(currentMetadata);

            // If end of stream, complete the request
            if (isEnd) {
                transportListener.onComplete();
            }
        } else {
            LOGGER.warn("No invoker found for path: {}", path);
            writeErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Service not found");
        }
    }

    /**
     * Handle HTTP/3 data frame.
     *
     * @param ctx    channel handler context
     * @param frame  data frame
     * @param isEnd  true if this is the end of the stream
     */
    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame, boolean isEnd) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/3 data received: {} bytes, endOfStream={}",
                frame.content().readableBytes(), isEnd);
        }

        if (transportListener == null) {
            LOGGER.warn("No transport listener available for data frame");
            ReferenceCountUtil.release(frame);
            return;
        }

        try {
            // Read data from the frame
            ByteBuf content = frame.content();
            byte[] data = new byte[content.readableBytes()];
            content.readBytes(data);

            // Create HTTP input message and pass to transport listener
            Http3InputMessage inputMessage = new Http3InputMessage(data, isEnd);
            transportListener.onData(inputMessage);

            // If end of stream, complete the request
            if (isEnd) {
                transportListener.onComplete();
            }
        } catch (Exception e) {
            LOGGER.error("Error processing HTTP/3 data", e);
            if (transportListener != null) {
                transportListener.onError(e);
            }
        } finally {
            ReferenceCountUtil.release(frame);
        }
    }

    // ==================== Lifecycle Handlers ====================

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/3 stream active: {}", ctx.channel());
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/3 stream inactive: {}", ctx.channel());
        }

        if (transportListener != null) {
            transportListener.onError(new RuntimeException("Stream closed"));
        }

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("HTTP/3 stream error", cause);

        if (transportListener != null) {
            transportListener.onError(cause);
        } else {
            ctx.close();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Get the appropriate invoker for the given path.
     *
     * @param path request path
     * @return UniqueIdInvoker or null
     */
    private UniqueIdInvoker getInvokerForPath(String path) {
        // Extract service name from path: /serviceName/methodName
        String serviceName = extractServiceName(path);
        if (serviceName == null || serviceName.isEmpty()) {
            return null;
        }

        // First, try to find in gRPC service name map (for protobuf services)
        if (grpcServiceNameInvokerMapSupplier != null) {
            Map<String, UniqueIdInvoker> grpcServiceNameMap = grpcServiceNameInvokerMapSupplier.get();
            if (grpcServiceNameMap != null) {
                UniqueIdInvoker invoker = grpcServiceNameMap.get(serviceName);
                if (invoker != null) {
                    return invoker;
                }
            }
        }

        // Fallback to interface ID map (for POJO services)
        if (invokerMapSupplier == null) {
            return null;
        }

        Map<String, UniqueIdInvoker> invokerMap = invokerMapSupplier.get();
        if (invokerMap == null || invokerMap.isEmpty()) {
            return null;
        }

        return invokerMap.get(serviceName);
    }

    /**
     * Extract service name from path.
     *
     * @param path request path
     * @return service name
     */
    private String extractServiceName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0) {
            return path.substring(0, slashIndex);
        }
        return path;
    }

    /**
     * Create HTTP/3 metadata from HTTP/3 headers.
     *
     * @param headers HTTP/3 headers
     * @return HTTP/3 metadata
     */
    private Http3Metadata createMetadata(Http3Headers headers) {
        String method = headers.method() != null ? headers.method().toString() : "POST";
        String path = headers.path() != null ? headers.path().toString() : "/";
        String scheme = headers.scheme() != null ? headers.scheme().toString() : "https";

        Http3Metadata metadata = new Http3Metadata(method, path);
        metadata.setScheme(scheme);

        // Copy headers
        headers.forEach(entry -> {
            metadata.headers().set(entry.getKey().toString(), entry.getValue().toString());
        });

        return metadata;
    }

    /**
     * Write error response.
     *
     * @param ctx     channel handler context
     * @param status  HTTP status
     * @param message error message
     */
    private void writeErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        Http3Headers headers = new DefaultHttp3Headers();
        headers.status(status.codeAsText());
        headers.set("grpc-status", "12"); // UNIMPLEMENTED
        headers.set("grpc-message", message);

        ctx.writeAndFlush(new io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame(headers));
        ctx.close();
    }
}