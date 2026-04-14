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
package com.alipay.sofa.rpc.transport.triple.codec;

import java.io.IOException;

/**
 * Abstraction of the Codec layer in the three-tier separation architecture.
 *
 * <p>The Codec layer sits between Transport (RpcStream) and Invocation (RpcCall):
 * <pre>
 *   RpcCall  (invocation semantics)
 *      |
 *   RpcCodec (encoding/decoding) ← this interface
 *      |
 *   RpcStream (transport)
 * </pre>
 *
 * <p>An {@code RpcCodec} is responsible for encoding/decoding the wire format of messages,
 * independently of the transport protocol. The default implementation ({@link GrpcCodec})
 * implements the gRPC 5-byte frame prefix and protobuf-encoded Request/Response envelope.
 *
 * <p>Implementations must be stateless and thread-safe.
 *
 * @see GrpcCodec
 */
public interface RpcCodec {

    /**
     * Encodes a raw payload into a transport-ready byte array.
     *
     * <p>The encoding includes any framing required by the protocol (e.g., the 5-byte
     * gRPC frame header). The returned bytes are ready to be passed to
     * {@link com.alipay.sofa.rpc.transport.triple.stream.RpcStream#writeMessage}.
     *
     * @param payload    the serialized payload to encode (must not be null)
     * @param compressed whether the payload should be marked as compressed
     * @return the encoded frame bytes, including any protocol-level framing
     * @throws IOException if encoding fails
     */
    byte[] encodeMessage(byte[] payload, boolean compressed) throws IOException;

    /**
     * Decodes transport bytes back into a raw payload.
     *
     * <p>Strips any protocol-level framing (e.g., the 5-byte gRPC frame header) and
     * returns the inner payload for further deserialization by the invocation layer.
     *
     * @param frameBytes the raw bytes received from the transport layer (must not be null)
     * @return a {@link DecodedMessage} containing the payload and compression flag
     * @throws IOException if decoding fails or the frame is malformed
     */
    DecodedMessage decodeMessage(byte[] frameBytes) throws IOException;

    /**
     * Represents a decoded message with payload and compression metadata.
     */
    class DecodedMessage {

        private final byte[]  payload;
        private final boolean compressed;

        public DecodedMessage(byte[] payload, boolean compressed) {
            this.payload = payload;
            this.compressed = compressed;
        }

        /**
         * Returns the inner payload bytes (without framing).
         *
         * @return the payload bytes
         */
        public byte[] getPayload() {
            return payload;
        }

        /**
         * Returns whether the payload was marked as compressed in the frame header.
         *
         * @return true if compressed
         */
        public boolean isCompressed() {
            return compressed;
        }
    }
}
