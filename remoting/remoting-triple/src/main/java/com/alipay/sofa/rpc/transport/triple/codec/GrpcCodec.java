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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * gRPC wire protocol codec implementation.
 *
 * <p>Implements the gRPC Length-Prefixed Message framing as defined in the gRPC specification:
 * <pre>
 *   Length-Prefixed-Message → Compressed-Flag Message-Length Message
 *   Compressed-Flag         → 0 / 1   (1 byte, 0 = uncompressed)
 *   Message-Length          → 4 bytes (big-endian unsigned 32-bit integer)
 *   Message                 → bytes
 * </pre>
 *
 * <p>This class also provides utilities for encoding/decoding the SOFARPC triple.Request
 * and triple.Response protobuf envelopes without any dependency on {@code grpc-netty-shaded}.
 * Uses {@code com.google.protobuf.CodedInputStream} / {@code CodedOutputStream} directly.
 *
 * <p>Wire format of {@code triple.Request} (protobuf field numbers):
 * <pre>
 *   field 1 (string):  serializeType   — serialization type identifier (e.g., "hessian2")
 *   field 2 (bytes):   args            — serialized argument bytes (repeated)
 *   field 3 (string):  argTypes        — argument type names (repeated)
 * </pre>
 *
 * <p>Wire format of {@code triple.Response} (protobuf field numbers):
 * <pre>
 *   field 1 (string):  serializeType   — serialization type identifier
 *   field 2 (bytes):   data            — serialized response data
 *   field 3 (string):  type            — response type name
 * </pre>
 *
 * <p>This class is stateless and thread-safe.
 *
 * @see RpcCodec
 */
public class GrpcCodec implements RpcCodec {

    /** Length of the gRPC frame header in bytes (1 compress flag + 4 length bytes). */
    public static final int       GRPC_FRAME_HEADER_LEN         = 5;

    /** Singleton instance (stateless, safe to share). */
    public static final GrpcCodec INSTANCE                      = new GrpcCodec();

    // Protobuf field numbers for triple.Request
    private static final int      FIELD_SERIALIZE_TYPE          = 1;
    private static final int      FIELD_ARGS                    = 2;
    private static final int      FIELD_ARG_TYPES               = 3;

    // Protobuf field numbers for triple.Response
    private static final int      RESPONSE_FIELD_SERIALIZE_TYPE = 1;
    private static final int      RESPONSE_FIELD_DATA           = 2;
    private static final int      RESPONSE_FIELD_TYPE           = 3;

    /**
     * Wraps a payload with a 5-byte gRPC frame header and returns the framed bytes.
     *
     * @param payload    the inner payload (without framing)
     * @param compressed if true, sets the compression flag in the frame header
     * @return the framed message ready for writing to the transport
     */
    @Override
    public byte[] encodeMessage(byte[] payload, boolean compressed) {
        return addGrpcFrameHeader(payload, compressed);
    }

    /**
     * Strips the 5-byte gRPC frame header from received bytes and returns the inner payload.
     *
     * @param frameBytes the full framed bytes received from the transport
     * @return a {@link DecodedMessage} containing the payload and compression flag
     * @throws IOException if the frame is too short or has an unsupported compression flag
     */
    @Override
    public DecodedMessage decodeMessage(byte[] frameBytes) throws IOException {
        if (frameBytes == null || frameBytes.length < GRPC_FRAME_HEADER_LEN) {
            throw new IOException(
                "gRPC frame too short: " + (frameBytes == null ? 0 : frameBytes.length));
        }
        boolean compressed = frameBytes[0] == 1;
        if (compressed) {
            throw new IOException(
                "gRPC compressed frames are not yet supported (compress flag=1)");
        }
        int length = readBigEndianInt(frameBytes, 1);
        if (frameBytes.length < GRPC_FRAME_HEADER_LEN + length) {
            throw new IOException(
                "gRPC frame data truncated: expected " + (GRPC_FRAME_HEADER_LEN + length)
                    + " bytes but got " + frameBytes.length);
        }
        byte[] payload = new byte[length];
        System.arraycopy(frameBytes, GRPC_FRAME_HEADER_LEN, payload, 0, length);
        return new DecodedMessage(payload, false);
    }

