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

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Server HTTP channel observer for writing responses.
 * This interface provides a unified way to write responses across different HTTP versions.
 *
 * @param <C> HTTP channel type
 */
public interface ServerHttpChannelObserver<C extends HttpChannel> {

    /**
     * Called when a response message is ready to be sent.
     *
     * @param value the response value
     */
    void onNext(Object value);

    /**
     * Called when an error occurs.
     *
     * @param throwable the error
     */
    void onError(Throwable throwable);

    /**
     * Called when the response is complete.
     */
    void onCompleted();

    /**
     * Get the underlying HTTP channel.
     *
     * @return HTTP channel
     */
    C getHttpChannel();

    /**
     * Add a headers customizer for modifying response headers.
     *
     * @param customizer headers customizer function
     */
    void addHeadersCustomizer(BiConsumer<HttpHeaders, Throwable> customizer);

    /**
     * Set an exception customizer for converting exceptions to response objects.
     *
     * @param customizer exception customizer function
     */
    void setExceptionCustomizer(Function<Throwable, ?> customizer);

    /**
     * Close the observer and release resources.
     */
    void close();

    /**
     * Check if the observer is cancelled.
     *
     * @return true if cancelled
     */
    default boolean isCancelled() {
        return false;
    }

    /**
     * Set the compression encoder to use.
     *
     * @param encoding compression encoding (e.g., "gzip", "identity")
     */
    default void setCompression(String encoding) {
        // Default: no-op
    }
}