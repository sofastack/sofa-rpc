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

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract HTTP channel interface for unified HTTP/1.1, HTTP/2, HTTP/3 communication.
 * This interface provides a common abstraction for writing HTTP messages across
 * different HTTP versions.
 */
public interface HttpChannel {

    /**
     * Get the HTTP version of this channel.
     *
     * @return HTTP version
     */
    HttpVersion httpVersion();

    /**
     * Write HTTP headers/metadata to the channel.
     *
     * @param metadata HTTP metadata (headers)
     * @return CompletableFuture that completes when headers are written
     */
    CompletableFuture<Void> writeHeader(HttpMetadata metadata);

    /**
     * Write HTTP message body to the channel.
     *
     * @param message HTTP output message
     * @return CompletableFuture that completes when message is written
     */
    CompletableFuture<Void> writeMessage(HttpOutputMessage message);

    /**
     * Write HTTP message body with end-of-stream flag.
     *
     * @param message HTTP output message
     * @param endOfStream true if this is the last message
     * @return CompletableFuture that completes when message is written
     */
    default CompletableFuture<Void> writeMessage(HttpOutputMessage message, boolean endOfStream) {
        return writeMessage(message);
    }

    /**
     * Create a new output message buffer.
     *
     * @return new HttpOutputMessage instance
     */
    HttpOutputMessage newOutputMessage();

    /**
     * Get remote address.
     *
     * @return remote socket address
     */
    SocketAddress remoteAddress();

    /**
     * Get local address.
     *
     * @return local socket address
     */
    SocketAddress localAddress();

    /**
     * Flush buffered data to the network.
     */
    void flush();

    /**
     * Check if the channel is active.
     *
     * @return true if channel is active
     */
    boolean isActive();

    /**
     * Check if the channel is writable.
     *
     * @return true if channel is writable
     */
    boolean isWritable();

    /**
     * Close the channel.
     */
    void close();

    /**
     * Get an attribute from the channel.
     *
     * @param key attribute key
     * @param <T> attribute type
     * @return attribute value, or null if not present
     */
    default <T> T getAttribute(String key) {
        return null;
    }

    /**
     * Set an attribute on the channel.
     *
     * @param key attribute key
     * @param value attribute value
     * @param <T> attribute type
     */
    default <T> void setAttribute(String key, T value) {
        // Default: no-op
    }
}