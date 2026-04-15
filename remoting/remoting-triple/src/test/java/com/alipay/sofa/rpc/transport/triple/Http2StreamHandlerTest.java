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

import com.alipay.sofa.rpc.transport.triple.stream.Http2RpcStream;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStream;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStreamFactory;
import com.alipay.sofa.rpc.transport.triple.stream.RpcStreamListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link Http2StreamHandler}.
 *
 * <p>Uses Mockito to stub the Netty {@link Http2ConnectionDecoder}/{@link Http2ConnectionEncoder}
 * so that {@link io.netty.handler.codec.http2.Http2ConnectionHandler}'s constructor can be
 * satisfied without real Netty infrastructure. The actual routing logic under test
 * ({@code onHeadersRead}, {@code onDataRead}, {@code onRstStreamRead}, etc.) does not
 * use the decoder/encoder at all, so stub behaviour is sufficient.
 *
 * <p>The {@link RpcStreamFactory} is also stubbed to return a {@link FakeHttp2RpcStream}
 * so that write operations are no-ops.
 */
public class Http2StreamHandlerTest {

    // ── Fake Http2RpcStream (no Netty ctx/encoder) ────────────────────────────

    static class FakeHttp2RpcStream extends Http2RpcStream {
        final List<byte[]>              written    = new ArrayList<byte[]>();
        final List<Map<String, String>> headers    = new ArrayList<Map<String, String>>();
        boolean                         halfClosed = false;
        boolean                         cancelled  = false;

        FakeHttp2RpcStream(int streamId) {
            super(null, null, streamId);
        }

        @Override
        public void writeMessage(byte[] data, boolean compressed) {
            written.add(data.clone());
        }

        @Override
        public void writeHeaders(Map<String, String> h, boolean endStream) {
            headers.add(new HashMap<String, String>(h));
        }

        @Override
        public void halfClose() {
            halfClosed = true;
        }

        @Override
        public void cancel(Throwable cause) {
            cancelled = true;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    // ── Recording listener ────────────────────────────────────────────────────

    static class RecordingListener implements RpcStreamListener {
        final List<Map<String, String>> headers   = new ArrayList<Map<String, String>>();
        final List<byte[]>              messages  = new ArrayList<byte[]>();
        int                             completes = 0;
        Throwable                       error     = null;

        @Override
        public void onHeaders(Map<String, String> h, boolean endStream) {
            headers.add(new HashMap<String, String>(h));
        }

        @Override
        public void onMessage(byte[] data) {
            messages.add(data.clone());
        }

        @Override
        public void onComplete() {
            completes++;
        }

        @Override
        public void onError(Throwable cause) {
            error = cause;
        }

        @Override
        public void onWritabilityChanged() {
        }
    }

    // ── Handler under test ────────────────────────────────────────────────────

    /**
     * Testable subclass: overrides encoder() to avoid NPE from Http2ConnectionHandler.
     */
    static class TestableHttp2StreamHandler extends Http2StreamHandler {
        private final Http2ConnectionEncoder fakeEncoder;

        TestableHttp2StreamHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                   HttpServerTransportListenerFactory listenerFactory,
                                   RpcStreamFactory streamFactory) {
            super(decoder, encoder, new Http2Settings(), listenerFactory, streamFactory);
            this.fakeEncoder = encoder;
        }

        @Override
        public Http2ConnectionEncoder encoder() {
            return fakeEncoder;
        }
    }

    // ── Test state ────────────────────────────────────────────────────────────

    private Http2ConnectionDecoder            decoder;
    private Http2ConnectionEncoder            encoder;
    private ChannelHandlerContext             ctx;

    /** Streams created by the factory, indexed by streamId */
    private Map<Integer, FakeHttp2RpcStream>  createdStreams;
    /** Listeners created by the listenerFactory, indexed by stream */
    private Map<RpcStream, RecordingListener> createdListeners;

    private TestableHttp2StreamHandler        handler;

    @Before
    public void setUp() {
        decoder = Mockito.mock(Http2ConnectionDecoder.class);
        encoder = Mockito.mock(Http2ConnectionEncoder.class);
        ctx = Mockito.mock(ChannelHandlerContext.class);

        createdStreams = new HashMap<Integer, FakeHttp2RpcStream>();
        createdListeners = new HashMap<RpcStream, RecordingListener>();

        RpcStreamFactory streamFactory = new RpcStreamFactory() {
            @Override
            public RpcStream create(ChannelHandlerContext c, Http2ConnectionEncoder enc, int streamId) {
                FakeHttp2RpcStream s = new FakeHttp2RpcStream(streamId);
                createdStreams.put(streamId, s);
                return s;
            }
        };

        HttpServerTransportListenerFactory listenerFactory = new HttpServerTransportListenerFactory() {
            @Override
            public RpcStreamListener newListener(RpcStream stream) {
                RecordingListener l = new RecordingListener();
                createdListeners.put(stream, l);
                return l;
            }
        };

        handler = new TestableHttp2StreamHandler(decoder, encoder, listenerFactory, streamFactory);
    }

