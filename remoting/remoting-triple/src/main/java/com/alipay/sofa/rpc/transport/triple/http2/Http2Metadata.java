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

import com.alipay.sofa.rpc.transport.triple.http.HttpHeaders;
import com.alipay.sofa.rpc.transport.triple.http.HttpMetadata;
import com.alipay.sofa.rpc.transport.triple.http.HttpVersion;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Headers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * HTTP/2 implementation of HttpMetadata.
 * Wraps Netty's Http2Headers to provide a uniform interface.
 */
public class Http2Metadata implements HttpMetadata {

    private final Http2Headers headers;
    private final int          streamId;
    private final HttpHeaders  wrappedHeaders;

    /**
     * Create a new Http2Metadata.
     *
     * @param headers HTTP/2 headers
     * @param streamId HTTP/2 stream ID
     */
    public Http2Metadata(Http2Headers headers, int streamId) {
        this.headers = headers;
        this.streamId = streamId;
        this.wrappedHeaders = new Http2HeadersAdapter(headers);
    }

    @Override
    public HttpHeaders headers() {
        return wrappedHeaders;
    }

    @Override
    public String method() {
        CharSequence method = headers.method();
        return method != null ? method.toString() : null;
    }

    @Override
    public String path() {
        CharSequence path = headers.path();
        return path != null ? path.toString() : null;
    }

    @Override
    public String uri() {
        // For HTTP/2, path and uri are typically the same
        return path();
    }

    @Override
    public HttpVersion httpVersion() {
        return HttpVersion.HTTP_2;
    }

    @Override
    public String contentType() {
        CharSequence contentType = headers.get("content-type");
        return contentType != null ? contentType.toString() : null;
    }

    @Override
    public boolean isGrpcRequest() {
        String contentType = contentType();
        return contentType != null && contentType.startsWith("application/grpc");
    }

    @Override
    public boolean isJsonRequest() {
        String contentType = contentType();
        return contentType != null && contentType.startsWith("application/json");
    }

    /**
     * Get the HTTP/2 stream ID.
     *
     * @return stream ID
     */
    public int getStreamId() {
        return streamId;
    }

    /**
     * Get the underlying HTTP/2 headers.
     *
     * @return HTTP/2 headers
     */
    public Http2Headers getHeaders() {
        return headers;
    }

    /**
     * Adapter to convert Netty Http2Headers to HttpHeaders interface.
     */
    private static class Http2HeadersAdapter implements HttpHeaders {

        private final Http2Headers http2Headers;

        public Http2HeadersAdapter(Http2Headers http2Headers) {
            this.http2Headers = http2Headers;
        }

        @Override
        public String get(String name) {
            CharSequence value = http2Headers.get(name);
            return value != null ? value.toString() : null;
        }

        public String get(CharSequence name) {
            CharSequence value = http2Headers.get(name);
            return value != null ? value.toString() : null;
        }

        @Override
        public HttpHeaders set(String name, String value) {
            http2Headers.set(name, value);
            return this;
        }

        public HttpHeaders set(CharSequence name, CharSequence value) {
            http2Headers.set(name, value);
            return this;
        }

        @Override
        public HttpHeaders add(String name, String value) {
            http2Headers.add(name, value);
            return this;
        }

        public HttpHeaders add(CharSequence name, CharSequence value) {
            http2Headers.add(name, value);
            return this;
        }

        @Override
        public HttpHeaders remove(String name) {
            http2Headers.remove(name);
            return this;
        }

        public HttpHeaders remove(CharSequence name) {
            http2Headers.remove(name);
            return this;
        }

        @Override
        public boolean contains(String name) {
            return http2Headers.contains(name);
        }

        public boolean contains(CharSequence name) {
            return http2Headers.contains(name);
        }

        @Override
        public Set<String> names() {
            Set<String> nameSet = new HashSet<>();
            for (CharSequence name : http2Headers.names()) {
                nameSet.add(name.toString());
            }
            return nameSet;
        }

        @Override
        public int size() {
            return http2Headers.size();
        }

        @Override
        public boolean isEmpty() {
            return http2Headers.isEmpty();
        }

        @Override
        public Iterable<String> getAll(String name) {
            return http2Headers.getAll(name).stream()
                .map(CharSequence::toString)
                ::iterator;
        }

        @Override
        public void clear() {
            // Not supported for HTTP/2 headers
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            Set<Map.Entry<String, String>> entries = new java.util.HashSet<>();
            for (Map.Entry<CharSequence, CharSequence> entry : http2Headers) {
                entries.add(new Map.Entry<String, String>() {
                    @Override
                    public String getKey() {
                        return entry.getKey().toString();
                    }

                    @Override
                    public String getValue() {
                        return entry.getValue().toString();
                    }

                    @Override
                    public String setValue(String value) {
                        String oldValue = getValue();
                        http2Headers.set(entry.getKey(), value);
                        return oldValue;
                    }
                });
            }
            return entries.iterator();
        }
    }
}