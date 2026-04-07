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

/**
 * HTTP metadata interface representing headers and request metadata.
 * This is the base interface for HTTP/1.1, HTTP/2, and HTTP/3 metadata.
 */
public interface HttpMetadata {

    /**
     * Get HTTP headers.
     *
     * @return HTTP headers
     */
    HttpHeaders headers();

    /**
     * Get header value by name.
     *
     * @param name header name
     * @return header value, or null if not present
     */
    default String header(String name) {
        HttpHeaders headers = headers();
        return headers != null ? headers.get(name) : null;
    }

    /**
     * Get HTTP method (GET, POST, PUT, DELETE, etc.)
     *
     * @return HTTP method
     */
    String method();

    /**
     * Get request path (URI path without query string).
     *
     * @return request path
     */
    String path();

    /**
     * Get full URI including query string.
     *
     * @return full URI
     */
    default String uri() {
        return path();
    }

    /**
     * Get HTTP version.
     *
     * @return HTTP version
     */
    HttpVersion httpVersion();

    /**
     * Get content type header.
     *
     * @return content type, or null if not present
     */
    default String contentType() {
        return header("content-type");
    }

    /**
     * Check if this is a gRPC request.
     *
     * @return true if gRPC request
     */
    default boolean isGrpcRequest() {
        String ct = contentType();
        return ct != null && ct.startsWith("application/grpc");
    }

    /**
     * Check if this is a REST/JSON request.
     *
     * @return true if REST/JSON request
     */
    default boolean isJsonRequest() {
        String ct = contentType();
        return ct != null && (ct.contains("application/json") || ct.contains("text/json"));
    }

    /**
     * Get the HTTP scheme (http or https).
     *
     * @return scheme
     */
    default String scheme() {
        return "http";
    }

    /**
     * Get the remote address (client address).
     *
     * @return remote address string
     */
    default String remoteAddress() {
        return null;
    }

    /**
     * Get the local address (server address).
     *
     * @return local address string
     */
    default String localAddress() {
        return null;
    }
}