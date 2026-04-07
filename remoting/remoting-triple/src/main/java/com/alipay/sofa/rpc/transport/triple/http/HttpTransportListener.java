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
 * Listener interface for HTTP transport events.
 * This is the core interface for handling HTTP requests across different HTTP versions.
 *
 * @param <H> HTTP metadata type
 * @param <M> HTTP input message type
 */
public interface HttpTransportListener<H extends HttpMetadata, M extends HttpInputMessage> {

    /**
     * Called when HTTP headers are received.
     * This is the first callback for a new request.
     *
     * @param metadata HTTP headers/metadata
     */
    void onMetadata(H metadata);

    /**
     * Called when HTTP body data is received.
     * May be called multiple times for streaming requests.
     *
     * @param message HTTP input message
     */
    void onData(M message);

    /**
     * Called when the stream is complete.
     * No more data will be received after this callback.
     */
    void onComplete();

    /**
     * Called when an error occurs.
     *
     * @param throwable the error
     */
    void onError(Throwable throwable);

    /**
     * Called when the stream is cancelled by the client.
     *
     * @param errorCode error code (HTTP/2 error code or -1 for unknown)
     */
    default void onCancel(long errorCode) {
        onError(new RuntimeException("Stream cancelled with code: " + errorCode));
    }

    /**
     * Get the HTTP version this listener handles.
     *
     * @return HTTP version
     */
    HttpVersion httpVersion();
}