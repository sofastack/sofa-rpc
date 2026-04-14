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
package com.alipay.sofa.rpc.transport.triple.stream;

import java.util.Map;

/**
 * Abstraction of a single bidirectional HTTP/2 stream.
 *
 * <p>This is the Transport layer in the three-tier separation architecture:
 * <pre>
 *   RpcCall  (invocation semantics)
 *      |
 *   RpcCodec (encoding/decoding)
 *      |
 *   RpcStream (transport) ← this interface
 * </pre>
 *
 * <p>An {@code RpcStream} represents one HTTP/2 stream and is responsible only for
 * sending and receiving raw bytes. It knows nothing about gRPC framing or RPC semantics.
 *
 * <p>Implementations must be thread-safe for write operations (they may be called from
 * multiple threads), but listener callbacks are always invoked on the Netty EventLoop thread.
 *
 * @see RpcStreamListener
 * @see Http2RpcStream
 */
public interface RpcStream {

    /**
     * Send a data frame on this stream.
     *
     * <p>The {@code data} array must already be encoded by {@code RpcCodec}.
     * The implementation may buffer frames internally and flush based on flow control.
     *
     * @param data       encoded payload bytes (must not be null)
     * @param compressed whether the payload was compressed
     */
    void writeMessage(byte[] data, boolean compressed);

    /**
     * Send HTTP/2 headers on this stream.
     *
     * <p>For requests, called before {@link #writeMessage}. For responses, called
     * to send status headers (e.g. ":status", "content-type", "grpc-status").
     *
     * @param headers   header key-value pairs (must not be null)
     * @param endStream if true, this is the terminal headers frame (trailers)
     */
    void writeHeaders(Map<String, String> headers, boolean endStream);

    /**
     * Half-close the stream: signals that the sender will not send any more data.
     *
     * <p>For client streams this means the request body is complete. The server may
     * still send responses after receiving a half-close.
     */
    void halfClose();

    /**
     * Abort the stream immediately with the given cause.
     *
     * @param cause the reason for cancellation (may be null for normal cancel)
     */
    void cancel(Throwable cause);

    /**
     * Register the event listener for this stream. Must be called before any read events arrive.
     *
     * @param listener the stream event listener (must not be null)
     */
    void setListener(RpcStreamListener listener);

    /**
     * Flow control: signal that the application is ready to receive {@code count} more messages.
     *
     * <p>This maps to HTTP/2 WINDOW_UPDATE semantics. The transport will deliver at most
     * {@code count} more {@link RpcStreamListener#onMessage} callbacks before pausing.
     *
     * @param count the number of messages the application can accept
     */
    void request(int count);

    /**
     * Returns {@code true} if the underlying channel/stream can accept writes without
     * buffering excessively (back-pressure indicator).
     *
     * @return true if the stream is writable
     */
    boolean isWritable();
}
