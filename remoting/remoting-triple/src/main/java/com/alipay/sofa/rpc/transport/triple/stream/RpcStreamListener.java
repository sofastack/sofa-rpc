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
 * Listener for events on a {@link RpcStream}.
 *
 * <p>All callbacks are invoked on the Netty EventLoop thread. Implementations
 * must not block or perform slow operations inside callbacks.
 *
 * <p>The expected event sequence for a normal RPC:
 * <pre>
 *   onHeaders(requestHeaders, false)
 *   onMessage(data)  [may repeat for streaming calls]
 *   onComplete()
 * </pre>
 *
 * On error:
 * <pre>
 *   onHeaders(requestHeaders, false)  [optional]
 *   onError(cause)
 * </pre>
 */
public interface RpcStreamListener {

    /**
     * Called when HTTP/2 headers are received on this stream.
     *
     * <p>For server-side: called once with the request headers.
     * For client-side: called once with the response headers (status 200), and
     * potentially again with trailers ({@code endStream=true}).
     *
     * @param headers   the received header key-value pairs
     * @param endStream true if this headers frame also terminates the stream
     */
    void onHeaders(Map<String, String> headers, boolean endStream);

    /**
     * Called when a data frame is received on this stream.
     *
     * <p>The {@code data} bytes are raw transport bytes (including any gRPC frame prefix).
     * The {@link com.alipay.sofa.rpc.transport.triple.codec.RpcCodec RpcCodec} layer is
     * responsible for decoding them.
     *
     * @param data the received payload bytes (not null, not empty)
     */
    void onMessage(byte[] data);

    /**
     * Called when the remote peer has half-closed the stream (sent END_STREAM).
     *
     * <p>After this callback, no more {@link #onMessage} or {@link #onHeaders} calls
     * will arrive. The local side may still send responses.
     */
    void onComplete();

    /**
     * Called when the stream is terminated abnormally.
     *
     * <p>After this callback, no further events will be delivered on this stream.
     *
     * @param cause the exception that caused the stream to fail
     */
    void onError(Throwable cause);

    /**
     * Called when the writability state of the underlying channel changes.
     *
     * <p>This maps to Netty's {@code channelWritabilityChanged} event. Listeners can use
     * {@link RpcStream#isWritable()} to check the current state and pause/resume writing.
     */
    void onWritabilityChanged();
}
