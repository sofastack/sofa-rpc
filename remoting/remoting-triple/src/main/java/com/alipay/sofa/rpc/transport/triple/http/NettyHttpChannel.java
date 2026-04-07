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
package com.alipay.sofa.rpc.transport.triple.http;

import com.alipay.sofa.rpc.transport.triple.http1.Http1OutputMessage;
import io.grpc.netty.shaded.io.netty.buffer.ByteBuf;
import io.grpc.netty.shaded.io.netty.buffer.Unpooled;
import io.grpc.netty.shaded.io.netty.channel.ChannelFuture;
import io.grpc.netty.shaded.io.netty.channel.ChannelFutureListener;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.DefaultHttpHeaders;
import io.grpc.netty.shaded.io.netty.handler.codec.http.FullHttpResponse;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaderNames;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpHeaders;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpResponseStatus;
import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpVersion;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty-based implementation of HttpChannel.
 * Wraps a Netty Channel to provide HTTP/1.1 response writing capabilities.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class NettyHttpChannel implements HttpChannel {

    /**
     * The underlying Netty channel
     */
    private final io.grpc.netty.shaded.io.netty.channel.Channel nettyChannel;

    /**
     * HTTP version for this channel
     */
    private final HttpVersion httpVersion;

    /**
     * Channel attributes
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Response headers to be sent
     */
    private HttpHeaders responseHeaders;

    /**
     * HTTP status code for response
     */
    private HttpResponseStatus responseStatus = HttpResponseStatus.OK;

    /**
     * Whether headers have been sent
     */
    private boolean headersSent = false;

    /**
     * Create a new NettyHttpChannel.
     *
     * @param nettyChannel the underlying Netty channel
     */
    public NettyHttpChannel(io.grpc.netty.shaded.io.netty.channel.Channel nettyChannel) {
        this.nettyChannel = nettyChannel;
        this.httpVersion = HttpVersion.HTTP_1_1;
        this.responseHeaders = new DefaultHttpHeaders();
    }

    /**
     * Create a new NettyHttpChannel with specified HTTP version.
     *
     * @param nettyChannel the underlying Netty channel
     * @param httpVersion HTTP version
     */
    public NettyHttpChannel(io.grpc.netty.shaded.io.netty.channel.Channel nettyChannel, HttpVersion httpVersion) {
        this.nettyChannel = nettyChannel;
        this.httpVersion = httpVersion;
        this.responseHeaders = new DefaultHttpHeaders();
    }

    @Override
    public com.alipay.sofa.rpc.transport.triple.http.HttpVersion httpVersion() {
        return com.alipay.sofa.rpc.transport.triple.http.HttpVersion.HTTP_1;
    }

    @Override
    public CompletableFuture<Void> writeHeader(HttpMetadata metadata) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (headersSent) {
            future.complete(null);
            return future;
        }

        // Copy headers from metadata
        com.alipay.sofa.rpc.transport.triple.http.HttpHeaders headers = metadata.headers();
        if (headers != null) {
            for (String name : headers.names()) {
                responseHeaders.set(name, headers.get(name));
            }
        }

        headersSent = true;
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

        byte[] body = message.getBody();
        ByteBuf content = body != null ? Unpooled.wrappedBuffer(body) : Unpooled.EMPTY_BUFFER;

        // Create HTTP response
        FullHttpResponse response = new DefaultFullHttpResponse(
                httpVersion, responseStatus, content);

        // Set response headers
        response.headers().setAll(responseHeaders);

        // Set content length if not already set
        if (!response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        }

        // Write response
        nettyChannel.writeAndFlush(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) {
                if (channelFuture.isSuccess()) {
                    future.complete(null);
                } else {
                    future.completeExceptionally(channelFuture.cause());
                }
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> writeMessage(HttpOutputMessage message, boolean endOfStream) {
        CompletableFuture<Void> future = writeMessage(message);

        if (endOfStream) {
            future = future.thenRun(() -> {
                // Connection will be closed after response if needed
            });
        }

        return future;
    }

    @Override
    public HttpOutputMessage newOutputMessage() {
        return new Http1OutputMessage();
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
     * Get the underlying Netty channel.
     *
     * @return Netty channel
     */
    public io.grpc.netty.shaded.io.netty.channel.Channel getNettyChannel() {
        return nettyChannel;
    }

    /**
     * Set the response status.
     *
     * @param status HTTP response status
     */
    public void setResponseStatus(HttpResponseStatus status) {
        this.responseStatus = status;
    }

    /**
     * Get the response headers.
     *
     * @return response headers
     */
    public HttpHeaders getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Add a response header.
     *
     * @param name header name
     * @param value header value
     */
    public void addResponseHeader(String name, String value) {
        responseHeaders.add(name, value);
    }

    /**
     * Set a response header.
     *
     * @param name header name
     * @param value header value
     */
    public void setResponseHeader(String name, String value) {
        responseHeaders.set(name, value);
    }
}