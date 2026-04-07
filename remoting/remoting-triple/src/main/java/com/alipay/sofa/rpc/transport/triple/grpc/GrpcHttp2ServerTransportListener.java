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
package com.alipay.sofa.rpc.transport.triple.grpc;

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.triple.http.*;

import java.util.concurrent.Executor;

/**
 * gRPC-compatible HTTP/2 server transport listener.
 * Maintains backward compatibility with existing gRPC clients.
 * This class wraps the existing gRPC-based implementation.
 */
public class GrpcHttp2ServerTransportListener
                                             implements HttpTransportListener<HttpMetadata, HttpInputMessage> {

    private static final String     GRPC_CONTENT_TYPE = "application/grpc";

    protected final HttpChannel     httpChannel;
    protected final ServerConfig    serverConfig;
    protected final UniqueIdInvoker invoker;
    protected Executor              executor;
    protected HttpMetadata          httpMetadata;
    protected HttpMessageListener   messageListener;

    public GrpcHttp2ServerTransportListener(HttpChannel httpChannel, ServerConfig serverConfig,
                                            UniqueIdInvoker invoker) {
        this(httpChannel, serverConfig, invoker, null);
    }

    public GrpcHttp2ServerTransportListener(HttpChannel httpChannel, ServerConfig serverConfig,
                                            UniqueIdInvoker invoker, Executor bizExecutor) {
        this.httpChannel = httpChannel;
        this.serverConfig = serverConfig;
        this.invoker = invoker;
        this.executor = bizExecutor;
    }

    @Override
    public void onMetadata(HttpMetadata metadata) {
        this.httpMetadata = metadata;
        // Build message listener
        messageListener = buildMessageListener();
    }

    @Override
    public void onData(HttpInputMessage message) {
        if (executor == null || messageListener == null) {
            return;
        }

        executor.execute(() -> {
            try {
                messageListener.onMessage(message.getBody());
            } catch (Throwable t) {
                onError(t);
            } finally {
                try {
                    message.close();
                } catch (Exception e) {
                    onError(e);
                }
            }
        });
    }

    @Override
    public void onComplete() {
        // Request complete, finalize processing
    }

    @Override
    public void onError(Throwable throwable) {
        // Handle error
        if (httpChannel != null) {
            httpChannel.close();
        }
    }

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_2;
    }

    /**
     * Build message listener for processing request data.
     *
     * @return message listener
     */
    protected HttpMessageListener buildMessageListener() {
        SofaRequest request = buildSofaRequest(httpMetadata);

        return data -> {
            // Decode gRPC framed data
            byte[] decodedData = decodeGrpcFrame(data);

            // Process the request
            processRequest(request, decodedData);
        };
    }

    /**
     * Build SofaRequest from HTTP metadata.
     *
     * @param metadata HTTP metadata
     * @return SofaRequest
     */
    protected SofaRequest buildSofaRequest(HttpMetadata metadata) {
        SofaRequest request = new SofaRequest();
        String path = metadata.path();

        // Parse service and method from path: /service/method
        String serviceName = extractServiceName(path);
        String methodName = extractMethodName(path);

        request.setInterfaceName(serviceName);
        request.setMethodName(methodName);

        // Parse headers
        parseHeaders(request, metadata.headers());

        return request;
    }

    /**
     * Decode gRPC framed data.
     * gRPC frame format: 1 byte compression flag + 4 byte length + data
     *
     * @param data framed data
     * @return decoded data
     */
    protected byte[] decodeGrpcFrame(byte[] data) {
        if (data == null || data.length < 5) {
            return data;
        }

        // Check compression flag (first byte)
        // Currently only identity (no compression) is supported
        // boolean compressed = data[0] != 0;

        // Read length (4 bytes, big-endian)
        int length = ((data[1] & 0xFF) << 24) |
            ((data[2] & 0xFF) << 16) |
            ((data[3] & 0xFF) << 8) |
            (data[4] & 0xFF);

        // Extract payload
        if (data.length >= 5 + length) {
            byte[] payload = new byte[length];
            System.arraycopy(data, 5, payload, 0, length);
            return payload;
        }

        return data;
    }

    /**
     * Process the request.
     *
     * @param request SofaRequest
     * @param data request data
     */
    protected void processRequest(SofaRequest request, byte[] data) {
        Invoker targetInvoker = findInvoker(request);
        if (targetInvoker == null) {
            onError(new RuntimeException("Service not found: " + request.getInterfaceName()));
            return;
        }

        // TODO: Deserialize data and invoke
    }

    /**
     * Find invoker for the request.
     *
     * @param request SofaRequest
     * @return invoker
     */
    protected Invoker findInvoker(SofaRequest request) {
        if (invoker == null) {
            return null;
        }
        // Use the invoker directly - it handles routing internally
        return invoker;
    }

    /**
     * Extract service name from path.
     *
     * @param path request path
     * @return service name
     */
    protected String extractServiceName(String path) {
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
     * Extract method name from path.
     *
     * @param path request path
     * @return method name
     */
    protected String extractMethodName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0 && slashIndex < path.length() - 1) {
            return path.substring(slashIndex + 1);
        }
        return "";
    }

    /**
     * Parse headers into SofaRequest.
     *
     * @param request SofaRequest
     * @param headers HTTP headers
     */
    protected void parseHeaders(SofaRequest request, HttpHeaders headers) {
        // Parse gRPC metadata
        String timeoutStr = headers != null ? headers.get("grpc-timeout") : null;
        if (timeoutStr != null) {
            // Parse gRPC timeout format (e.g., "10S", "100m")
            request.setTimeout(parseGrpcTimeout(timeoutStr));
        }

        // Parse custom triple headers
        String serialization = headers != null ? headers.get("tri-serialize-type") : null;
        if (serialization != null) {
            try {
                request.setSerializeType(Byte.parseByte(serialization));
            } catch (NumberFormatException e) {
                // Ignore invalid serialization type
            }
        }
    }

    /**
     * Parse gRPC timeout string.
     *
     * @param timeoutStr timeout string (e.g., "10S", "100m")
     * @return timeout in milliseconds
     */
    protected int parseGrpcTimeout(String timeoutStr) {
        if (timeoutStr == null || timeoutStr.isEmpty()) {
            return 0;
        }

        try {
            // Extract unit and value
            char unit = timeoutStr.charAt(timeoutStr.length() - 1);
            long value = Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1));

            switch (unit) {
                case 'H':
                    return (int) (value * 60 * 60 * 1000);
                case 'M':
                    return (int) (value * 60 * 1000);
                case 'S':
                    return (int) (value * 1000);
                case 'm':
                    return (int) value;
                case 'u':
                    return (int) (value / 1000);
                case 'n':
                    return (int) (value / 1000000);
                default:
                    return (int) value;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Check if this listener supports the given content type.
     *
     * @param contentType content type
     * @return true if supported
     */
    public boolean supportsContentType(String contentType) {
        return contentType != null && contentType.startsWith(GRPC_CONTENT_TYPE);
    }

    /**
     * HTTP message listener interface.
     */
    @FunctionalInterface
    protected interface HttpMessageListener {
        void onMessage(byte[] data);
    }
}