    // ── Constructor validation ────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void testNullListenerFactoryThrows() {
        new TestableHttp2StreamHandler(decoder, encoder, null, RpcStreamFactory.HTTP2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullStreamFactoryThrows() {
        new TestableHttp2StreamHandler(decoder, encoder,
            stream -> new RecordingListener(), null);
    }

    // ── onHeadersRead: new stream created and listener registered ────────────

    @Test
    public void testFirstHeadersCreateStreamAndRegisterListener() throws Http2Exception {
        Http2Headers h2Headers = new DefaultHttp2Headers().method("POST").path("/hello");
        handler.onHeadersRead(ctx, 1, h2Headers, 0, false);

        Assert.assertEquals(1, createdStreams.size());
        FakeHttp2RpcStream stream = createdStreams.get(1);
        Assert.assertNotNull(stream);
        Assert.assertNotNull(createdListeners.get(stream));
    }

    @Test
    public void testHeadersDispatchedToListener() throws Http2Exception {
        Http2Headers h2Headers = new DefaultHttp2Headers()
            .method("POST")
            .path("/com.example.HelloService/sayHello")
            .add("content-type", "application/grpc+proto");

        handler.onHeadersRead(ctx, 3, h2Headers, 0, false);

        FakeHttp2RpcStream stream = createdStreams.get(3);
        RecordingListener listener = createdListeners.get(stream);

        Assert.assertEquals(1, listener.headers.size());
        Assert.assertEquals("POST", listener.headers.get(0).get(":method"));
        Assert.assertEquals("application/grpc+proto", listener.headers.get(0).get("content-type"));
    }

    @Test
    public void testHeadersWithEndStream_completesStream() throws Http2Exception {
        Http2Headers h2Headers = new DefaultHttp2Headers().method("POST");
        handler.onHeadersRead(ctx, 5, h2Headers, 0, true); // endStream=true

        FakeHttp2RpcStream stream = createdStreams.get(5);
        RecordingListener listener = createdListeners.get(stream);

        Assert.assertEquals(1, listener.headers.size());
        Assert.assertEquals(1, listener.completes);
        // stream removed after completion
        Assert.assertEquals(0, handler.activeStreamCount());
    }

    @Test
    public void testSubsequentHeadersDoNotCreateNewStream() throws Http2Exception {
        Http2Headers h1 = new DefaultHttp2Headers().method("POST");
        Http2Headers h2 = new DefaultHttp2Headers().add("trailer", "grpc-status");
        handler.onHeadersRead(ctx, 7, h1, 0, false);
        handler.onHeadersRead(ctx, 7, h2, 0, false); // same stream id

        // only one stream created
        Assert.assertEquals(1, createdStreams.size());
        FakeHttp2RpcStream stream = createdStreams.get(7);
        RecordingListener listener = createdListeners.get(stream);
        Assert.assertEquals(2, listener.headers.size());
    }

    // ── Multiple concurrent streams ───────────────────────────────────────────

    @Test
    public void testMultipleConcurrentStreams() throws Http2Exception {
        Http2Headers h = new DefaultHttp2Headers().method("POST");
        handler.onHeadersRead(ctx, 1, h, 0, false);
        handler.onHeadersRead(ctx, 3, h, 0, false);
        handler.onHeadersRead(ctx, 5, h, 0, false);

        Assert.assertEquals(3, handler.activeStreamCount());
        Assert.assertEquals(3, createdStreams.size());
    }

    // ── onDataRead ────────────────────────────────────────────────────────────

    @Test
    public void testDataDispatchedToListener() throws Http2Exception {
        // first establish stream
        handler.onHeadersRead(ctx, 1, new DefaultHttp2Headers().method("POST"), 0, false);

        byte[] data = { 0x00, 0x00, 0x00, 0x00, 0x03, 0x01, 0x02, 0x03 };
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        int processed = handler.onDataRead(ctx, 1, buf, 0, false);

        FakeHttp2RpcStream stream = createdStreams.get(1);
        RecordingListener listener = createdListeners.get(stream);

        Assert.assertEquals(1, listener.messages.size());
        Assert.assertArrayEquals(data, listener.messages.get(0));
        Assert.assertEquals(data.length, processed);
    }

    @Test
    public void testDataWithEndStream_completesStream() throws Http2Exception {
        handler.onHeadersRead(ctx, 1, new DefaultHttp2Headers().method("POST"), 0, false);

        ByteBuf buf = Unpooled.wrappedBuffer(new byte[] { 0x01, 0x02 });
        handler.onDataRead(ctx, 1, buf, 0, true); // endStream=true

        FakeHttp2RpcStream stream = createdStreams.get(1);
        RecordingListener listener = createdListeners.get(stream);

        Assert.assertEquals(1, listener.messages.size());
        Assert.assertEquals(1, listener.completes);
        Assert.assertEquals(0, handler.activeStreamCount());
    }

    @Test
    public void testDataForUnknownStream_doesNotThrow() throws Http2Exception {
        // data arrives for a stream id we never saw headers for — should not throw
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[] { 0x01 });
        int processed = handler.onDataRead(ctx, 999, buf, 0, false);
        Assert.assertEquals(1 /* data */+ 0 /* padding */, processed);
    }

