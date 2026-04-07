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
package com.alipay.sofa.rpc.transport.triple.http1;

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.triple.http.*;
import com.alipay.sofa.rpc.transport.triple.server.AbstractServerCallListener;
import com.alipay.sofa.rpc.transport.triple.server.AbstractServerTransportListener;
import com.alipay.sofa.rpc.transport.triple.server.UnaryServerCallListener;
import com.alipay.sofa.rpc.transport.triple.server.ServerStreamServerCallListener;

import java.util.concurrent.Executor;

/**
 * HTTP/1.1 server transport listener implementation.
 * Supports unary and server-streaming calls.
 */
public class Http1ServerTransportListener
                                         extends AbstractServerTransportListener<Http1Metadata, Http1InputMessage> {

    /** Default request timeout in milliseconds */
    private static final int          DEFAULT_TIMEOUT_MS = 3000;

    private final Executor             bizExecutor;
    private HttpMessageListener        messageListener;
    private Http1ServerChannelObserver responseObserver;

    public Http1ServerTransportListener(HttpChannel channel, ServerConfig serverConfig,
                                        UniqueIdInvoker invoker, Executor bizExecutor) {
        super(channel, serverConfig, invoker);
        this.bizExecutor = bizExecutor;
        this.responseObserver = new Http1ServerChannelObserver(channel);
    }

    @Override
    protected Executor initializeExecutor(Http1Metadata metadata) {
        return bizExecutor;
    }

    @Override
    protected HttpMessageListener buildHttpMessageListener() {
        // Build request and start invocation
        SofaRequest request = buildSofaRequest(getHttpMetadata());
        AbstractServerCallListener callListener = startCall(request);

        return callListener::onMessage;
    }

    @Override
    protected void setHttpMessageListener(HttpMessageListener listener) {
        this.messageListener = listener;
    }

    @Override
    protected void onMessage(byte[] data) {
        if (messageListener != null) {
            messageListener.onMessage(data);
        }
    }

    @Override
    public void onComplete() {
        // Invoke the service - message processing is complete
        // The actual invocation happens in onMessage via messageListener
    }

    /**
     * Start a call for the given request.
     *
     * @param request SofaRequest
     * @return server call listener
     */
    protected AbstractServerCallListener startCall(SofaRequest request) {
        Invoker targetInvoker = findInvoker(request);
        if (targetInvoker == null) {
            responseObserver.onError(new RuntimeException("Service not found: " + request.getInterfaceName()));
            return null;
        }

        // Determine call type based on method signature
        boolean isServerStream = isServerStreamingCall(request);

        if (isServerStream) {
            return new ServerStreamServerCallListener(request, targetInvoker, responseObserver);
        } else {
            return new UnaryServerCallListener(request, targetInvoker, responseObserver);
        }
    }

    /**
     * Check if the request is a server streaming call.
     *
     * @param request SofaRequest
     * @return true if server streaming
     */
    protected boolean isServerStreamingCall(SofaRequest request) {
        // Check method signature or header
        String streamHeader = getHttpMetadata().header("tri-stream-type");
        return "server".equals(streamHeader);
    }

    @Override
    protected SofaRequest buildSofaRequest(Http1Metadata metadata) {
        SofaRequest request = super.buildSofaRequest(metadata);

        // Parse query parameters for additional info
        String uri = metadata.uri();
        if (uri != null && uri.contains("?")) {
            String query = uri.substring(uri.indexOf("?") + 1);
            parseQueryParameters(request, query);
        }

        // Set default timeout
        if (request.getTimeout() <= 0) {
            request.setTimeout(DEFAULT_TIMEOUT_MS);
        }

        return request;
    }

    /**
     * Parse query parameters into request.
     *
     * @param request SofaRequest
     * @param query query string
     */
    protected void parseQueryParameters(SofaRequest request, String query) {
        if (StringUtils.isBlank(query)) {
            return;
        }

        String[] params = query.split("&");
        for (String param : params) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0];
                String value = kv[1];

                switch (key) {
                    case "timeout":
                        try {
                            request.setTimeout(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                        break;
                    case "serialize":
                        try {
                            request.setSerializeType(Byte.parseByte(value));
                        } catch (NumberFormatException e) {
                            // Ignore invalid serialization type
                        }
                        break;
                    case "version":
                        request.setTargetServiceUniqueName(request.getInterfaceName() + ":" + value);
                        break;
                    default:
                        // Ignore unknown parameters
                }
            }
        }
    }

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_1;
    }
}