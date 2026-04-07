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
package com.alipay.sofa.rpc.transport.triple.grpc;

import com.alipay.sofa.rpc.transport.triple.http.*;

import java.util.function.BiConsumer;
import java.util.function.Function;

import io.grpc.stub.StreamObserver;

/**
 * gRPC server channel observer for writing responses.
 * Wraps gRPC StreamObserver to provide compatibility with the HTTP transport abstraction.
 */
public class GrpcServerChannelObserver implements ServerHttpChannelObserver<HttpChannel> {

    private final StreamObserver<?>            grpcStreamObserver;
    private final HttpChannel                  httpChannel;
    private Function<Throwable, ?>             exceptionCustomizer;
    private BiConsumer<HttpHeaders, Throwable> headersCustomizer;
    private boolean                            completed = false;

    @SuppressWarnings("unchecked")
    public GrpcServerChannelObserver(StreamObserver<?> grpcStreamObserver, HttpChannel httpChannel) {
        this.grpcStreamObserver = grpcStreamObserver;
        this.httpChannel = httpChannel;
    }

    @Override
    public void onNext(Object value) {
        if (completed) {
            return;
        }

        @SuppressWarnings("unchecked")
        StreamObserver<Object> observer = (StreamObserver<Object>) grpcStreamObserver;
        observer.onNext(value);
    }

    @Override
    public void onError(Throwable throwable) {
        if (completed) {
            return;
        }
        completed = true;

        // gRPC StreamObserver doesn't have onError for responses
        // We need to handle this differently
        if (httpChannel != null && httpChannel.isActive()) {
            // Write error response through HTTP channel
            // For gRPC, errors are typically sent as trailers
            if (exceptionCustomizer != null) {
                exceptionCustomizer.apply(throwable);
            }
            httpChannel.close();
        }
    }

    @Override
    public void onCompleted() {
        if (completed) {
            return;
        }
        completed = true;
        grpcStreamObserver.onCompleted();
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
        if (httpChannel != null) {
            httpChannel.close();
        }
    }

    @Override
    public boolean isCancelled() {
        return completed || (httpChannel != null && !httpChannel.isActive());
    }

    /**
     * Get the underlying gRPC StreamObserver.
     *
     * @return StreamObserver
     */
    public StreamObserver<?> getGrpcStreamObserver() {
        return grpcStreamObserver;
    }
}