    // ── gRPC frame utilities ──────────────────────────────────────────────────

    /**
     * Adds a 5-byte gRPC frame header to the given payload bytes.
     *
     * <p>The header layout: {@code [compress(0)] [len>>24] [len>>16] [len>>8] [len&0xFF]}
     *
     * @param payload    the inner message bytes
     * @param compressed whether to set the compression flag
     * @return the payload prefixed with the 5-byte gRPC frame header
     */
    public static byte[] addGrpcFrameHeader(byte[] payload, boolean compressed) {
        int len = payload.length;
        byte[] framed = new byte[GRPC_FRAME_HEADER_LEN + len];
        framed[0] = compressed ? (byte) 1 : (byte) 0;
        framed[1] = (byte) (len >> 24);
        framed[2] = (byte) (len >> 16);
        framed[3] = (byte) (len >> 8);
        framed[4] = (byte) (len);
        System.arraycopy(payload, 0, framed, GRPC_FRAME_HEADER_LEN, len);
        return framed;
    }

    /**
     * Strips the 5-byte gRPC frame header from {@code frameBytes} and returns the payload.
     *
     * @param frameBytes the full framed message
     * @return the inner payload bytes
     * @throws IOException if the frame is malformed
     */
    public static byte[] stripGrpcFrameHeader(byte[] frameBytes) throws IOException {
        if (frameBytes == null || frameBytes.length < GRPC_FRAME_HEADER_LEN) {
            throw new IOException(
                "gRPC frame too short: " + (frameBytes == null ? 0 : frameBytes.length));
        }
        if (frameBytes[0] != 0) {
            throw new IOException("Compressed gRPC frames not supported (compress flag="
                + frameBytes[0] + ")");
        }
        int length = readBigEndianInt(frameBytes, 1);
        byte[] payload = new byte[length];
        System.arraycopy(frameBytes, GRPC_FRAME_HEADER_LEN, payload, 0, length);
        return payload;
    }

    // ── triple.Request encode/decode ──────────────────────────────────────────

    /**
     * Decodes a {@code triple.Request} protobuf message from the given bytes.
     *
     * <p>The input may either be the raw proto bytes (without frame header) or the full
     * framed bytes (with the 5-byte header). If the first byte is 0 and length ≥ 5,
     * the frame header is stripped first.
     *
     * @param data the encoded request bytes (with or without gRPC frame header)
     * @return the decoded {@link GrpcRequest} object
     * @throws IOException if decoding fails
     */
    public static GrpcRequest decodeRequest(byte[] data) throws IOException {
        byte[] proto = stripIfFramed(data);
        return parseRequest(proto);
    }

    private static GrpcRequest parseRequest(byte[] proto) throws IOException {
        CodedInputStream in = CodedInputStream.newInstance(proto);
        String serializeType = "";
        List<byte[]> args = new ArrayList<byte[]>();
        List<String> argTypes = new ArrayList<String>();

        int tag;
        while ((tag = in.readTag()) != 0) {
            int fieldNumber = tag >>> 3;
            switch (fieldNumber) {
                case FIELD_SERIALIZE_TYPE:
                    serializeType = in.readString();
                    break;
                case FIELD_ARGS:
                    args.add(in.readByteArray());
                    break;
                case FIELD_ARG_TYPES:
                    argTypes.add(in.readString());
                    break;
                default:
                    in.skipField(tag);
                    break;
            }
        }
        return new GrpcRequest(serializeType, args, argTypes);
    }

