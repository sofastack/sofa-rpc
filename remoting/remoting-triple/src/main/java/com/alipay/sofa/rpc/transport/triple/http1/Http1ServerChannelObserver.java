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

import com.alipay.sofa.rpc.transport.triple.http.*;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * HTTP/1.1 server channel observer for writing responses.
 */
public class Http1ServerChannelObserver implements ServerHttpChannelObserver<HttpChannel> {

    protected final HttpChannel                  httpChannel;
    protected Function<Throwable, ?>             exceptionCustomizer;
    protected BiConsumer<HttpHeaders, Throwable> headersCustomizer;
    protected boolean                            completed = false;
    protected String                             compression;

    public Http1ServerChannelObserver(HttpChannel httpChannel) {
        this.httpChannel = httpChannel;
    }

    @Override
    public void onNext(Object value) {
        if (completed) {
            return;
        }

        // Encode and write response body
        HttpOutputMessage message = encodeResponse(value);
        httpChannel.writeMessage(message);
    }

    @Override
    public void onError(Throwable throwable) {
        if (completed) {
            return;
        }
        completed = true;

        Object errorResponse = exceptionCustomizer != null
            ? exceptionCustomizer.apply(throwable)
            : buildErrorResponse(throwable);

        // Write error response
        HttpMetadata headers = buildErrorHeaders(throwable);
        httpChannel.writeHeader(headers);
        httpChannel.writeMessage(encodeResponse(errorResponse));
        httpChannel.flush();
        httpChannel.close();
    }

    @Override
    public void onCompleted() {
        if (completed) {
            return;
        }
        completed = true;
        httpChannel.flush();
    }

    /**
     * Encode response value to HTTP output message.
     *
     * @param value response value
     * @return HTTP output message
     */
    protected HttpOutputMessage encodeResponse(Object value) {
        Http1OutputMessage message = new Http1OutputMessage();
        if (value instanceof byte[]) {
            message.setBody((byte[]) value);
        } else if (value != null) {
            // TODO: Use serializer to encode
            message.setBody(value.toString().getBytes());
        }
        return message;
    }

    /**
     * Build error headers.
     *
     * @param throwable error
     * @return HTTP metadata with error headers
     */
    protected HttpMetadata buildErrorHeaders(Throwable throwable) {
        Http1Metadata metadata = new Http1Metadata();
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.set("content-type", "application/json");
        headers.set("tri-error", "true");
        headers.set("tri-error-message", throwable.getMessage());
        metadata.setHeaders(headers);
        return metadata;
    }

    /**
     * Build error response object.
     *
     * @param throwable error
     * @return error response
     */
    protected Object buildErrorResponse(Throwable throwable) {
        return "{\"error\":\"" + throwable.getMessage() + "\"}";
    }

    @Override
    public HttpChannel getHttpChannel() {
        return httpChannel;
    }

    @Override
    public void addHeadersCustomizer(BiConsumer<HttpHeaders, Throwable> customizer) {
        this.headersCustomizer = customizer;
    }

    @Override
    public void setExceptionCustomizer(Function<Throwable, ?> customizer) {
        this.exceptionCustomizer = customizer;
    }

    @Override
    public void close() {
        completed = true;
        httpChannel.close();
    }

    @Override
    public boolean isCancelled() {
        return !httpChannel.isActive();
    }

    @Override
    public void setCompression(String encoding) {
        this.compression = encoding;
    }
}