    @Test
    public void testDataReturnedProcessedBytesIncludesPadding() throws Http2Exception {
        handler.onHeadersRead(ctx, 1, new DefaultHttp2Headers().method("POST"), 0, false);

        ByteBuf buf = Unpooled.wrappedBuffer(new byte[] { 0x01, 0x02, 0x03 });
        int processed = handler.onDataRead(ctx, 1, buf, 4 /* padding */, false);

        Assert.assertEquals(3 + 4, processed); // data + padding
    }

    // ── onRstStreamRead ───────────────────────────────────────────────────────

    @Test
    public void testRstStreamTriggersOnError() throws Http2Exception {
        handler.onHeadersRead(ctx, 1, new DefaultHttp2Headers().method("POST"), 0, false);

        FakeHttp2RpcStream stream = createdStreams.get(1);
        RecordingListener listener = createdListeners.get(stream);

        handler.onRstStreamRead(ctx, 1, 8L); // error code 8 = CANCEL

        Assert.assertNotNull(listener.error);
        Assert.assertTrue(listener.error instanceof Http2Exception);
        // stream removed
        Assert.assertEquals(0, handler.activeStreamCount());
    }

    @Test
    public void testRstStreamForUnknownStream_doesNotThrow() {
        handler.onRstStreamRead(ctx, 999, 0L);
    }

    // ── exceptionCaught ───────────────────────────────────────────────────────

    @Test
    public void testExceptionCaughtPropagatesErrorToAllActiveStreams() throws Http2Exception {
        Http2Headers h = new DefaultHttp2Headers().method("POST");
        handler.onHeadersRead(ctx, 1, h, 0, false);
        handler.onHeadersRead(ctx, 3, h, 0, false);

        RuntimeException ex = new RuntimeException("channel error");
        handler.exceptionCaught(ctx, ex);

        for (RpcStream s : createdListeners.keySet()) {
            RecordingListener l = createdListeners.get(s);
            Assert.assertSame(ex, l.error);
        }
        // all streams cleared
        Assert.assertEquals(0, handler.activeStreamCount());
    }

    // ── activeStreamCount ─────────────────────────────────────────────────────

    @Test
    public void testActiveStreamCountStartsAtZero() {
        Assert.assertEquals(0, handler.activeStreamCount());
    }

    @Test
    public void testActiveStreamCountDecreasesAfterCompletion() throws Http2Exception {
        Http2Headers h = new DefaultHttp2Headers().method("POST");
        handler.onHeadersRead(ctx, 1, h, 0, false);
        handler.onHeadersRead(ctx, 3, h, 0, false);
        Assert.assertEquals(2, handler.activeStreamCount());

        // complete stream 1
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[0]);
        handler.onDataRead(ctx, 1, buf, 0, true);
        Assert.assertEquals(1, handler.activeStreamCount());

        // RST stream 3
        handler.onRstStreamRead(ctx, 3, 0L);
        Assert.assertEquals(0, handler.activeStreamCount());
    }

    // ── No-op frame callbacks (smoke test: must not throw) ────────────────────

    @Test
    public void testNoOpCallbacksDoNotThrow() throws Http2Exception {
        handler.onPriorityRead(ctx, 1, 0, (short) 0, false);
        handler.onSettingsAckRead(ctx);
        handler.onSettingsRead(ctx, new Http2Settings());
        handler.onPingRead(ctx, 0L);
        handler.onPingAckRead(ctx, 0L);
        handler.onGoAwayRead(ctx, 0, 0L, Unpooled.EMPTY_BUFFER);
        handler.onWindowUpdateRead(ctx, 0, 100);
        handler.onUnknownFrame(ctx, (byte) 0xFF, 0, null, Unpooled.EMPTY_BUFFER);
    }
}
