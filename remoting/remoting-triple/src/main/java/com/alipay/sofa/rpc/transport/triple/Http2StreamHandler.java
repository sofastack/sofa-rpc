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

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.triple.http.HttpInputMessage;
import com.alipay.sofa.rpc.transport.triple.http.HttpMetadata;
import com.alipay.sofa.rpc.transport.triple.http.HttpTransportListener;
import com.alipay.sofa.rpc.transport.triple.http2.Http2Channel;
import com.alipay.sofa.rpc.transport.triple.http2.Http2InputMessage;
import com.alipay.sofa.rpc.transport.triple.http2.Http2Metadata;
import com.alipay.sofa.rpc.transport.triple.http2.PureHttp2ServerTransportListener;
import io.grpc.netty.shaded.io.netty.buffer.ByteBuf;
import io.grpc.netty.shaded.io.netty.channel.ChannelHandlerContext;
import io.grpc.netty.shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2DataFrame;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Headers;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2HeadersFrame;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2ResetFrame;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2SettingsFrame;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Handler for HTTP/2 streams.
 * Each HTTP/2 stream gets its own instance of this handler.
 * Uses PureHttp2ServerTransportListener for request processing.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class Http2StreamHandler extends ChannelInboundHandlerAdapter {

    private static final Logger                                   LOGGER = LoggerFactory
                                                                             .getLogger(Http2StreamHandler.class);

    private final ServerConfig                                    serverConfig;
    private final Supplier<Map<String, UniqueIdInvoker>>          invokerMapSupplier;
    private final Supplier<Map<String, UniqueIdInvoker>>          grpcServiceNameInvokerMapSupplier;
    private final Executor                                        bizExecutor;

    /**
     * Transport listener for this stream
     */
    private HttpTransportListener<HttpMetadata, HttpInputMessage> transportListener;

    /**
     * HTTP/2 channel for this stream
     */
    private Http2Channel                                          http2Channel;

    /**
     * Create a new HTTP/2 stream handler.
     *
     * @param serverConfig server configuration
     * @param invokerMapSupplier supplier for invoker map
     * @param grpcServiceNameInvokerMapSupplier supplier for gRPC service name to invoker map
     * @param bizExecutor business thread pool executor
     */
    public Http2StreamHandler(ServerConfig serverConfig,
                              Supplier<Map<String, UniqueIdInvoker>> invokerMapSupplier,
                              Supplier<Map<String, UniqueIdInvoker>> grpcServiceNameInvokerMapSupplier,
                              Executor bizExecutor) {
        this.serverConfig = serverConfig;
        this.invokerMapSupplier = invokerMapSupplier;
        this.grpcServiceNameInvokerMapSupplier = grpcServiceNameInvokerMapSupplier;
        this.bizExecutor = bizExecutor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/2 stream active: {}", ctx.channel());
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
            handleHeaders(ctx, headersFrame);
        } else if (msg instanceof Http2DataFrame) {
            Http2DataFrame dataFrame = (Http2DataFrame) msg;
            handleData(ctx, dataFrame);
        } else if (msg instanceof Http2SettingsFrame) {
            // Handle HTTP/2 settings
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTTP/2 settings received: {}", ctx.channel());
            }
        } else if (msg instanceof Http2ResetFrame) {
            // Handle stream reset
            Http2ResetFrame resetFrame = (Http2ResetFrame) msg;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTTP/2 stream reset: {}, error code: {}", ctx.channel(), resetFrame.errorCode());
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * Handle HTTP/2 headers frame.
     *
     * @param ctx channel handler context
     * @param headersFrame headers frame
     */
    private void handleHeaders(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame) {
        Http2Headers headers = headersFrame.headers();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/2 headers received: path={}, method={}",
                headers.path(), headers.method());
        }

        // Get stream ID from the HTTP/2 frame
        int streamId = headersFrame.stream().id();

        // Create HTTP/2 channel for this stream
        http2Channel = new Http2Channel(ctx.channel(), streamId);

        // Create HTTP metadata from HTTP/2 headers
        Http2Metadata metadata = new Http2Metadata(headers, streamId);

        // Get the appropriate invoker for this service
        String path = headers.path() != null ? headers.path().toString() : "/";
        UniqueIdInvoker invoker = getInvokerForPath(path);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/2 request path: {}, invoker found: {}", path, invoker != null);
        }

        // Create and initialize the transport listener
        if (invoker != null) {
            transportListener = new PureHttp2ServerTransportListener(http2Channel, serverConfig, invoker, bizExecutor);
            transportListener.onMetadata(metadata);
        } else {
            LOGGER.warn("No invoker found for path: {}", path);
            // Send 404 error
            http2Channel.writeError(12, "Service not found"); // 12 = UNIMPLEMENTED
            ctx.close();
        }
    }

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
     * Handle HTTP/2 data frame.
     *
     * @param ctx channel handler context
     * @param dataFrame data frame
     */
    private void handleData(ChannelHandlerContext ctx, Http2DataFrame dataFrame) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/2 data received: {} bytes, endOfStream={}",
                dataFrame.content().readableBytes(), dataFrame.isEndStream());
        }

        if (transportListener == null) {
            LOGGER.warn("No transport listener available for data frame");
            dataFrame.release();
            return;
        }

        // Read data from the frame
        ByteBuf content = dataFrame.content();
        byte[] data = new byte[content.readableBytes()];
        content.readBytes(data);

        // Create HTTP input message and pass to transport listener
        Http2InputMessage inputMessage = new Http2InputMessage(data);

        try {
            transportListener.onData(inputMessage);
        } catch (Exception e) {
            LOGGER.error("Error processing HTTP/2 data", e);
            transportListener.onError(e);
        }

        // If end of stream, call onComplete
        if (dataFrame.isEndStream()) {
            transportListener.onComplete();
        }

        // Release the data frame
        dataFrame.release();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/2 stream inactive: {}", ctx.channel());
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("HTTP/2 stream error", cause);
        ctx.close();
    }
}