    /**
     * Encodes a {@code triple.Response} protobuf message and wraps it with a gRPC frame header.
     *
     * @param serializeType the serialization type identifier (e.g., "hessian2")
     * @param responseData  the serialized response data
     * @param responseType  the response type name
     * @return the framed response bytes (5-byte header + proto payload)
     * @throws IOException if encoding fails
     */
    public static byte[] encodeResponse(String serializeType, byte[] responseData,
                                        String responseType) throws IOException {
        byte[] proto = buildResponseProto(serializeType, responseData, responseType);
        return addGrpcFrameHeader(proto, false);
    }

    private static byte[] buildResponseProto(String serializeType, byte[] responseData,
                                             String responseType) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(baos);
        if (serializeType != null && !serializeType.isEmpty()) {
            out.writeString(RESPONSE_FIELD_SERIALIZE_TYPE, serializeType);
        }
        if (responseData != null && responseData.length > 0) {
            out.writeByteArray(RESPONSE_FIELD_DATA, responseData);
        }
        if (responseType != null && !responseType.isEmpty()) {
            out.writeString(RESPONSE_FIELD_TYPE, responseType);
        }
        out.flush();
        return baos.toByteArray();
    }

    /**
     * Decodes a {@code triple.Response} from the given bytes.
     *
     * @param data the encoded response bytes (with or without gRPC frame header)
     * @return the decoded {@link GrpcResponse} object
     * @throws IOException if decoding fails
     */
    public static GrpcResponse decodeResponse(byte[] data) throws IOException {
        byte[] proto = stripIfFramed(data);
        return parseResponse(proto);
    }

    private static GrpcResponse parseResponse(byte[] proto) throws IOException {
        CodedInputStream in = CodedInputStream.newInstance(proto);
        String serializeType = "";
        byte[] responseData = null;
        String responseType = "";

        int tag;
        while ((tag = in.readTag()) != 0) {
            int fieldNumber = tag >>> 3;
            switch (fieldNumber) {
                case RESPONSE_FIELD_SERIALIZE_TYPE:
                    serializeType = in.readString();
                    break;
                case RESPONSE_FIELD_DATA:
                    responseData = in.readByteArray();
                    break;
                case RESPONSE_FIELD_TYPE:
                    responseType = in.readString();
                    break;
                default:
                    in.skipField(tag);
                    break;
            }
        }
        return new GrpcResponse(serializeType, responseData != null ? responseData : new byte[0],
            responseType);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static byte[] stripIfFramed(byte[] data) throws IOException {
        if (data != null && data.length >= GRPC_FRAME_HEADER_LEN && data[0] == 0) {
            int expectedLen = readBigEndianInt(data, 1);
            if (data.length == GRPC_FRAME_HEADER_LEN + expectedLen) {
                return stripGrpcFrameHeader(data);
            }
        }
        return data;
    }

    private static int readBigEndianInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
            | ((bytes[offset + 1] & 0xFF) << 16)
            | ((bytes[offset + 2] & 0xFF) << 8)
            | (bytes[offset + 3] & 0xFF);
    }

    // ── DTO classes ──────────────────────────────────────────────────────────

    /**
     * Decoded triple.Request envelope.
     */
    public static class GrpcRequest {
        /** Serialization type identifier (e.g., "hessian2", "protobuf"). */
        public final String       serializeType;
        /** Serialized argument byte arrays (one per method argument). */
        public final List<byte[]> args;
        /** Argument type names corresponding to {@link #args}. */
        public final List<String> argTypes;

        GrpcRequest(String serializeType, List<byte[]> args, List<String> argTypes) {
            this.serializeType = serializeType;
            this.args = args;
            this.argTypes = argTypes;
        }
    }

    /**
     * Decoded triple.Response envelope.
     */
    public static class GrpcResponse {
        /** Serialization type identifier. */
        public final String serializeType;
        /** Serialized response data bytes. */
        public final byte[] data;
        /** Response type name. */
        public final String type;

        GrpcResponse(String serializeType, byte[] data, String type) {
            this.serializeType = serializeType;
            this.data = data;
            this.type = type;
        }
    }
}
