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
package com.alipay.sofa.rpc.transport.triple.http3;

import com.alipay.sofa.rpc.transport.triple.http.DefaultHttpHeaders;
import com.alipay.sofa.rpc.transport.triple.http.HttpHeaders;
import com.alipay.sofa.rpc.transport.triple.http.HttpMetadata;
import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;

/**
 * HTTP/3 metadata implementation.
 * HTTP/3 uses QUIC transport and has similar semantics to HTTP/2.
 */
public class Http3Metadata implements HttpMetadata {

    private String      method;
    private String      path;
    private String      uri;
    private String      scheme;
    private HttpHeaders headers;
    private String      remoteAddress;
    private String      localAddress;

    public Http3Metadata() {
        this.headers = new DefaultHttpHeaders();
    }

    public Http3Metadata(String method, String path) {
        this.method = method;
        this.path = path;
        this.uri = path;
        this.headers = new DefaultHttpHeaders();
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String uri() {
        return uri != null ? uri : path;
    }

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_3;
    }

    @Override
    public String scheme() {
        return scheme != null ? scheme : "https"; // HTTP/3 typically uses HTTPS
    }

    @Override
    public String remoteAddress() {
        return remoteAddress;
    }

    @Override
    public String localAddress() {
        return localAddress;
    }

    // Setters

    public void setMethod(String method) {
        this.method = method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public String toString() {
        return "Http3Metadata{" +
            "method='" + method + '\'' +
            ", path='" + path + '\'' +
            ", httpVersion=" + httpVersion() +
            '}';
    }
}