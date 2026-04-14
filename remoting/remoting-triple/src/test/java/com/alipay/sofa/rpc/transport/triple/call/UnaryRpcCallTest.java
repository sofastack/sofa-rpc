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
import com.alipay.sofa.rpc.transport.triple.stream.RpcStream;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStreamListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link UnaryRpcCall}.
 *
 * <p>Uses a stub {@link RpcStream} to verify the interaction between UnaryRpcCall
 * and the underlying transport without requiring Netty infrastructure.
 */
public class UnaryRpcCallTest {

    // ── Stub RpcStream ─────────────────────────────────────────────────────────

    static class StubRpcStream implements RpcStream {
        RpcStreamListener               listener;
        final List<byte[]>              sentMessages    = new ArrayList<byte[]>();
        final List<Map<String, String>> sentHeaders     = new ArrayList<Map<String, String>>();
        boolean                         halfCloseCalled = false;
        boolean                         cancelCalled    = false;
        Throwable                       cancelCause     = null;

        @Override
        public void writeMessage(byte[] data, boolean compressed) {
            sentMessages.add(data.clone());
        }

        @Override
        public void writeHeaders(Map<String, String> headers, boolean endStream) {
            sentHeaders.add(new HashMap<String, String>(headers));
        }

        @Override
        public void halfClose() {
            halfCloseCalled = true;
        }

        @Override
        public void cancel(Throwable cause) {
            cancelCalled = true;
            cancelCause = cause;
        }

        @Override
        public void setListener(RpcStreamListener listener) {
            this.listener = listener;
        }

