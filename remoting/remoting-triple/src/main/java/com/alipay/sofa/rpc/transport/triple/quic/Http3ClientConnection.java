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

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.transport.triple.client.HttpClientConnection;
import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;
import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamType;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * HTTP/3 client connection implementation.
 * Uses QUIC transport for HTTP/3 requests.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class Http3ClientConnection implements HttpClientConnection {

    private static final Logger                                       LOGGER       = LoggerFactory
                                                                                       .getLogger(Http3ClientConnection.class);

    /**
     * Attribute key for response holder
     */
    public static final AttributeKey<Http3ClientHandler.HttpResponse> RESPONSE_KEY = AttributeKey
                                                                                       .valueOf("http3-response");

    /**
     * QUIC channel
     */
    private final QuicChannel                                         quicChannel;

    /**
     * Provider info
     */
    private final ProviderInfo                                        providerInfo;

    /**
     * HTTP/3 client handler
     */
    private final Http3ClientHandler                                  responseHandler;

    /**
     * Event loop group (for cleanup)
     */
    private final EventLoopGroup                                      eventLoopGroup;

    /**
     * Create HTTP/3 client connection.
     *
     * @param quicChannel QUIC channel
     * @param providerInfo provider info
     * @param responseHandler response handler
     * @param eventLoopGroup event loop group
     */
    public Http3ClientConnection(QuicChannel quicChannel, ProviderInfo providerInfo,
                                 Http3ClientHandler responseHandler, EventLoopGroup eventLoopGroup) {
        this.quicChannel = quicChannel;
        this.providerInfo = providerInfo;
        this.responseHandler = responseHandler;
        this.eventLoopGroup = eventLoopGroup;
    }

    @Override
    public SofaResponse syncSend(SofaRequest request, int timeout) throws Exception {
        if (!isAvailable()) {
            throw new SofaRpcException("HTTP/3 connection is not available");
        }

        // Create response holder
        Http3ClientHandler.HttpResponse httpResponse = new Http3ClientHandler.HttpResponse();

        // Create request stream
        QuicStreamChannel streamChannel = createRequestStream(httpResponse);

        // Send request
        sendRequest(streamChannel, request);

        // Wait for response
        if (!httpResponse.await(timeout)) {
            throw new SofaRpcException("HTTP/3 request timeout");
        }

        // Check for error
        if (httpResponse.getError() != null) {
            throw new SofaRpcException("HTTP/3 request failed: " + httpResponse.getError().getMessage(),
                httpResponse.getError());
        }

        // Parse response
        return parseResponse(httpResponse);
    }

    @Override
    public ResponseFuture asyncSend(SofaRequest request, int timeout) throws Exception {
        // For now, synchronous implementation
        // TODO: Implement true async support
        throw new UnsupportedOperationException("Async send not yet implemented for HTTP/3");
    }

    @Override
    public boolean isAvailable() {
        return quicChannel != null && quicChannel.isActive();
    }

    @Override
    public void close() {
        if (quicChannel != null && quicChannel.isActive()) {
            quicChannel.close();
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_3;
    }

    @Override
    public ProviderInfo getProviderInfo() {
        return providerInfo;
    }

    /**
     * Create a new request stream.
     *
     * @param httpResponse HTTP response holder
     * @return stream channel
     */
    private QuicStreamChannel createRequestStream(Http3ClientHandler.HttpResponse httpResponse) throws Exception {
        // Create a new bidirectional stream for this request
        // The response handler will handle incoming frames
        QuicStreamChannel streamChannel = quicChannel.createStream(
            QuicStreamType.BIDIRECTIONAL,
            new Http3RequestStreamHandler(httpResponse)
            ).sync().getNow();

        return streamChannel;
    }

    /**
     * Send HTTP/3 request.
     *
     * @param streamChannel stream channel
     * @param request SofaRequest
     */
    private void sendRequest(QuicStreamChannel streamChannel, SofaRequest request) {
        // Build HTTP/3 headers
        Http3Headers headers = buildHeaders(request);
        Http3HeadersFrame headersFrame = new DefaultHttp3HeadersFrame(headers);

        // Build request body (gRPC frame format)
        byte[] body = buildRequestBody(request);

        // Write headers
        streamChannel.write(headersFrame);

        // Write data if present
        if (body != null && body.length > 0) {
            ByteBuf content = Unpooled.wrappedBuffer(body);
            DefaultHttp3DataFrame dataFrame = new DefaultHttp3DataFrame(content);
            streamChannel.write(dataFrame);
        }

        // Write trailing headers to end stream
        DefaultHttp3Headers trailingHeaders = new DefaultHttp3Headers();
        Http3HeadersFrame trailingFrame = new DefaultHttp3HeadersFrame(trailingHeaders);
        streamChannel.writeAndFlush(trailingFrame);
    }

    /**
     * Build HTTP/3 headers from SofaRequest.
     *
     * @param request SofaRequest
     * @return HTTP/3 headers
     */
    private Http3Headers buildHeaders(SofaRequest request) {
        DefaultHttp3Headers headers = new DefaultHttp3Headers();

        // Set HTTP/3 pseudo-headers
        headers.method("POST");
        headers.scheme("https");
        headers.path("/" + request.getInterfaceName() + "/" + request.getMethodName());
        headers.authority(getAuthority());

        // Set gRPC headers
        headers.set("content-type", "application/grpc");
        headers.set("te", "trailers");
        headers.set("grpc-encoding", "identity");
        headers.set("grpc-accept-encoding", "gzip");

        // Set custom headers
        if (request.getRequestProps() != null) {
            request.getRequestProps().forEach((key, value) -> {
                if (value != null) {
                    headers.set(key, value.toString());
                }
            });
        }

        return headers;
    }

    /**
     * Get authority (host:port).
     *
     * @return authority
     */
    private String getAuthority() {
        if (providerInfo != null) {
            return providerInfo.getHost() + ":" + providerInfo.getPort();
        }
        InetSocketAddress remoteAddress = (InetSocketAddress) quicChannel.remoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getHostString() + ":" + remoteAddress.getPort();
        }
        return "localhost";
    }

    /**
     * Build request body in gRPC frame format.
     *
     * @param request SofaRequest
     * @return request body
     */
    private byte[] buildRequestBody(SofaRequest request) {
        // Build Triple Request protobuf
        triple.Request.Builder requestBuilder = triple.Request.newBuilder();
        requestBuilder.setSerializeType("hessian2");

        // Add arguments
        if (request.getMethodArgs() != null) {
            for (Object arg : request.getMethodArgs()) {
                if (arg != null) {
                    requestBuilder.addArgTypes(arg.getClass().getName());
                    // TODO: Serialize argument
                    // For now, use empty bytes
                    requestBuilder.addArgs(ByteString.EMPTY);
                }
            }
        }

        byte[] requestBytes = requestBuilder.build().toByteArray();

        // Wrap in gRPC frame format
        return encodeGrpcFrame(requestBytes);
    }

    /**
     * Encode data in gRPC frame format.
     *
     * @param data data to encode
     * @return gRPC framed data
     */
    private byte[] encodeGrpcFrame(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }

        byte[] frame = new byte[5 + data.length];
        frame[0] = 0; // No compression

        // Write length as 4 bytes big-endian
        frame[1] = (byte) ((data.length >> 24) & 0xFF);
        frame[2] = (byte) ((data.length >> 16) & 0xFF);
        frame[3] = (byte) ((data.length >> 8) & 0xFF);
        frame[4] = (byte) (data.length & 0xFF);

        System.arraycopy(data, 0, frame, 5, data.length);

        return frame;
    }

    /**
     * Parse HTTP/3 response to SofaResponse.
     *
     * @param httpResponse HTTP response
     * @return SofaResponse
     */
    private SofaResponse parseResponse(Http3ClientHandler.HttpResponse httpResponse) {
        SofaResponse response = new SofaResponse();

        // Check gRPC status in trailers
        Http3Headers headers = httpResponse.getHeaders();
        if (headers != null) {
            CharSequence grpcStatus = headers.get("grpc-status");
            if (grpcStatus != null && !"0".equals(grpcStatus.toString())) {
                CharSequence grpcMessage = headers.get("grpc-message");
                response.setErrorMsg(grpcMessage != null ? grpcMessage.toString() : "Unknown error");
                return response;
            }
        }

        // Parse response data
        byte[] data = httpResponse.getData();
        if (data != null && data.length > 0) {
            try {
                // Decode gRPC frame
                byte[] payload = decodeGrpcFrame(data);

                // Parse Triple Response
                triple.Response tripleResponse = triple.Response.parseFrom(payload);

                // TODO: Deserialize response data
                // For now, return the raw data
                response.setAppResponse(tripleResponse.getData().toByteArray());
            } catch (Exception e) {
                LOGGER.error("Failed to parse HTTP/3 response", e);
                response.setErrorMsg("Failed to parse response: " + e.getMessage());
            }
        }

        return response;
    }

    /**
     * Decode gRPC frame.
     *
     * @param data framed data
     * @return payload
     */
    private byte[] decodeGrpcFrame(byte[] data) {
        if (data == null || data.length < 5) {
            return data;
        }

        int length = ((data[1] & 0xFF) << 24) |
            ((data[2] & 0xFF) << 16) |
            ((data[3] & 0xFF) << 8) |
            (data[4] & 0xFF);

        if (data.length >= 5 + length) {
            byte[] payload = new byte[length];
            System.arraycopy(data, 5, payload, 0, length);
            return payload;
        }

        return data;
    }

    /**
     * HTTP/3 request stream handler for handling responses.
     */
    private static class Http3RequestStreamHandler extends Http3RequestStreamInboundHandler {
        private final Http3ClientHandler.HttpResponse httpResponse;

        Http3RequestStreamHandler(Http3ClientHandler.HttpResponse httpResponse) {
            this.httpResponse = httpResponse;
        }

        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame, boolean isEnd) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTTP/3 client received headers: status={}, endOfStream={}",
                    frame.headers().status(), isEnd);
            }

            httpResponse.setHeaders(frame.headers());
            if (isEnd) {
                httpResponse.complete();
            }
        }

        @Override
        protected void channelRead(ChannelHandlerContext ctx, io.netty.incubator.codec.http3.Http3DataFrame frame,
                                   boolean isEnd) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTTP/3 client received {} bytes, endOfStream={}",
                    frame.content().readableBytes(), isEnd);
            }

            try {
                ByteBuf content = frame.content();
                byte[] data = new byte[content.readableBytes()];
                content.readBytes(data);
                httpResponse.addData(data);

                if (isEnd) {
                    httpResponse.complete();
                }
            } finally {
                frame.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOGGER.error("HTTP/3 client error", cause);
            httpResponse.setError(cause);
            ctx.close();
        }
    }
}