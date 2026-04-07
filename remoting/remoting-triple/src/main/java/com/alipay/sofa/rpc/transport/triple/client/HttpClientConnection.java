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
package com.alipay.sofa.rpc.transport.triple.client;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;

/**
 * HTTP client connection interface for different HTTP versions.
 * Each HTTP version (HTTP/1.1, HTTP/2, HTTP/3) should have its own implementation.
 */
public interface HttpClientConnection {

    /**
     * Send synchronous request.
     *
     * @param request SofaRequest
     * @param timeout timeout in milliseconds
     * @return SofaResponse
     * @throws Exception if error occurs
     */
    SofaResponse syncSend(SofaRequest request, int timeout) throws Exception;

    /**
     * Send asynchronous request.
     *
     * @param request SofaRequest
     * @param timeout timeout in milliseconds
     * @return ResponseFuture
     * @throws Exception if error occurs
     */
    ResponseFuture asyncSend(SofaRequest request, int timeout) throws Exception;

    /**
     * Check if connection is available.
     *
     * @return true if available
     */
    boolean isAvailable();

    /**
     * Close the connection.
     */
    void close();

    /**
     * Get the HTTP version of this connection.
     *
     * @return HTTP version
     */
    HttpVersion httpVersion();

    /**
     * Get the provider info.
     *
     * @return provider info
     */
    ProviderInfo getProviderInfo();

    /**
     * Get the number of pending requests.
     *
     * @return pending request count
     */
    default int pendingRequests() {
        return 0;
    }
}