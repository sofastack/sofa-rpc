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

import com.alipay.sofa.rpc.transport.triple.codec.GrpcCodec;
import com.alipay.sofa.rpc.transport.triple.codec.RpcCodec;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStream;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStreamListener;

import java.io.IOException;
import java.util.Map;

/**
 * Unary (single request / single response) RPC call implementation.
 *
 * <p>A UnaryRpcCall manages the lifecycle of a unary RPC:
 * <ol>
 *   <li>Caller invokes {@link #start} with response headers and a listener</li>
 *   <li>Caller invokes {@link #sendMessage} exactly once with the response data</li>
 *   <li>{@link #halfClose} is called automatically to finish the stream</li>
 * </ol>
 *
 * <p>On the inbound side, the call registers itself as a {@link RpcStreamListener} on the
 * underlying {@link RpcStream} and dispatches received frames through the {@link RpcCallListener}.
 *
 * <p>This class uses {@link GrpcCodec} for all encoding/decoding.
 * It has no dependency on {@code grpc-netty-shaded}.
 *
 * <p>Thread safety: write operations (sendMessage, halfClose, cancel) should be called
 * from a single thread. The listener callbacks are delivered on the Netty EventLoop thread.
 */
public class UnaryRpcCall implements RpcCall, RpcStreamListener {

    private final RpcStream stream;
    private final RpcCodec  codec;
    private RpcCallListener listener;

    /**
     * Creates a new UnaryRpcCall backed by the given stream.
     *
     * @param stream the underlying transport stream (must not be null)
     */
    public UnaryRpcCall(RpcStream stream) {
        this(stream, GrpcCodec.INSTANCE);
    }

    /**
     * Creates a new UnaryRpcCall with a custom codec (primarily for testing).
     *
     * @param stream the underlying transport stream
     * @param codec  the codec for encoding/decoding frames
     */
    public UnaryRpcCall(RpcStream stream, RpcCodec codec) {
        if (stream == null) {
            throw new IllegalArgumentException("stream must not be null");
        }
        this.stream = stream;
        this.codec = codec;
    }

    @Override
    public void start(Map<String, String> headers, RpcCallListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        this.listener = listener;
        stream.setListener(this);
        stream.writeHeaders(headers, false);
    }

    @Override
    public void sendMessage(byte[] message) {
        try {
            byte[] framed = codec.encodeMessage(message, false);
            stream.writeMessage(framed, false);
        } catch (IOException e) {
            cancel(e);
        }
    }

    @Override
    public void halfClose() {
        stream.halfClose();
    }

    @Override
    public void cancel(Throwable cause) {
        stream.cancel(cause);
    }

    @Override
    public boolean isReady() {
        return stream.isWritable();
    }

    // ── RpcStreamListener callbacks ────────────────────────────────────────────

    @Override
    public void onHeaders(Map<String, String> headers, boolean endStream) {
        RpcCallListener l = listener;
        if (l != null) {
            l.onHeaders(headers, endStream);
        }
    }

    @Override
    public void onMessage(byte[] data) {
        RpcCallListener l = listener;
        if (l == null) {
            return;
        }
        try {
            RpcCodec.DecodedMessage decoded = codec.decodeMessage(data);
            l.onMessage(decoded.getPayload());
        } catch (IOException e) {
            l.onError(e);
        }
    }

    @Override
    public void onComplete() {
        RpcCallListener l = listener;
        if (l != null) {
            l.onComplete();
        }
    }

    @Override
    public void onError(Throwable cause) {
        RpcCallListener l = listener;
        if (l != null) {
            l.onError(cause);
        }
    }

    @Override
    public void onWritabilityChanged() {
        // No-op for unary calls — no back-pressure feedback needed
    }

    /**
     * Returns the underlying {@link RpcStream} for diagnostic purposes.
     *
     * @return the stream backing this call
     */
    public RpcStream getStream() {
        return stream;
    }
}
