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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class Http1ServerTask extends AbstractHttpServerTask {

    protected final boolean keepAlive;

    public Http1ServerTask(HttpServerHandler serverHandler, SofaRequest request,
                           ChannelHandlerContext ctx, boolean keepAlive) {
        super(serverHandler, request, ctx);
        this.keepAlive = keepAlive;
    }

    @Override
    protected void sendAppResponse(HttpResponseStatus status, ByteBuf data) {
        sendHttp1Response0(status, false, data);
    }

    @Override
    protected void sendAppError(HttpResponseStatus status, ByteBuf data) {
        sendHttp1Response0(status, true, data);
    }

    @Override
    protected void sendRpcError(HttpResponseStatus status, ByteBuf data) {
        sendHttp1Response0(status, true, data);
    }

    protected void sendHttp1Response0(HttpResponseStatus status, boolean error, ByteBuf content) {
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, status, content);
        HttpHeaders headers = httpResponse.headers();

        headers.setInt(CONTENT_LENGTH, httpResponse.content().readableBytes());
        if (request.getSerializeType() > 0) {
            String serialization = SerializerFactory.getAliasByCode(request.getSerializeType());
            headers.set(RemotingConstants.HEAD_SERIALIZE_TYPE, serialization);
        } else {
            headers.set(CONTENT_TYPE, "text/plain; charset=" + RpcConstants.DEFAULT_CHARSET.displayName());
        }
        if (error) {
            headers.set(RemotingConstants.HEAD_RESPONSE_ERROR, "true");
        }
        if (!keepAlive) {
            ctx.write(httpResponse).addListener(ChannelFutureListener.CLOSE);
        } else {
            httpResponse.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(httpResponse);
        }
    }
}
