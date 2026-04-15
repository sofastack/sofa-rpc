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
 * Unit tests for {@link ServerStreamRpcCall}.
 *
 * <p>Covers the server-streaming lifecycle: start → multiple sendMessage → halfClose,
 * as well as inbound event dispatching from the client's single request message.
 */
public class ServerStreamRpcCallTest {

    // ── Stub RpcStream ─────────────────────────────────────────────────────────

    static class StubRpcStream implements RpcStream {
        RpcStreamListener               listener;
        final List<byte[]>              sentMessages = new ArrayList<byte[]>();
        final List<Map<String, String>> sentHeaders  = new ArrayList<Map<String, String>>();
        boolean                         halfClosed   = false;
        boolean                         cancelled    = false;
        Throwable                       cancelCause  = null;

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
            halfClosed = true;
        }

        @Override
        public void cancel(Throwable cause) {
            cancelled = true;
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
            receivedHeaders.add(new HashMap<String, String>(headers));
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

    private StubRpcStream       stream;
    private StubCallListener    listener;
    private ServerStreamRpcCall call;

    @Before
    public void setUp() {
        stream = new StubRpcStream();
        listener = new StubCallListener();
        call = new ServerStreamRpcCall(stream);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNullStream() {
        new ServerStreamRpcCall(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartRejectsNullListener() {
        call.start(new HashMap<String, String>(), null);
    }

    // ── start ─────────────────────────────────────────────────────────────────

    @Test
    public void testStartSendsHeadersAndRegistersStreamListener() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(":status", "200");
        headers.put("content-type", "application/grpc+proto");

        call.start(headers, listener);

        Assert.assertEquals(1, stream.sentHeaders.size());
        Assert.assertEquals("200", stream.sentHeaders.get(0).get(":status"));
        // stream.listener should be the call itself (it's a RpcStreamListener)
        Assert.assertSame(call, stream.listener);
    }

    // ── sendMessage — server streaming sends multiple messages ────────────────

    @Test
    public void testSendSingleMessage() {
        call.start(new HashMap<String, String>(), listener);

        byte[] payload = { 0x01, 0x02, 0x03 };
        call.sendMessage(payload);

        Assert.assertEquals(1, stream.sentMessages.size());
        byte[] framed = stream.sentMessages.get(0);
        Assert.assertEquals(GrpcCodec.GRPC_FRAME_HEADER_LEN + payload.length, framed.length);
        Assert.assertEquals(0, framed[0]); // compress=0
        Assert.assertEquals(payload.length, framed[4]);
    }

    @Test
    public void testSendMultipleMessages() {
        call.start(new HashMap<String, String>(), listener);

        byte[] m1 = { 0x01 };
        byte[] m2 = { 0x02, 0x03 };
        byte[] m3 = { 0x04, 0x05, 0x06 };
        call.sendMessage(m1);
        call.sendMessage(m2);
        call.sendMessage(m3);

        Assert.assertEquals(3, stream.sentMessages.size());
        // verify framing of each
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(0, stream.sentMessages.get(i)[0]); // compress=0
        }
    }

    @Test
    public void testSendEmptyMessage() {
        call.start(new HashMap<String, String>(), listener);
        call.sendMessage(new byte[0]);

        Assert.assertEquals(1, stream.sentMessages.size());
        Assert.assertEquals(GrpcCodec.GRPC_FRAME_HEADER_LEN, stream.sentMessages.get(0).length);
    }

    // ── halfClose ─────────────────────────────────────────────────────────────

    @Test
    public void testHalfCloseAfterMultipleMessages() {
        call.start(new HashMap<String, String>(), listener);
        call.sendMessage(new byte[] { 0x01 });
        call.sendMessage(new byte[] { 0x02 });
        call.halfClose();

        Assert.assertEquals(2, stream.sentMessages.size());
        Assert.assertTrue(stream.halfClosed);
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    public void testCancelDelegatesToStream() {
        call.start(new HashMap<String, String>(), listener);
        RuntimeException ex = new RuntimeException("cancel");
        call.cancel(ex);
        Assert.assertTrue(stream.cancelled);
        Assert.assertSame(ex, stream.cancelCause);
    }

    // ── isReady ───────────────────────────────────────────────────────────────

    @Test
    public void testIsReadyDelegatesToStream() {
        Assert.assertTrue(call.isReady());
    }

    @Test
    public void testGetStreamReturnsSameStream() {
        Assert.assertSame(stream, call.getStream());
    }

    // ── Inbound: single request from client ──────────────────────────────────

    @Test
    public void testOnHeadersDispatchedToListener() {
        call.start(new HashMap<String, String>(), listener);

        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put(":method", "POST");
        call.onHeaders(reqHeaders, false);

        Assert.assertEquals(1, listener.receivedHeaders.size());
        Assert.assertEquals("POST", listener.receivedHeaders.get(0).get(":method"));
    }

    @Test
    public void testOnMessageDecodesAndDispatchesToListener() throws IOException {
        call.start(new HashMap<String, String>(), listener);

        byte[] payload = { 0x0A, 0x03, 'f', 'o', 'o' };
        byte[] framed = GrpcCodec.addGrpcFrameHeader(payload, false);
        call.onMessage(framed);

        Assert.assertEquals(1, listener.receivedMessages.size());
        Assert.assertArrayEquals(payload, listener.receivedMessages.get(0));
    }

    @Test
    public void testOnMessageMalformedFrameTriggersOnError() {
        call.start(new HashMap<String, String>(), listener);
        call.onMessage(new byte[] { 0x00, 0x01 }); // truncated — too short

        Assert.assertNotNull(listener.lastError);
        Assert.assertTrue(listener.lastError instanceof IOException);
    }

    @Test
    public void testOnCompleteDispatchedToListener() {
        call.start(new HashMap<String, String>(), listener);
        call.onComplete();
        Assert.assertEquals(1, listener.completeCount);
    }

    @Test
    public void testOnErrorDispatchedToListener() {
        call.start(new HashMap<String, String>(), listener);
        RuntimeException ex = new RuntimeException("stream error");
        call.onError(ex);
        Assert.assertSame(ex, listener.lastError);
    }

    // ── Guard: no listener set ────────────────────────────────────────────────

    @Test
    public void testOnHeadersWithNoListener_doesNotThrow() {
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

    // ── Full server-streaming lifecycle ──────────────────────────────────────

    @Test
    public void testFullServerStreamLifecycle() throws IOException {
        // Server streaming: client sends one request, server replies with 3 responses
        Map<String, String> initHeaders = new HashMap<String, String>();
        initHeaders.put(":status", "200");
        initHeaders.put("content-type", "application/grpc+proto");
        call.start(initHeaders, listener);

        // Simulate inbound: client request headers + 1 request message
        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put(":method", "POST");
        call.onHeaders(reqHeaders, false);

        byte[] requestPayload = { 0x01, 0x02 };
        byte[] requestFramed = GrpcCodec.addGrpcFrameHeader(requestPayload, false);
        call.onMessage(requestFramed);
        call.onComplete(); // client half-closes after sending request

        // Verify request received
        Assert.assertEquals(1, listener.receivedMessages.size());
        Assert.assertArrayEquals(requestPayload, listener.receivedMessages.get(0));
        Assert.assertEquals(1, listener.completeCount);

        // Server sends 3 streaming responses
        call.sendMessage(new byte[] { 0x0A });
        call.sendMessage(new byte[] { 0x0B });
        call.sendMessage(new byte[] { 0x0C });
        call.halfClose(); // server done

        Assert.assertEquals(3, stream.sentMessages.size());
        Assert.assertTrue(stream.halfClosed);
        Assert.assertNull(listener.lastError);
    }
}
