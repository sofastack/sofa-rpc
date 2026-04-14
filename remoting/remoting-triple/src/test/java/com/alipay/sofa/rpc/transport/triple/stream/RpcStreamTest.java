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
 * Contract tests for {@link RpcStream} and {@link RpcStreamListener} interfaces.
 *
 * <p>Uses a simple mock implementation to verify the interface contracts.
 * These tests ensure the interface is stable and usable before any Netty-specific
 * implementation is tested.
 */
public class RpcStreamTest {

    // ── Mock RpcStreamListener ─────────────────────────────────────────────────

    static class RecordingListener implements RpcStreamListener {
        final List<Map<String, String>> receivedHeaders         = new ArrayList<Map<String, String>>();
        final List<byte[]>              receivedMessages        = new ArrayList<byte[]>();
        int                             completeCount           = 0;
        Throwable                       lastError               = null;
        int                             writabilityChangedCount = 0;

        @Override
        public void onHeaders(Map<String, String> headers, boolean endStream) {
            receivedHeaders.add(new HashMap<String, String>(headers));
        }

        @Override
        public void onMessage(byte[] data) {
            receivedMessages.add(data.clone());
        }

        @Override
        public void onComplete() {
            completeCount++;
        }

        @Override
        public void onError(Throwable cause) {
            lastError = cause;
        }

        @Override
        public void onWritabilityChanged() {
            writabilityChangedCount++;
        }
    }

    // ── Mock RpcStream ─────────────────────────────────────────────────────────

    static class MockRpcStream implements RpcStream {
        final List<byte[]>              sentMessages     = new ArrayList<byte[]>();
        final List<Map<String, String>> sentHeaders      = new ArrayList<Map<String, String>>();
        boolean                         halfClosedCalled = false;
        boolean                         cancelCalled     = false;
        Throwable                       cancelCause      = null;
        int                             requestCount     = 0;
        boolean                         writableState    = true;
        RpcStreamListener               listener;

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
            halfClosedCalled = true;
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
            requestCount += count;
        }

        @Override
        public boolean isWritable() {
            return writableState;
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    public void testListenerReceivesHeaders() {
        RecordingListener listener = new RecordingListener();
        MockRpcStream stream = new MockRpcStream();
        stream.setListener(listener);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(":method", "POST");
        headers.put("content-type", "application/grpc+proto");

        stream.listener.onHeaders(headers, false);

        Assert.assertEquals(1, listener.receivedHeaders.size());
        Assert.assertEquals("POST", listener.receivedHeaders.get(0).get(":method"));
        Assert.assertEquals("application/grpc+proto",
            listener.receivedHeaders.get(0).get("content-type"));
    }

    @Test
    public void testListenerReceivesMessage() {
        RecordingListener listener = new RecordingListener();
        MockRpcStream stream = new MockRpcStream();
        stream.setListener(listener);

        byte[] payload = { 0x00, 0x01, 0x02, 0x03 };
        stream.listener.onMessage(payload);

        Assert.assertEquals(1, listener.receivedMessages.size());
        Assert.assertArrayEquals(payload, listener.receivedMessages.get(0));
    }

    @Test
    public void testListenerReceivesComplete() {
        RecordingListener listener = new RecordingListener();
        MockRpcStream stream = new MockRpcStream();
        stream.setListener(listener);

        stream.listener.onComplete();

        Assert.assertEquals(1, listener.completeCount);
    }

    @Test
    public void testListenerReceivesError() {
        RecordingListener listener = new RecordingListener();
        MockRpcStream stream = new MockRpcStream();
        stream.setListener(listener);

        RuntimeException ex = new RuntimeException("stream error");
        stream.listener.onError(ex);

        Assert.assertSame(ex, listener.lastError);
    }

    @Test
    public void testWriteMessage() {
        MockRpcStream stream = new MockRpcStream();
        byte[] data = { 0x00, 0x00, 0x00, 0x00, 0x05, 0x0A, 0x03, 'f', 'o', 'o' };
        stream.writeMessage(data, false);

        Assert.assertEquals(1, stream.sentMessages.size());
        Assert.assertArrayEquals(data, stream.sentMessages.get(0));
    }

    @Test
    public void testWriteHeaders() {
        MockRpcStream stream = new MockRpcStream();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(":status", "200");
        headers.put("content-type", "application/grpc");
        stream.writeHeaders(headers, false);

        Assert.assertEquals(1, stream.sentHeaders.size());
        Assert.assertEquals("200", stream.sentHeaders.get(0).get(":status"));
    }

    @Test
    public void testHalfClose() {
        MockRpcStream stream = new MockRpcStream();
        Assert.assertFalse(stream.halfClosedCalled);
        stream.halfClose();
        Assert.assertTrue(stream.halfClosedCalled);
    }

    @Test
    public void testCancel() {
        MockRpcStream stream = new MockRpcStream();
        RuntimeException cause = new RuntimeException("cancelled");
        stream.cancel(cause);

        Assert.assertTrue(stream.cancelCalled);
        Assert.assertSame(cause, stream.cancelCause);
    }

    @Test
    public void testCancelWithNullCause() {
        MockRpcStream stream = new MockRpcStream();
        stream.cancel(null);
        Assert.assertTrue(stream.cancelCalled);
        Assert.assertNull(stream.cancelCause);
    }

    @Test
    public void testRequest() {
        MockRpcStream stream = new MockRpcStream();
        stream.request(5);
        stream.request(3);
        Assert.assertEquals(8, stream.requestCount);
    }

    @Test
    public void testIsWritable() {
        MockRpcStream stream = new MockRpcStream();
        Assert.assertTrue(stream.isWritable());
        stream.writableState = false;
        Assert.assertFalse(stream.isWritable());
    }

    @Test
    public void testWritabilityChangedCallback() {
        RecordingListener listener = new RecordingListener();
        MockRpcStream stream = new MockRpcStream();
        stream.setListener(listener);

        stream.listener.onWritabilityChanged();
        Assert.assertEquals(1, listener.writabilityChangedCount);
    }

    @Test
    public void testMultipleMessagesInOrder() {
        RecordingListener listener = new RecordingListener();
        MockRpcStream stream = new MockRpcStream();
        stream.setListener(listener);

        byte[] msg1 = { 0x01, 0x02 };
        byte[] msg2 = { 0x03, 0x04 };
        byte[] msg3 = { 0x05, 0x06 };

        stream.listener.onMessage(msg1);
        stream.listener.onMessage(msg2);
        stream.listener.onMessage(msg3);

        Assert.assertEquals(3, listener.receivedMessages.size());
        Assert.assertArrayEquals(msg1, listener.receivedMessages.get(0));
        Assert.assertArrayEquals(msg2, listener.receivedMessages.get(1));
        Assert.assertArrayEquals(msg3, listener.receivedMessages.get(2));
    }

    @Test
    public void testRpcStreamFactoryDefaultIsHttp2() {
        Assert.assertNotNull(RpcStreamFactory.HTTP2);
    }
}
