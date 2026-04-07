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
package com.alipay.sofa.rpc.transport.triple.server;

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.triple.http.*;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

/**
 * Abstract base class for server-side HTTP transport listeners.
 * Provides common request routing, serialization, and invocation logic.
 *
 * @param <H> HTTP metadata type
 * @param <M> HTTP input message type
 */
public abstract class AbstractServerTransportListener<H extends HttpMetadata, M extends HttpInputMessage>
                                                                                                          implements
                                                                                                          HttpTransportListener<H, M> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServerTransportListener.class);

    protected final ServerConfig    serverConfig;
    protected final UniqueIdInvoker invoker;
    protected final HttpChannel     httpChannel;
    protected Executor              executor;
    protected H                     httpMetadata;

    protected AbstractServerTransportListener(HttpChannel httpChannel, ServerConfig serverConfig,
                                              UniqueIdInvoker invoker) {
        this.httpChannel = httpChannel;
        this.serverConfig = serverConfig;
        this.invoker = invoker;
    }

    @Override
    public void onMetadata(H metadata) {
        this.httpMetadata = metadata;
        try {
            executor = initializeExecutor(metadata);
            if (executor == null) {
                throw new IllegalStateException("Executor must not be null");
            }
            executor.execute(() -> {
                try {
                    onPrepareMetadata(metadata);
                    HttpMessageListener messageListener = buildHttpMessageListener();
                    setHttpMessageListener(messageListener);
                    onMetadataCompletion(metadata);
                } catch (Throwable t) {
                    onMetadataError(metadata, t);
                }
            });
        } catch (Throwable t) {
            onMetadataError(metadata, t);
        }
    }

    @Override
    public void onData(M message) {
        if (executor == null) {
            return;
        }
        executor.execute(() -> {
            try {
                doOnData(message);
            } catch (Throwable t) {
                onError(message, t);
            } finally {
                try {
                    message.close();
                } catch (Exception e) {
                    onError(e);
                }
            }
        });
    }

    /**
     * Process received data.
     *
     * @param message HTTP input message
     */
    protected void doOnData(M message) {
        onPrepareData(message);
        onMessage(message.getBody());
        onDataCompletion(message);
    }

    /**
     * Build HTTP message listener after metadata is prepared.
     *
     * @return message listener
     */
    protected abstract HttpMessageListener buildHttpMessageListener();

    /**
     * Set the HTTP message listener.
     *
     * @param listener message listener
     */
    protected abstract void setHttpMessageListener(HttpMessageListener listener);

    /**
     * Initialize executor for request processing.
     *
     * @param metadata HTTP metadata
     * @return executor
     */
    protected abstract Executor initializeExecutor(H metadata);

    /**
     * Called when metadata preparation is complete.
     *
     * @param metadata HTTP metadata
     */
    protected void onPrepareMetadata(H metadata) {
        // Default: no-op
    }

    /**
     * Called when metadata processing is complete.
     *
     * @param metadata HTTP metadata
     */
    protected void onMetadataCompletion(H metadata) {
        // Default: no-op
    }

    /**
     * Called when metadata processing fails.
     *
     * @param metadata HTTP metadata
     * @param throwable error
     */
    protected void onMetadataError(H metadata, Throwable throwable) {
        onError(throwable);
    }

    /**
     * Called before processing data.
     *
     * @param message HTTP input message
     */
    protected void onPrepareData(M message) {
        // Default: no-op
    }

    /**
     * Called when data processing is complete.
     *
     * @param message HTTP input message
     */
    protected void onDataCompletion(M message) {
        // Default: no-op
    }

    /**
     * Called when a complete message is received.
     *
     * @param data message bytes
     */
    protected abstract void onMessage(byte[] data);

    /**
     * Called when an error occurs during data processing.
     *
     * @param message HTTP input message
     * @param throwable error
     */
    protected void onError(M message, Throwable throwable) {
        onError(throwable);
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.error("Transport error, closing channel", throwable);
        if (httpChannel != null) {
            httpChannel.close();
        }
    }

    /**
     * Get the HTTP metadata.
     *
     * @return HTTP metadata
     */
    protected H getHttpMetadata() {
        return httpMetadata;
    }

    /**
     * Get the remote address.
     *
     * @return remote address
     */
    protected SocketAddress getRemoteAddress() {
        return httpChannel.remoteAddress();
    }

    /**
     * Get the local address.
     *
     * @return local address
     */
    protected SocketAddress getLocalAddress() {
        return httpChannel.localAddress();
    }

    /**
     * Build SofaRequest from HTTP metadata.
     *
     * @param metadata HTTP metadata
     * @return SofaRequest
     */
    protected SofaRequest buildSofaRequest(H metadata) {
        SofaRequest request = new SofaRequest();
        String path = metadata.path();

        // Parse service and method from path: /service/method
        String serviceName = extractServiceName(path);
        String methodName = extractMethodName(path);

        request.setInterfaceName(serviceName);
        request.setMethodName(methodName);

        // Parse headers for timeout, serialization, etc.
        parseHeaders(request, metadata.headers());

        return request;
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
        // Remove leading slash
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
        // Remove leading slash
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
        // Parse timeout
        String timeoutStr = headers != null ? headers.get("tri-timeout") : null;
        if (timeoutStr != null) {
            try {
                request.setTimeout(Integer.parseInt(timeoutStr));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Parse serialization type
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
     * HTTP message listener interface.
     */
    @FunctionalInterface
    protected interface HttpMessageListener {
        /**
         * Called when a message is received.
         *
         * @param data message bytes
         */
        void onMessage(byte[] data);
    }
}