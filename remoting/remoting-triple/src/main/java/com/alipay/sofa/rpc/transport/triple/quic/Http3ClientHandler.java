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

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HTTP/3 client handler for processing responses.
 * Handles HTTP/3 headers and data frames from server.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class Http3ClientHandler extends Http3RequestStreamInboundHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http3ClientHandler.class);

    /**
     * Stream ID generator
     */
    private static final AtomicLong STREAM_ID_GENERATOR = new AtomicLong(0);

    /**
     * Pending responses by stream ID
     */
    private final Map<Long, HttpResponse> pendingResponses = new ConcurrentHashMap<>();

    /**
     * Create a new HTTP/3 client handler.
     */
    public Http3ClientHandler() {
    }

    /**
     * Create a new stream ID for a request.
     *
     * @return new stream ID
     */
    public long newStreamId() {
        return STREAM_ID_GENERATOR.incrementAndGet();
    }

    /**
     * Register a pending response for a stream.
     *
     * @param streamId stream ID
     * @param response HTTP response holder
     */
    public void registerResponse(long streamId, HttpResponse response) {
        pendingResponses.put(streamId, response);
    }

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame, boolean isEnd) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/3 client received headers: status={}, endOfStream={}",
                frame.headers().status(), isEnd);
        }

        // For now, we use a single response holder
        // In a full implementation, we would track by stream ID
        HttpResponse response = getOrCreateResponse(ctx);
        if (response != null) {
            response.setHeaders(frame.headers());
            if (isEnd) {
                response.complete();
            }
        }
    }

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame, boolean isEnd) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HTTP/3 client received  {} bytes, endOfStream={}",
                frame.content().readableBytes(), isEnd);
        }

        try {
            HttpResponse response = getOrCreateResponse(ctx);
            if (response != null) {
                ByteBuf content = frame.content();
                byte[] data = new byte[content.readableBytes()];
                content.readBytes(data);
                response.addData(data);

                if (isEnd) {
                    response.complete();
                }
            }
        } finally {
            ReferenceCountUtil.release(frame);
        }
    }

    /**
     * Get or create response holder for the current stream.
     *
     * @param ctx channel handler context
     * @return HTTP response holder
     */
    private HttpResponse getOrCreateResponse(ChannelHandlerContext ctx) {
        // Use channel attribute to store response
        HttpResponse response = ctx.channel().attr(Http3ClientConnection.RESPONSE_KEY).get();
        if (response == null) {
            response = new HttpResponse();
            ctx.channel().attr(Http3ClientConnection.RESPONSE_KEY).set(response);
        }
        return response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("HTTP/3 client error", cause);
        HttpResponse response = ctx.channel().attr(Http3ClientConnection.RESPONSE_KEY).get();
        if (response != null) {
            response.setError(cause);
        }
        ctx.close();
    }

    /**
     * HTTP response holder.
     */
    public static class HttpResponse {
        private final CountDownLatch latch = new CountDownLatch(1);
        private io.netty.incubator.codec.http3.Http3Headers headers;
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();
        private Throwable error;

        /**
         * Set response headers.
         *
         * @param headers HTTP/3 headers
         */
        public void setHeaders(io.netty.incubator.codec.http3.Http3Headers headers) {
            this.headers = headers;
        }

        /**
         * Get response headers.
         *
         * @return HTTP/3 headers
         */
        public io.netty.incubator.codec.http3.Http3Headers getHeaders() {
            return headers;
        }

        /**
         * Add data to response.
         *
         * @param chunk data chunk
         */
        public void addData(byte[] chunk) {
            try {
                data.write(chunk);
            } catch (Exception e) {
                LOGGER.error("Failed to write response data", e);
            }
        }

        /**
         * Get response data.
         *
         * @return response data
         */
        public byte[] getData() {
            return data.toByteArray();
        }

        /**
         * Complete the response.
         */
        public void complete() {
            latch.countDown();
        }

        /**
         * Set error.
         *
         * @param error error
         */
        public void setError(Throwable error) {
            this.error = error;
            latch.countDown();
        }

        /**
         * Get error.
         *
         * @return error
         */
        public Throwable getError() {
            return error;
        }

        /**
         * Wait for response completion.
         *
         * @param timeout timeout in milliseconds
         * @return true if completed
         */
        public boolean await(long timeout) {
            try {
                return latch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        /**
         * Check if response is complete.
         *
         * @return true if complete
         */
        public boolean isComplete() {
            return latch.getCount() == 0;
        }
    }
}