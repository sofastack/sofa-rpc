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
package com.alipay.sofa.rpc.transport.triple.call;

import com.alipay.sofa.rpc.transport.triple.codec.RpcCodec;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStream;

import java.util.Map;

/**
 * Abstraction of the Invocation layer in the three-tier separation architecture.
 *
 * <p>The Call layer sits at the top of the three-tier hierarchy:
 * <pre>
 *   RpcCall  (invocation semantics) ← this interface
 *      |
 *   RpcCodec (encoding/decoding)
 *      |
 *   RpcStream (transport)
 * </pre>
 *
 * <p>An {@code RpcCall} is responsible for managing the lifecycle of a single RPC call,
 * including sending request headers and data, handling responses, and converting between
 * the wire format (raw bytes from RpcCodec) and the application-level objects.
 *
 * <p>Different subclasses handle different call types:
 * <ul>
 *   <li>{@link UnaryRpcCall} — single request, single response (Unary)</li>
 *   <li>ServerStreamRpcCall — single request, streaming responses (Sprint 5)</li>
 *   <li>BidiStreamRpcCall — streaming requests and responses (Sprint 5)</li>
 * </ul>
 *
 * @see UnaryRpcCall
 * @see RpcStream
 * @see RpcCodec
 */
public interface RpcCall {

    /**
     * Starts the RPC call by sending the initial headers.
     *
     * <p>For server-side calls, this sends the HTTP/2 response headers.
     * For client-side calls, this sends the HTTP/2 request headers.
     *
     * @param headers  the initial headers to send (e.g., ":status", "content-type")
     * @param listener the response listener for receiving callbacks
     */
    void start(Map<String, String> headers, RpcCallListener listener);

    /**
     * Sends a serialized request message on this call.
     *
     * <p>The message bytes are already encoded by the invocation layer (e.g., hessian2).
     * The codec layer will add any necessary framing before writing to the transport.
     *
     * @param message the serialized message bytes (must not be null)
     */
    void sendMessage(byte[] message);

    /**
     * Half-closes the sending side of this call, signaling that no more messages will be sent.
     *
     * <p>For unary calls, this is called automatically after {@link #sendMessage}.
     * For streaming calls, this must be called explicitly.
     */
    void halfClose();

    /**
     * Cancels the call with an optional cause.
     *
     * @param cause the reason for cancellation (may be null)
     */
    void cancel(Throwable cause);

    /**
     * Returns whether this call is ready to accept message writes without excessive buffering.
     *
     * @return true if the call is ready to write
     */
    boolean isReady();

    /**
     * Listener interface for receiving call lifecycle events.
     */
    interface RpcCallListener {

        /**
         * Called when response headers are received.
         *
         * @param headers   the response headers
         * @param endStream true if no data will follow
         */
        void onHeaders(Map<String, String> headers, boolean endStream);

        /**
         * Called when a response message is received.
         *
         * <p>The raw bytes from the transport have been stripped of framing,
         * but not yet deserialized.
         *
         * @param message the decoded (frame-stripped) message bytes
         */
        void onMessage(byte[] message);

        /**
         * Called when the call completes successfully (no more messages).
         */
        void onComplete();

        /**
         * Called when the call fails with an error.
         *
         * @param cause the exception that caused the failure
         */
        void onError(Throwable cause);
    }
}
