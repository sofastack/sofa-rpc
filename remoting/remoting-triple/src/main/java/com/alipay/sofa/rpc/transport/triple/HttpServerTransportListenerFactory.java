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
package com.alipay.sofa.rpc.transport.triple;

import com.alipay.sofa.rpc.transport.triple.stream.RpcStream;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStreamListener;

/**
 * Factory for creating server-side {@link RpcStreamListener} instances.
 *
 * <p>This factory is the integration point between the transport layer
 * ({@link Http2StreamHandler}) and the invocation layer (RPC call handling).
 * For each new inbound HTTP/2 stream, {@link Http2StreamHandler} calls
 * {@link #newListener(RpcStream)} to create a listener that handles the
 * RPC call lifecycle for that stream.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *   <li>Decoding the request from the {@code RpcStream}</li>
 *   <li>Dispatching to the appropriate server-side invoker</li>
 *   <li>Writing the response back through the {@code RpcStream}</li>
 * </ul>
 *
 * <p>This factory pattern allows the transport handler to be decoupled from
 * the specific server-side RPC framework (SOFARPC invoker, mock, etc.).
 *
 * <p>Implementations must be thread-safe; {@link #newListener} may be called
 * concurrently from the Netty EventLoop for different streams.
 *
 * @see Http2StreamHandler
 */
public interface HttpServerTransportListenerFactory {

    /**
     * Creates a new {@link RpcStreamListener} for the given inbound stream.
     *
     * <p>This method is called once per new HTTP/2 stream, from the Netty EventLoop.
     * The returned listener will receive all inbound events (headers, data, completion)
     * for the stream.
     *
     * @param stream the inbound transport stream (must not be null)
     * @return a new listener to handle events on this stream
     */
    RpcStreamListener newListener(RpcStream stream);
}
