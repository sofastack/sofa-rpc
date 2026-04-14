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

import com.google.protobuf.CodedOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Tests for {@link GrpcCodec}.
 *
 * <p>This test class serves double duty:
 * <ol>
 *   <li>Unit tests for GrpcCodec's own framing logic</li>
 *   <li>Backward compatibility tests against the reference gRPC wire format
 *       (M7 requirement — verifies that GrpcCodec can decode frames produced
 *       by grpc-java and vice versa)</li>
 * </ol>
 */
public class GrpcCodecTest {

    // ── Frame header tests ─────────────────────────────────────────────────────

    @Test
    public void testAddGrpcFrameHeaderUncompressed() {
        byte[] payload = { 0x01, 0x02, 0x03, 0x04 };
        byte[] framed = GrpcCodec.addGrpcFrameHeader(payload, false);

        Assert.assertEquals(GrpcCodec.GRPC_FRAME_HEADER_LEN + payload.length, framed.length);
        Assert.assertEquals(0, framed[0]); // compress flag = 0
        Assert.assertEquals(0, framed[1]); // length[3] = MSB
        Assert.assertEquals(0, framed[2]);
        Assert.assertEquals(0, framed[3]);
        Assert.assertEquals(4, framed[4]); // length = 4
        Assert.assertArrayEquals(payload, slice(framed, 5, framed.length));
    }

