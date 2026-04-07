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
 * HTTP protocol version enumeration for TripleX.
 * Supports HTTP/1.1, HTTP/2, and HTTP/3.
 */
public enum HttpVersion {

    /**
     * HTTP/1.1 version
     */
    HTTP_1("http1", "HTTP/1.1"),

    /**
     * HTTP/2 version
     */
    HTTP_2("http2", "HTTP/2.0"),

    /**
     * HTTP/3 version (QUIC-based)
     */
    HTTP_3("http3", "HTTP/3.0");

    private final String version;
    private final String protocol;

    HttpVersion(String version, String protocol) {
        this.version = version;
        this.protocol = protocol;
    }

    /**
     * Get the short version string (e.g., "http1", "http2", "http3")
     *
     * @return version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the protocol string (e.g., "HTTP/1.1", "HTTP/2.0", "HTTP/3.0")
     *
     * @return protocol string
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Parse HttpVersion from version string.
     *
     * @param version version string (e.g., "http1", "http2", "http3")
     * @return HttpVersion enum value, defaults to HTTP_2 if not found
     */
    public static HttpVersion fromVersion(String version) {
        if (version == null) {
            return HTTP_2;
        }
        switch (version.toLowerCase()) {
            case "http1":
            case "http/1.1":
            case "1.1":
            case "1":
                return HTTP_1;
            case "http3":
            case "http/3.0":
            case "http/3":
            case "3.0":
            case "3":
                return HTTP_3;
            case "http2":
            case "http/2.0":
            case "http/2":
            case "2.0":
            case "2":
            default:
                return HTTP_2;
        }
    }
}