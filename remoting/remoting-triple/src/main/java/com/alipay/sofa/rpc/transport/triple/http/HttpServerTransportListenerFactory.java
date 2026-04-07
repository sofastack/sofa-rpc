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

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.Extensible;

import java.util.concurrent.Executor;

/**
 * SPI factory for creating HTTP server transport listeners.
 * Each HTTP version (HTTP/1.1, HTTP/2, HTTP/3) should have its own factory implementation.
 */
@Extensible
public interface HttpServerTransportListenerFactory {

    /**
     * Create a new server transport listener.
     *
     * @param channel HTTP channel
     * @param serverConfig server configuration
     * @param bizExecutor business thread pool executor
     * @return transport listener instance
     */
    HttpTransportListener<?, ?> newInstance(HttpChannel channel, ServerConfig serverConfig, Executor bizExecutor);

    /**
     * Check if this factory supports the given HTTP version.
     *
     * @param version HTTP version
     * @return true if supported
     */
    boolean supports(HttpVersion version);

    /**
     * Check if this factory supports the given content type.
     * This is used for content-type based routing (e.g., gRPC vs REST).
     *
     * @param contentType content type header value
     * @return true if supported
     */
    boolean supportsContentType(String contentType);

    /**
     * Get priority for factory selection (higher = preferred).
     * This is used when multiple factories support the same HTTP version.
     *
     * @return priority value
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Get the HTTP version this factory handles.
     *
     * @return HTTP version
     */
    HttpVersion httpVersion();
}