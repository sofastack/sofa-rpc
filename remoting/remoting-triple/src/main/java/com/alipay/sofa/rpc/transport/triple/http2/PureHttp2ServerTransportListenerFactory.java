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

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.server.triple.UniqueIdInvoker;
import com.alipay.sofa.rpc.transport.triple.http.HttpChannel;
import com.alipay.sofa.rpc.transport.triple.http.HttpServerTransportListenerFactory;
import com.alipay.sofa.rpc.transport.triple.http.HttpTransportListener;
import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;

/**
 * Factory for creating pure HTTP/2 server transport listeners.
 * This implementation uses native Netty HTTP/2 (not gRPC) while maintaining
 * gRPC protocol compatibility.
 *
 * <p>Key features:
 * <ul>
 *   <li>Compatible with gRPC clients (application/grpc* content types)</li>
 *   <li>Supports gRPC frame format (compression flag + length + data)</li>
 *   <li>Handles gRPC-specific headers (grpc-timeout, grpc-encoding, etc.)</li>
 *   <li>Uses native Netty HTTP/2 instead of gRPC Netty</li>
 * </ul>
 */
@Extension("http2-pure")
public class PureHttp2ServerTransportListenerFactory implements HttpServerTransportListenerFactory {

    private UniqueIdInvoker invoker;

    @Override
    public HttpTransportListener<?, ?> newInstance(HttpChannel channel, ServerConfig serverConfig) {
        return new PureHttp2ServerTransportListener(channel, serverConfig, invoker);
    }

    @Override
    public boolean supports(HttpVersion version) {
        return version == HttpVersion.HTTP_2;
    }

    @Override
    public boolean supportsContentType(String contentType) {
        // Support both gRPC and JSON content types
        if (contentType == null) {
            return true;
        }
        // Support gRPC content types
        if (contentType.startsWith("application/grpc")) {
            return true;
        }
        // Also support JSON for REST-style calls
        return contentType.startsWith("application/json");
    }

    @Override
    public int getPriority() {
        // Lower priority than gRPC implementation (100), but higher than HTTP/1.1 (-100)
        return 50;
    }

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_2;
    }

    /**
     * Set the invoker.
     *
     * @param invoker UniqueIdInvoker
     */
    public void setInvoker(UniqueIdInvoker invoker) {
        this.invoker = invoker;
    }
}