        @Override
        public void request(int count) {
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    // ── Stub RpcCallListener ──────────────────────────────────────────────────

    static class StubCallListener implements RpcCall.RpcCallListener {
        final List<Map<String, String>> receivedHeaders  = new ArrayList<Map<String, String>>();
        final List<byte[]>              receivedMessages = new ArrayList<byte[]>();
        int                             completeCount    = 0;
        Throwable                       lastError        = null;

        @Override
        public void onHeaders(Map<String, String> headers, boolean endStream) {
            receivedHeaders.add(headers);
        }

        @Override
        public void onMessage(byte[] message) {
            receivedMessages.add(message.clone());
        }

        @Override
        public void onComplete() {
            completeCount++;
        }

        @Override
        public void onError(Throwable cause) {
            lastError = cause;
        }
    }

    private StubRpcStream    stream;
    private StubCallListener callListener;
    private UnaryRpcCall     call;

    @Before
    public void setUp() {
        stream = new StubRpcStream();
        callListener = new StubCallListener();
        call = new UnaryRpcCall(stream);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNullStream() {
        new UnaryRpcCall(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartRejectsNullListener() {
        call.start(new HashMap<String, String>(), null);
    }

    @Test
    public void testStartSendsHeadersAndRegistersListener() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(":status", "200");
        headers.put("content-type", "application/grpc");

        call.start(headers, callListener);

        Assert.assertEquals(1, stream.sentHeaders.size());
        Assert.assertEquals("200", stream.sentHeaders.get(0).get(":status"));
        Assert.assertSame(call, stream.listener);
    }

    @Test
    public void testSendMessageEncodesWithGrpcFrameHeader() throws IOException {
        call.start(new HashMap<String, String>(), callListener);

        byte[] payload = { 0x01, 0x02, 0x03 };
        call.sendMessage(payload);

        Assert.assertEquals(1, stream.sentMessages.size());
        byte[] framed = stream.sentMessages.get(0);

        // Verify it has 5-byte gRPC header
        Assert.assertEquals(GrpcCodec.GRPC_FRAME_HEADER_LEN + payload.length, framed.length);
        Assert.assertEquals(0, framed[0]); // compress=0
        Assert.assertEquals(payload.length, framed[4]); // length in last byte
    }

    @Test
    public void testHalfCloseCallsStreamHalfClose() {
        call.start(new HashMap<String, String>(), callListener);
        call.halfClose();
        Assert.assertTrue(stream.halfCloseCalled);
    }

    @Test
    public void testCancelCallsStreamCancel() {
        call.start(new HashMap<String, String>(), callListener);
        RuntimeException ex = new RuntimeException("cancel");
        call.cancel(ex);
        Assert.assertTrue(stream.cancelCalled);
        Assert.assertSame(ex, stream.cancelCause);
    }

    @Test
    public void testIsReadyDelegatesToStream() {
        Assert.assertTrue(call.isReady());
    }

    @Test
    public void testGetStreamReturnsSameStream() {
        Assert.assertSame(stream, call.getStream());
    }

    // ── Inbound event handling ─────────────────────────────────────────────────

    @Test
    public void testOnHeadersDispatchedToListener() {
        call.start(new HashMap<String, String>(), callListener);

        Map<String, String> responseHeaders = new HashMap<String, String>();
        responseHeaders.put(":status", "200");
        call.onHeaders(responseHeaders, false);

        Assert.assertEquals(1, callListener.receivedHeaders.size());
    }

    @Test
    public void testOnMessageDecodesGrpcFrameAndDispatchesToListener() throws IOException {
        call.start(new HashMap<String, String>(), callListener);

        byte[] payload = { 0x0A, 0x03, 'h', 'i', '!' };
        byte[] framed = GrpcCodec.addGrpcFrameHeader(payload, false);
        call.onMessage(framed);

        Assert.assertEquals(1, callListener.receivedMessages.size());
        Assert.assertArrayEquals(payload, callListener.receivedMessages.get(0));
    }

    @Test
    public void testOnMessageMalformedFrameTriggersOnError() {
        call.start(new HashMap<String, String>(), callListener);

        // Pass malformed frame (too short)
        call.onMessage(new byte[] { 0x00, 0x01 });

        Assert.assertNotNull(callListener.lastError);
        Assert.assertTrue(callListener.lastError instanceof IOException);
    }

    @Test
    public void testOnCompleteDispatchedToListener() {
        call.start(new HashMap<String, String>(), callListener);
        call.onComplete();
        Assert.assertEquals(1, callListener.completeCount);
    }

    @Test
    public void testOnErrorDispatchedToListener() {
        call.start(new HashMap<String, String>(), callListener);
        RuntimeException ex = new RuntimeException("stream error");
        call.onError(ex);
        Assert.assertSame(ex, callListener.lastError);
    }

    @Test
    public void testOnHeadersWithNoListener_doesNotThrow() {
        // listener not yet set (start not called)
        call.onHeaders(new HashMap<String, String>(), false);
    }

    @Test
    public void testOnCompleteWithNoListener_doesNotThrow() {
        call.onComplete();
    }

    @Test
    public void testOnErrorWithNoListener_doesNotThrow() {
        call.onError(new RuntimeException());
    }

    @Test
    public void testFullUnaryCallLifecycle() throws IOException {
        // Complete unary call lifecycle: start → sendMessage → halfClose → receive response
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(":method", "POST");
        requestHeaders.put("content-type", "application/grpc+proto");

        call.start(requestHeaders, callListener);

        byte[] requestPayload = { 0x01, 0x02, 0x03 };
        call.sendMessage(requestPayload);
        call.halfClose();

        // Simulate server response
        Map<String, String> responseHeaders = new HashMap<String, String>();
        responseHeaders.put(":status", "200");
        call.onHeaders(responseHeaders, false);

        byte[] responsePayload = { 0x04, 0x05, 0x06 };
        byte[] responseFramed = GrpcCodec.addGrpcFrameHeader(responsePayload, false);
        call.onMessage(responseFramed);
        call.onComplete();

        // Verify complete lifecycle
        Assert.assertEquals(1, stream.sentHeaders.size());
        Assert.assertEquals(1, stream.sentMessages.size());
        Assert.assertTrue(stream.halfCloseCalled);
        Assert.assertEquals(1, callListener.receivedHeaders.size());
        Assert.assertEquals(1, callListener.receivedMessages.size());
        Assert.assertArrayEquals(responsePayload, callListener.receivedMessages.get(0));
        Assert.assertEquals(1, callListener.completeCount);
        Assert.assertNull(callListener.lastError);
    }
}
