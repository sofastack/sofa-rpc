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
package com.alipay.sofa.rpc.server.rest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpUtil;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.jboss.resteasy.plugins.server.netty.NettyHttpResponse;
import org.jboss.resteasy.plugins.server.netty.NettyUtil;
import org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.ResteasyUriInfo;

import java.util.List;

/**
 * Code base on RestEasyHttpRequestDecoder, Code improved please search CHANGE
 * @see org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder
 */
@Sharable
public class SofaRestEasyHttpRequestDecoder extends MessageToMessageDecoder<io.netty.handler.codec.http.HttpRequest> {
    private final static Logger         logger = Logger
                                                   .getLogger(org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder.class);
    private final SynchronousDispatcher dispatcher;
    private final String                servletMappingPrefix;
    private final String                proto;

    public SofaRestEasyHttpRequestDecoder(SynchronousDispatcher dispatcher, String servletMappingPrefix,
                                          RestEasyHttpRequestDecoder.Protocol protocol) {
        this.dispatcher = dispatcher;
        this.servletMappingPrefix = servletMappingPrefix;
        if (protocol == RestEasyHttpRequestDecoder.Protocol.HTTP) {
            proto = "http";
        } else {
            proto = "https";
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, io.netty.handler.codec.http.HttpRequest request, List<Object> out)
        throws Exception {
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        final NettyHttpResponse response = new NettyHttpResponse(ctx, keepAlive, dispatcher.getProviderFactory());
        final ResteasyHttpHeaders headers;
        final ResteasyUriInfo uriInfo;
        try {
            headers = NettyUtil.extractHttpHeaders(request);

            uriInfo = NettyUtil.extractUriInfo(request, servletMappingPrefix, proto);
            NettyHttpRequest nettyRequest = new NettyHttpRequest(ctx, headers, uriInfo, request.method().name(),
                dispatcher, response, HttpUtil.is100ContinueExpected(request));
            if (request instanceof HttpContent) {
                HttpContent content = (HttpContent) request;

                // Does the request contain a body that will need to be retained
                if (content.content().readableBytes() > 0) {
                    ByteBuf buf = content.content().retain();
                    ByteBufInputStream in = new ByteBufInputStream(buf, true); // CHANGE: set releaseOnClose to true
                    nettyRequest.setInputStream(in);
                }

                out.add(nettyRequest);
            }
        } catch (Exception e) {
            response.sendError(400);
            // made it warn so that people can filter this.
            logger.warn("Failed to parse request.", e);
        }
    }
}
