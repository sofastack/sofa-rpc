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

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerEndHandleEvent;
import com.alipay.sofa.rpc.event.rest.RestServerReceiveEvent;
import com.alipay.sofa.rpc.event.rest.RestServerSendEvent;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.lookout.RestLookoutAdapter;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.jboss.resteasy.plugins.server.netty.NettyHttpResponse;
import org.jboss.resteasy.plugins.server.netty.RequestDispatcher;
import org.jboss.resteasy.spi.Failure;
import sun.rmi.runtime.Log;

import javax.ws.rs.core.HttpHeaders;
import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 参考resteasy实现，增加了取远程地址的方法，包括vip或者nginx转发的情况
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @see org.jboss.resteasy.plugins.server.netty.RequestHandler
 */
public class SofaRestRequestHandler extends SimpleChannelInboundHandler {
    protected final RequestDispatcher dispatcher;
    private final static Logger       logger = LoggerFactory.getLogger(SofaRestRequestHandler.class);

    public SofaRestRequestHandler(RequestDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof NettyHttpRequest) {

            NettyHttpRequest request = (NettyHttpRequest) msg;

            try {
                if (EventBus.isEnable(RestServerReceiveEvent.class)) {
                    EventBus.post(new RestServerReceiveEvent(request));
                }

                if (request.getUri().getPath().endsWith(RemotingConstants.IGNORE_WEB_BROWSER)) {
                    HttpResponse response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
                    ctx.writeAndFlush(response);
                    return;
                }

                if (request.is100ContinueExpected()) {
                    send100Continue(ctx);
                }

                NettyHttpResponse response = request.getResponse();
                Exception exception = null;
                try {
                    RpcInternalContext context = RpcInternalContext.getContext();
                    context.setProviderSide(true);
                    // 获取远程ip 兼容 nignx 转发和 vip 等
                    HttpHeaders httpHeaders = request.getHttpHeaders();
                    String remoteIP = httpHeaders.getHeaderString("X-Forwarded-For");
                    if (remoteIP == null) {
                        remoteIP = httpHeaders.getHeaderString("X-Real-IP");
                    }
                    if (remoteIP != null) {
                        context.setRemoteAddress(remoteIP, 0);
                    } else { // request取不到就从channel里取
                        context.setRemoteAddress((InetSocketAddress) ctx.channel().remoteAddress());
                    }
                    // 设置本地地址
                    context.setLocalAddress((InetSocketAddress) ctx.channel().localAddress());

                    dispatcher.service(ctx, request, response, true);
                } catch (Failure e1) {
                    response.reset();
                    response.setStatus(e1.getErrorCode());
                    exception = e1;
                } catch (Exception ex) {
                    response.reset();
                    response.setStatus(500);
                    logger.error(LogCodes.getLog(LogCodes.ERROR_PROCESS_UNKNOWN), ex); // todo 异常带给用户?
                    exception = ex;
                } finally {
                    if (EventBus.isEnable(RestServerSendEvent.class)) {
                        EventBus.post(new RestServerSendEvent(request, response, exception));
                    }

                    RestLookoutAdapter.sendRestServerSendEvent(new RestServerSendEvent(request, response, exception));
                }

                if (!request.getAsyncContext().isSuspended()) {
                    response.finish();
                }
            } finally {
                /**
                 * issue: https://github.com/sofastack/sofa-rpc/issues/592
                 */
                request.releaseContentBuffer();
                if (EventBus.isEnable(ServerEndHandleEvent.class)) {
                    EventBus.post(new ServerEndHandleEvent());
                }
                RpcInvokeContext.removeContext();
                RpcInternalContext.removeAllContext();
            }
        }
    }

    private void send100Continue(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)
        throws Exception {
        // handle the case of to big requests.
        if (e.getCause() instanceof TooLongFrameException) {
            DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, REQUEST_ENTITY_TOO_LARGE);
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            if (ctx.channel().isActive()) { // 连接已断开就不打印了
                logger.warn("Exception caught by request handler", e);
            }
            ctx.close();
        }
    }
}
