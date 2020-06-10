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
package com.alipay.sofa.rpc.transport.http;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.http.HttpServerHandler;
import com.alipay.sofa.rpc.transport.netty.NettyByteBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2Stream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public final class Http2ServerChannelHandler extends Http2ConnectionHandler implements Http2FrameListener {

    /**
     * Logger for Http2ChannelHandler
     **/
    private static final Logger               LOGGER           = LoggerFactory
                                                                   .getLogger(Http2ServerChannelHandler.class);

    private final Http2Connection.PropertyKey headerKey        = encoder().connection().newKey();
    private final Http2Connection.PropertyKey messageKey       = encoder().connection().newKey();

    private final HttpServerHandler           serverHandler;

    private boolean                           isUpgradeH2cMode = false;

    Http2ServerChannelHandler(HttpServerHandler serverHandler, Http2ConnectionDecoder decoder,
                              Http2ConnectionEncoder encoder,
                              Http2Settings initialSettings) {
        super(decoder, encoder, initialSettings);
        this.serverHandler = serverHandler;
    }

    private static Http2Headers http1HeadersToHttp2Headers(FullHttpRequest request) {
        CharSequence host = request.headers().get(HttpHeaderNames.HOST);
        Http2Headers http2Headers = new DefaultHttp2Headers()
            .method(HttpMethod.GET.asciiName())
            .path(request.uri())
            .scheme(HttpScheme.HTTP.name());
        if (host != null) {
            http2Headers.authority(host);
        }
        return http2Headers;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        /*
         * Handles the cleartext HTTP upgrade event. If an upgrade occurred, sends a simple response via HTTP/2
         * on stream 1 (the stream specifically reserved for cleartext HTTP upgrade).
         */
        if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent) {
            HttpServerUpgradeHandler.UpgradeEvent upgradeEvent =
                    (HttpServerUpgradeHandler.UpgradeEvent) evt;
            this.isUpgradeH2cMode = true;
            onHeadersRead(ctx, 1, http1HeadersToHttp2Headers(upgradeEvent.upgradeRequest()), 0, true);
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("", cause);
        }
        ctx.close();
    }

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) {
        int processed = data.readableBytes() + padding;

        Http2Stream http2Stream = connection().stream(streamId);
        ByteBuf msg = http2Stream.getProperty(messageKey);
        if (msg == null) {
            msg = ctx.alloc().buffer();
            http2Stream.setProperty(messageKey, msg);
        }
        final int dataReadableBytes = data.readableBytes();
        msg.writeBytes(data, data.readerIndex(), dataReadableBytes);

        if (endOfStream) {
            // read cached http2 header from stream
            Http2Headers headers = http2Stream.getProperty(headerKey);
            handleRequest(ctx, streamId, headers, msg);
        }
        return processed;
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                              Http2Headers headers, int padding, boolean endOfStream) {
        /**
         * https://http2.github.io/http2-spec/#rfc.section.5.1.1 second paragraph
         * only when in upgrade h2c mode, 0x01 cannot be selected as a new stream identifier.
         * some gateway or proxy product, use 0x01 as first normal request's stream id  when
         * in prior knowleadge mode.
         */
        if (this.isUpgradeH2cMode && streamId > 1 || !this.isUpgradeH2cMode && streamId > 0) {
            // 正常的请求（streamId==1 的是settings请求）
            if (endOfStream) {
                // 没有DATA帧的请求，可能是DATA
                handleRequest(ctx, streamId, headers, null);
            } else {
                // 缓存起来
                connection().stream(streamId).setProperty(headerKey, headers);
            }
        }
    }

    protected void handleRequest(ChannelHandlerContext ctx, int streamId, Http2Headers http2Headers, ByteBuf data) {
        String uri = StringUtils.defaultString(http2Headers.path());
        // ignore uris
        if (RemotingConstants.IGNORE_WEB_BROWSER.equals(uri)) {
            sendHttp2Response(ctx, streamId, HttpResponseStatus.OK, StringUtils.EMPTY);
            return;
        }

        CharSequence reqMethod = StringUtils.defaultString(http2Headers.method());
        // HEAD for check method exists
        if (reqMethod.equals(HttpMethod.HEAD.name())) {
            String[] iam = HttpTransportUtils.getInterfaceIdAndMethod(uri);
            boolean exists = serverHandler.checkService(iam[0], iam[1]);
            sendHttp2Response(ctx, streamId, exists ? HttpResponseStatus.OK : HttpResponseStatus.NOT_FOUND, null);
            return;
        }
        // POST(primary) / GET for invoke
        else if (!reqMethod.equals(HttpMethod.POST.name())) {
            sendHttp2Response(ctx, streamId, HttpResponseStatus.BAD_REQUEST, "Only support POST/HEAD");
            return;
        }

        /**
         * https://http2.github.io/http2-spec/#rfc.section.5.1.1 second paragraph
         * only when in upgrade h2c mode, 0x01 cannot be selected as a new stream identifier.
         * some gateway or proxy product, use 0x01 as first normal request's stream id  when
         * in prior knowleadge mode.
         */
        if (this.isUpgradeH2cMode && streamId > 1 || !this.isUpgradeH2cMode && streamId > 0) {
            // 本来这里可以提前检查接口方法是否存在，但是为了日志统一，全部放到serverHandler里去
            SofaRequest sofaRequest = new SofaRequest();
            try {
                String[] iam = HttpTransportUtils.getInterfaceIdAndMethod(uri);
                sofaRequest.setTargetServiceUniqueName(iam[0]);
                sofaRequest.setMethodName(iam[1]);
                sofaRequest.setData(new NettyByteBuffer(data));
                parseHttp2Request(http2Headers, sofaRequest);
            } catch (Exception e) {
                String message = "Failed to parse http2 request for uri " + uri + " form "
                    + NetUtils.channelToString(ctx.channel().remoteAddress(), ctx.channel().localAddress())
                    + ", cause by: " + e.getMessage();
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(message, e);
                }
                sendHttp2Response(ctx, streamId, HttpResponseStatus.BAD_REQUEST, message);
                return;
            }
            try {
                serverHandler.handleHttp2Request(streamId, sofaRequest, ctx, encoder());
            } catch (SofaRpcException e) {
                int type = e.getErrorType();
                if (type == RpcErrorType.SERVER_BUSY) {
                    sendHttp2Response(ctx, streamId, HttpResponseStatus.SERVICE_UNAVAILABLE, e.getMessage());
                } else if (type == RpcErrorType.SERVER_NOT_FOUND_INVOKER) {
                    sendHttp2Response(ctx, streamId, HttpResponseStatus.NOT_FOUND, e.getMessage());
                } else {
                    sendHttp2Response(ctx, streamId, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                }
            } catch (Exception e) {
                sendHttp2Response(ctx, streamId, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }

    private void parseHttp2Request(Http2Headers headers, SofaRequest sofaRequest) {
        String targetApp = StringUtils.toString(headers.get(RemotingConstants.HEAD_TARGET_APP));
        sofaRequest.setTargetAppName(targetApp);
        // 获取序列化类型
        byte serializeType;
        CharSequence codeName = headers.get(RemotingConstants.HEAD_SERIALIZE_TYPE);
        if (codeName != null) {
            serializeType = HttpTransportUtils.getSerializeTypeByName(codeName.toString());
        } else {
            String contentType = StringUtils.toString(headers.get(HttpHeaderNames.CONTENT_TYPE));
            serializeType = HttpTransportUtils.getSerializeTypeByContentType(contentType);
        }
        sofaRequest.setSerializeType(serializeType);

        // 解析trace信息
        Map<String, String> traceMap = new HashMap<String, String>(8);
        Iterator<Map.Entry<CharSequence, CharSequence>> it = headers.iterator();
        while (it.hasNext()) {
            Map.Entry<CharSequence, CharSequence> entry = it.next();
            String key = entry.getKey().toString();
            if (HttpTracerUtils.isTracerKey(key)) {
                HttpTracerUtils.parseTraceKey(traceMap, key, StringUtils.toString(entry.getValue()));
            } else if (!key.startsWith(":")) {
                sofaRequest.addRequestProp(key, StringUtils.toString(entry.getValue()));
            }
        }
        if (!traceMap.isEmpty()) {
            sofaRequest.addRequestProp(RemotingConstants.RPC_TRACE_NAME, traceMap);
        }
    }

    protected void sendHttp2Response(ChannelHandlerContext ctx, int streamId, HttpResponseStatus status, String result) {
        // Send a frame for the response status
        Http2Headers headers = new DefaultHttp2Headers().status(status.codeAsText());
        if (!HttpResponseStatus.OK.equals(status)) {
            headers.set(RemotingConstants.HEAD_RESPONSE_ERROR, "true");
        }
        if (StringUtils.isNotBlank(result)) {
            ByteBuf data = ctx.alloc().buffer();
            data.writeBytes(result.getBytes(RpcConstants.DEFAULT_CHARSET));
            encoder().writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
            encoder().writeData(ctx, streamId, data, 0, true, ctx.newPromise());
        } else {
            encoder().writeHeaders(ctx, streamId, headers, 0, true, ctx.newPromise());
        }
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
                              short weight, boolean exclusive, int padding, boolean endOfStream) {
        onHeadersRead(ctx, streamId, headers, padding, endOfStream);
    }

    @Override
    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency,
                               short weight, boolean exclusive) {
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
    }

    @Override
    public void onSettingsAckRead(ChannelHandlerContext ctx) {
    }

    @Override
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
    }

    @Override
    public void onPingRead(ChannelHandlerContext ctx, long data) {
    }

    @Override
    public void onPingAckRead(ChannelHandlerContext ctx, long data) {
    }

    @Override
    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
                                  Http2Headers headers, int padding) {
    }

    @Override
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {
    }

    @Override
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {
    }

    @Override
    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId,
                               Http2Flags flags, ByteBuf payload) {
    }
}
