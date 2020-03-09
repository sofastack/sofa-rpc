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
package com.alipay.sofa.rpc.server.http;

import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class Http2ServerTask extends AbstractHttpServerTask {

    protected final Integer                streamId;
    protected final Http2ConnectionEncoder encoder;

    public Http2ServerTask(HttpServerHandler serverHandler, SofaRequest request,
                           ChannelHandlerContext ctx, Integer streamId, Http2ConnectionEncoder encoder) {
        super(serverHandler, request, ctx);
        this.streamId = streamId;
        this.encoder = encoder;
    }

    @Override
    protected void sendAppResponse(HttpResponseStatus status, ByteBuf data) {
        sendHttp2Response0(status, false, data);
    }

    @Override
    protected void sendAppError(HttpResponseStatus status, ByteBuf data) {
        sendHttp2Response0(status, true, data);
    }

    @Override
    protected void sendRpcError(HttpResponseStatus status, ByteBuf data) {
        sendHttp2Response0(status, true, data);
    }

    private void sendHttp2Response0(HttpResponseStatus status, boolean error, ByteBuf data) {
        Http2Headers headers = new DefaultHttp2Headers().status(status.codeAsText());

        if (request.getSerializeType() > 0) {
            String serialization = SerializerFactory.getAliasByCode(request.getSerializeType());
            headers.set(RemotingConstants.HEAD_SERIALIZE_TYPE, serialization);
        } else {
            headers.set(CONTENT_TYPE, "text/plain; charset=" + RpcConstants.DEFAULT_CHARSET.displayName());
        }
        if (error) {
            headers.set(RemotingConstants.HEAD_RESPONSE_ERROR, "true");
        }
        if (data != null) {
            encoder.writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
            encoder.writeData(ctx, streamId, data, 0, true, ctx.newPromise());
        } else {
            encoder.writeHeaders(ctx, streamId, headers, 0, true, ctx.newPromise());
        }
    }
}
