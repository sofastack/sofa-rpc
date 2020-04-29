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

import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.netty.NettyHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.internal.PlatformDependent;

import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Process {@link FullHttpResponse} translated from HTTP/2 frames
 * 
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class Http2ClientChannelHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    /**
     * Logger for HttpClientChannelHandler
     **/
    private static final Logger                                                 LOGGER = LoggerFactory
                                                                                           .getLogger(Http2ClientChannelHandler.class);

    /**
     * 
     */
    private final Map<Integer, Entry<ChannelFuture, AbstractHttpClientHandler>> streamIdPromiseMap;

    public Http2ClientChannelHandler() {
        // Use a concurrent map because we add and iterate from the main thread (just for the purposes of the example),
        // but Netty also does a get on the map when messages are received in a EventLoop thread.
        streamIdPromiseMap = PlatformDependent.newConcurrentHashMap();
    }

    /**
     * Create an association between an anticipated response stream id and a {@link ChannelPromise}
     *
     * @param streamId    The stream for which a response is expected
     * @param writeFuture A future that represent the request write operation
     * @param promise     The promise object that will be used to wait/notify events
     * @return The previous object associated with {@code streamId}
     */
    public Entry<ChannelFuture, AbstractHttpClientHandler> put(int streamId, ChannelFuture writeFuture,
                                                               AbstractHttpClientHandler promise) {
        return streamIdPromiseMap.put(streamId, new SimpleEntry<ChannelFuture, AbstractHttpClientHandler>(
            writeFuture, promise));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        HttpHeaders headers = msg.headers();
        Integer streamId = headers.getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
        if (streamId == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("HttpResponseHandler unexpected message received: {}, data is {}", msg.toString(),
                    NettyHelper.toString(msg.content()));
            }
            return;
        }

        Entry<ChannelFuture, AbstractHttpClientHandler> entry = removePromise(streamId);
        if (entry == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Message received for unknown stream id {}, msg is {}, data is {}", streamId,
                    msg.toString(), NettyHelper.toString(msg.content()));
            }
        } else {
            final AbstractHttpClientHandler callback = entry.getValue();
            callback.receiveHttpResponse(msg);
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Channel inactive: {}", channel);
        }
        final Exception e = new SofaRpcException(RpcErrorType.CLIENT_NETWORK, "Channel "
            + NetUtils.channelToString(channel.localAddress(), channel.remoteAddress())
            + " has been closed, remove future when channel inactive.");
        Iterator<Entry<Integer, Entry<ChannelFuture, AbstractHttpClientHandler>>> it =
                streamIdPromiseMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Entry<ChannelFuture, AbstractHttpClientHandler>> mapEntry = it.next();
            it.remove();
            Entry<ChannelFuture, AbstractHttpClientHandler> entry = mapEntry.getValue();
            entry.getValue().onException(e);
        }
    }

    public Entry<ChannelFuture, AbstractHttpClientHandler> removePromise(int streamId) {
        return streamIdPromiseMap.remove(streamId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(LogCodes.getLog(LogCodes.ERROR_CATCH_EXCEPTION), cause);
    }
}
