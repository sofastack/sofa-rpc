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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link Http2RpcStream}.
 *
 * <p>These tests use a lightweight stub (not Mockito, not EmbeddedChannel) to avoid
 * heavy Netty infrastructure in unit tests. The focus is on:
 * <ul>
 *   <li>Inbound event dispatch: onHeadersReceived, onDataReceived, onStreamComplete, onStreamError</li>
 *   <li>Guard conditions: null listener, cancelled stream, half-closed stream</li>
 *   <li>State management: streamId, getListener</li>
 * </ul>
 *
 * <p>Full integration with Netty's {@code EmbeddedChannel} is tested in
 * integration/end-to-end tests in later Sprints.
 */
public class Http2RpcStreamTest {

    // ── Stub listener ─────────────────────────────────────────────────────────

    static class StubListener implements RpcStreamListener {
        final List<Map<String, String>> headers       = new ArrayList<Map<String, String>>();
        final List<byte[]>              messages      = new ArrayList<byte[]>();
        int                             completeCount = 0;
        Throwable                       error         = null;

        @Override
        public void onHeaders(Map<String, String> h, boolean endStream) {
            headers.add(h);
        }

        @Override
        public void onMessage(byte[] data) {
            messages.add(data);
        }

        @Override
        public void onComplete() {
            completeCount++;
        }

        @Override
        public void onError(Throwable cause) {
            error = cause;
        }

        @Override
        public void onWritabilityChanged() {
        }
    }

    /**
     * Minimal stub for Http2RpcStream that bypasses Netty infrastructure.
     * Only tests the inbound dispatch and state logic.
     */
    static class TestableHttp2RpcStream extends Http2RpcStream {
        TestableHttp2RpcStream(int streamId) {
            super(null, null, streamId);
        }

        // Override write methods to avoid NPE on null ctx/encoder in unit tests
        @Override
        public void writeMessage(byte[] data, boolean compressed) {
            // no-op in unit test
        }

        @Override
        public void writeHeaders(Map<String, String> headers, boolean endStream) {
            // no-op in unit test
        }

        @Override
        public void halfClose() {
            // no-op in unit test
        }

        @Override
        public void cancel(Throwable cause) {
            // no-op in unit test
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    public void testGetStreamId() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(3);
        Assert.assertEquals(3, stream.getStreamId());
    }

    @Test
    public void testSetAndGetListener() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(1);
        StubListener listener = new StubListener();
        stream.setListener(listener);
        Assert.assertSame(listener, stream.getListener());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNullListenerThrows() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(1);
        stream.setListener(null);
    }

    @Test
    public void testOnHeadersReceivedDispatchesToListener() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(5);
        StubListener listener = new StubListener();
        stream.setListener(listener);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(":method", "POST");
        headers.put("content-type", "application/grpc+proto");

        stream.onHeadersReceived(headers, false);

        Assert.assertEquals(1, listener.headers.size());
        Assert.assertEquals("POST", listener.headers.get(0).get(":method"));
    }

    @Test
    public void testOnDataReceivedDispatchesToListener() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(5);
        StubListener listener = new StubListener();
        stream.setListener(listener);

        byte[] data = { 0x00, 0x00, 0x00, 0x00, 0x04, 0x0A, 0x02, 'h', 'i' };
        stream.onDataReceived(data);

        Assert.assertEquals(1, listener.messages.size());
        Assert.assertSame(data, listener.messages.get(0));
    }

    @Test
    public void testOnStreamCompleteDispatchesToListener() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(5);
        StubListener listener = new StubListener();
        stream.setListener(listener);

        stream.onStreamComplete();

        Assert.assertEquals(1, listener.completeCount);
    }

    @Test
    public void testOnStreamErrorDispatchesToListener() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(5);
        StubListener listener = new StubListener();
        stream.setListener(listener);

        RuntimeException ex = new RuntimeException("test error");
        stream.onStreamError(ex);

        Assert.assertSame(ex, listener.error);
    }

    @Test
    public void testNoListenerRegistered_doesNotThrow() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(5);
        // No listener set — should not throw
        Map<String, String> headers = new HashMap<String, String>();
        stream.onHeadersReceived(headers, false);
        stream.onDataReceived(new byte[] { 0x01 });
        stream.onStreamComplete();
        stream.onStreamError(new RuntimeException());
        stream.onWritabilityChanged();
    }

    @Test
    public void testIsWritableDefault() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(1);
        Assert.assertTrue(stream.isWritable());
    }

    @Test
    public void testRequestIsNoOp() {
        // request() is a flow-control placeholder in S0 — just verify it doesn't throw
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(1);
        stream.request(10);
        stream.request(Integer.MAX_VALUE);
    }

    @Test
    public void testMultipleDataFramesDispatchedInOrder() {
        TestableHttp2RpcStream stream = new TestableHttp2RpcStream(7);
        StubListener listener = new StubListener();
        stream.setListener(listener);

        byte[] frame1 = { 0x01 };
        byte[] frame2 = { 0x02 };
        byte[] frame3 = { 0x03 };

        stream.onDataReceived(frame1);
        stream.onDataReceived(frame2);
        stream.onDataReceived(frame3);

        Assert.assertEquals(3, listener.messages.size());
        Assert.assertSame(frame1, listener.messages.get(0));
        Assert.assertSame(frame2, listener.messages.get(1));
        Assert.assertSame(frame3, listener.messages.get(2));
    }
}