    @Test
    public void testAddGrpcFrameHeaderLargePayload() {
        byte[] payload = new byte[300];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }
        byte[] framed = GrpcCodec.addGrpcFrameHeader(payload, false);
        Assert.assertEquals(5 + 300, framed.length);
        Assert.assertEquals(0, framed[0]); // compress=0
        Assert.assertEquals(0, framed[1]); // 300 >> 24 = 0
        Assert.assertEquals(0, framed[2]); // 300 >> 16 = 0
        Assert.assertEquals(1, framed[3]); // 300 >> 8  = 1
        Assert.assertEquals(44, framed[4]); // 300 & 0xFF = 44
    }

    @Test
    public void testStripGrpcFrameHeader() throws IOException {
        byte[] payload = { 0x0A, 0x03, 'f', 'o', 'o' };
        byte[] framed = GrpcCodec.addGrpcFrameHeader(payload, false);
        byte[] stripped = GrpcCodec.stripGrpcFrameHeader(framed);
        Assert.assertArrayEquals(payload, stripped);
    }

    @Test(expected = IOException.class)
    public void testStripGrpcFrameHeaderTooShort() throws IOException {
        GrpcCodec.stripGrpcFrameHeader(new byte[] { 0x00, 0x00 });
    }

    @Test(expected = IOException.class)
    public void testStripGrpcFrameHeaderNull() throws IOException {
        GrpcCodec.stripGrpcFrameHeader(null);
    }

    @Test(expected = IOException.class)
    public void testStripGrpcFrameHeaderCompressedNotSupported() throws IOException {
        byte[] payload = { 0x01, 0x02 };
        byte[] framed = { 0x01, 0x00, 0x00, 0x00, 0x02, 0x01, 0x02 }; // compress=1
        GrpcCodec.stripGrpcFrameHeader(framed);
    }

    // ── RpcCodec interface tests ───────────────────────────────────────────────

    @Test
    public void testEncodeMessageRoundTrip() throws IOException {
        GrpcCodec codec = GrpcCodec.INSTANCE;
        byte[] payload = { 0x01, 0x02, 0x03 };
        byte[] framed = codec.encodeMessage(payload, false);
        RpcCodec.DecodedMessage decoded = codec.decodeMessage(framed);
        Assert.assertArrayEquals(payload, decoded.getPayload());
        Assert.assertFalse(decoded.isCompressed());
    }

    @Test
    public void testDecodeMessageEmptyPayload() throws IOException {
        GrpcCodec codec = GrpcCodec.INSTANCE;
        byte[] framed = codec.encodeMessage(new byte[0], false);
        RpcCodec.DecodedMessage decoded = codec.decodeMessage(framed);
        Assert.assertEquals(0, decoded.getPayload().length);
    }

    // ── triple.Request encode/decode ──────────────────────────────────────────

    /**
     * Encodes a triple.Request manually using protobuf CodedOutputStream (simulates grpc-java),
     * then decodes with GrpcCodec, verifying backward compatibility.
     */
    @Test
    public void testDecodeRequest_backwardCompatWithGrpcJava() throws IOException {
        String serializeType = "hessian2";
        byte[] argBytes = { 0x01, 0x02, 0x03 };
        String argType = "com.example.HelloRequest";

        // Simulate what grpc-java sends: proto-encode the triple.Request
        byte[] proto = buildTripleRequestProto(serializeType, argBytes, argType);
        byte[] framed = GrpcCodec.addGrpcFrameHeader(proto, false);

        GrpcCodec.GrpcRequest decoded = GrpcCodec.decodeRequest(framed);

        Assert.assertEquals(serializeType, decoded.serializeType);
        Assert.assertEquals(1, decoded.args.size());
        Assert.assertArrayEquals(argBytes, decoded.args.get(0));
        Assert.assertEquals(1, decoded.argTypes.size());
        Assert.assertEquals(argType, decoded.argTypes.get(0));
    }

    @Test
    public void testDecodeRequest_multipleArgs() throws IOException {
        byte[] proto = buildTripleRequestProtoMultipleArgs("protobuf",
            new byte[][] { { 0x01, 0x02 }, { 0x03, 0x04 } },
            new String[] { "com.example.Req1", "com.example.Req2" });
        byte[] framed = GrpcCodec.addGrpcFrameHeader(proto, false);

        GrpcCodec.GrpcRequest decoded = GrpcCodec.decodeRequest(framed);

        Assert.assertEquals("protobuf", decoded.serializeType);
        Assert.assertEquals(2, decoded.args.size());
        Assert.assertArrayEquals(new byte[] { 0x01, 0x02 }, decoded.args.get(0));
        Assert.assertArrayEquals(new byte[] { 0x03, 0x04 }, decoded.args.get(1));
        Assert.assertEquals(2, decoded.argTypes.size());
        Assert.assertEquals("com.example.Req1", decoded.argTypes.get(0));
        Assert.assertEquals("com.example.Req2", decoded.argTypes.get(1));
    }

    @Test
    public void testDecodeRequest_withoutFrameHeader() throws IOException {
        byte[] proto = buildTripleRequestProto("hessian2", new byte[] { 0x0A }, "java.lang.String");
        GrpcCodec.GrpcRequest decoded = GrpcCodec.decodeRequest(proto);
        Assert.assertEquals("hessian2", decoded.serializeType);
        Assert.assertEquals(1, decoded.args.size());
    }

    // ── triple.Response encode/decode ─────────────────────────────────────────

    @Test
    public void testEncodeDecodeResponse() throws IOException {
        String serializeType = "hessian2";
        byte[] responseData = { 0x05, 0x06, 0x07 };
        String responseType = "java.lang.String";

        byte[] framed = GrpcCodec.encodeResponse(serializeType, responseData, responseType);

        // Verify frame header
        Assert.assertEquals(0, framed[0]); // compress=0

        GrpcCodec.GrpcResponse decoded = GrpcCodec.decodeResponse(framed);
        Assert.assertEquals(serializeType, decoded.serializeType);
        Assert.assertArrayEquals(responseData, decoded.data);
        Assert.assertEquals(responseType, decoded.type);
    }

    @Test
    public void testEncodeDecodeResponse_backwardCompatWithGrpcJava() throws IOException {
        // Simulate grpc-java sending a response: proto-encode triple.Response manually
        String serializeType = "hessian2";
        byte[] data = { 0x01, 0x02, 0x03, 0x04 };
        String type = "com.example.HelloResponse";

        byte[] proto = buildTripleResponseProto(serializeType, data, type);
        byte[] framed = GrpcCodec.addGrpcFrameHeader(proto, false);

        GrpcCodec.GrpcResponse decoded = GrpcCodec.decodeResponse(framed);
        Assert.assertEquals(serializeType, decoded.serializeType);
        Assert.assertArrayEquals(data, decoded.data);
        Assert.assertEquals(type, decoded.type);
    }

    @Test
    public void testEncodeDecodeResponse_nullSerializeType() throws IOException {
        byte[] framed = GrpcCodec.encodeResponse(null, new byte[] { 0x01 }, "type");
        GrpcCodec.GrpcResponse decoded = GrpcCodec.decodeResponse(framed);
        Assert.assertEquals("", decoded.serializeType);
        Assert.assertArrayEquals(new byte[] { 0x01 }, decoded.data);
    }

    @Test
    public void testEncodeDecodeResponse_nullData() throws IOException {
        byte[] framed = GrpcCodec.encodeResponse("hessian2", null, "type");
        GrpcCodec.GrpcResponse decoded = GrpcCodec.decodeResponse(framed);
        Assert.assertEquals(0, decoded.data.length);
    }

    // ── Round-trip symmetry tests ─────────────────────────────────────────────

    @Test
    public void testEncodeDecodeRoundTrip_variousPayloadSizes() throws IOException {
        GrpcCodec codec = GrpcCodec.INSTANCE;
        int[] sizes = { 0, 1, 4, 5, 100, 255, 256, 65535 };
        for (int size : sizes) {
            byte[] payload = new byte[size];
            for (int i = 0; i < size; i++) {
                payload[i] = (byte) (i & 0xFF);
            }
            byte[] framed = codec.encodeMessage(payload, false);
            RpcCodec.DecodedMessage decoded = codec.decodeMessage(framed);
            Assert.assertEquals("size=" + size, size, decoded.getPayload().length);
            if (size > 0) {
                Assert.assertArrayEquals("size=" + size, payload, decoded.getPayload());
            }
        }
    }

    // ── Helper: manually build triple.Request proto ───────────────────────────

    private byte[] buildTripleRequestProto(String serializeType, byte[] argBytes,
                                           String argType) throws IOException {
        return buildTripleRequestProtoMultipleArgs(serializeType, new byte[][] { argBytes },
            new String[] { argType });
    }

    private byte[] buildTripleRequestProtoMultipleArgs(String serializeType, byte[][] args,
                                                       String[] argTypes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(baos);
        if (serializeType != null && !serializeType.isEmpty()) {
            out.writeString(1, serializeType);
        }
        for (byte[] arg : args) {
            out.writeByteArray(2, arg);
        }
        for (String type : argTypes) {
            out.writeString(3, type);
        }
        out.flush();
        return baos.toByteArray();
    }

    private byte[] buildTripleResponseProto(String serializeType, byte[] data,
                                            String type) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(baos);
        if (serializeType != null && !serializeType.isEmpty()) {
            out.writeString(1, serializeType);
        }
        if (data != null && data.length > 0) {
            out.writeByteArray(2, data);
        }
        if (type != null && !type.isEmpty()) {
            out.writeString(3, type);
        }
        out.flush();
        return baos.toByteArray();
    }

    private byte[] slice(byte[] src, int from, int to) {
        byte[] result = new byte[to - from];
        System.arraycopy(src, from, result, 0, to - from);
        return result;
    }
}
