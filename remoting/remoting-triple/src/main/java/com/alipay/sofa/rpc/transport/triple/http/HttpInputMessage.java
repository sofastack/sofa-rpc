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

import java.io.Closeable;
import java.io.InputStream;

/**
 * HTTP input message interface for TripleX.
 * Represents the body of an HTTP request or response.
 */
public interface HttpInputMessage extends Closeable {

    /**
     * Get the message body as byte array.
     *
     * @return body bytes
     */
    byte[] getBody();

    /**
     * Get the message body as input stream.
     *
     * @return input stream
     */
    InputStream getBodyStream();

    /**
     * Check if the message has a body.
     *
     * @return true if has body
     */
    default boolean hasBody() {
        byte[] body = getBody();
        return body != null && body.length > 0;
    }

    /**
     * Get the content length of the body.
     *
     * @return content length, or -1 if unknown
     */
    default long contentLength() {
        byte[] body = getBody();
        return body != null ? body.length : -1;
    }

    /**
     * Check if the message is end of stream.
     *
     * @return true if end of stream
     */
    default boolean isEndOfStream() {
        return true;
    }

    /**
     * Close the message and release resources.
     */
    @Override
    default void close() {
        // Default: no-op
    }
}