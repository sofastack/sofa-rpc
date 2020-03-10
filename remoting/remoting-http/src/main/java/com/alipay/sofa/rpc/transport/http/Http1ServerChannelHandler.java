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
import com.alipay.sofa.rpc.common.annotation.Unstable;
import com.alipay.sofa.rpc.common.cache.ReflectCache;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.http.HttpServerHandler;
import com.alipay.sofa.rpc.transport.netty.NettyByteBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Server channel handler for HTTP/1.1, support GET/POST/HEAD
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
@Unstable
public class Http1ServerChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * Logger for Http1ChannelHandler
     **/
    private static final Logger     LOGGER = LoggerFactory.getLogger(Http1ServerChannelHandler.class);

    private final HttpServerHandler serverHandler;

    public Http1ServerChannelHandler(HttpServerHandler serverHandler) {
        this.serverHandler = checkNotNull(serverHandler, "serverHandler");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

        if (HttpUtil.is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
        }
        boolean keepAlive = HttpUtil.isKeepAlive(req);

        String uri = req.uri();
        // ignore uris
        if (RemotingConstants.IGNORE_WEB_BROWSER.equals(uri)) {
            sendHttp1Response(ctx, HttpResponseStatus.OK, StringUtils.EMPTY, keepAlive);
            return;
        }

        HttpMethod reqMethod = req.method();
        // HEAD for check method exists
        if (reqMethod == HttpMethod.HEAD) {
            String[] iam = HttpTransportUtils.getInterfaceIdAndMethod(uri);
            boolean exists = serverHandler.checkService(iam[0], iam[1]);
            sendHttp1Response(ctx, exists ? HttpResponseStatus.OK : HttpResponseStatus.NOT_FOUND, "", keepAlive);
            return;
        }
        // POST(primary) / GET for invoke
        if (reqMethod != HttpMethod.POST && reqMethod != HttpMethod.GET) {
            sendHttp1Response(ctx, HttpResponseStatus.BAD_REQUEST, "Only support GET/POST/HEAD", keepAlive);
            return;
        }

        // call service
        SofaRequest sofaRequest = new SofaRequest();
        try {
            String[] iam = HttpTransportUtils.getInterfaceIdAndMethod(uri);
            String serviceName = iam[0];
            String methodName = iam[1];
            sofaRequest.setTargetServiceUniqueName(serviceName);
            sofaRequest.setMethodName(methodName);
            parseHeader(req, sofaRequest);

            if (reqMethod == HttpMethod.GET) {
                Method method = ReflectCache.getMethodCache(serviceName, methodName);
                if (method == null) {
                    sendHttp1Response(ctx, HttpResponseStatus.NOT_FOUND,
                        "Not found method:" + serviceName + "." + methodName, keepAlive);
                    return;
                }
                String params = null;
                Class[] classArray = method.getParameterTypes();
                int length = classArray.length;
                Object[] paramList = new Object[length];
                int i = uri.indexOf('?');
                if (i >= 0) {
                    params = uri.substring(i + 1);
                    paramList = this.parseParamArg(classArray, params);
                } else {
                    if (length != 0) {
                        throw new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, "The number of parameter is wrong.");
                    }
                }
                sofaRequest.setMethodArgSigs(ReflectCache.getMethodSigsCache(serviceName, methodName));
                sofaRequest.setMethodArgs(paramList);
            } else {
                sofaRequest.setData(new NettyByteBuffer(req.content()));
            }
        } catch (Exception e) {
            String message = "Failed to parse http2 request for uri " + uri + " form "
                + NetUtils.channelToString(ctx.channel().remoteAddress(), ctx.channel().localAddress())
                + ", cause by: " + e.getMessage();
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(message, e);
            }
            sendHttp1Response(ctx, HttpResponseStatus.BAD_REQUEST, message, keepAlive);
            return;
        }
        try {
            serverHandler.handleHttp1Request(sofaRequest, ctx, keepAlive);
        } catch (SofaRpcException e) {
            int type = e.getErrorType();
            if (type == RpcErrorType.SERVER_BUSY) {
                sendHttp1Response(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, e.getMessage(), keepAlive);
            } else if (type == RpcErrorType.SERVER_NOT_FOUND_INVOKER) {
                sendHttp1Response(ctx, HttpResponseStatus.NOT_FOUND, e.getMessage(), keepAlive);
            } else {
                sendHttp1Response(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage(), keepAlive);
            }
        } catch (Exception e) {
            sendHttp1Response(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage(), keepAlive);
        }
    }

    public Object[] parseParamArg(Class[] classTypes, String params) {
        String[] paramList = params.split("&");
        if (classTypes.length != paramList.length) {
            throw new SofaRpcException(RpcErrorType.SERVER_DESERIALIZE, "The number of parameter is wrong.");
        }
        Object[] resultList = new Object[paramList.length];
        for (int i = 0; i < classTypes.length; i++) {
            Class cl = classTypes[i];
            String value = paramList[i].substring(paramList[i].indexOf('=') + 1).trim();
            if (String.class.equals(cl)) {
                try {
                    resultList[i] = URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    resultList[i] = value;
                }
            } else if (boolean.class.equals(cl) || Boolean.class.equals(cl)) {
                resultList[i] = Boolean.parseBoolean(value);
            } else if (byte.class.equals(cl) || Byte.class.equals(cl)) {
                resultList[i] = Byte.decode(value);
            } else if (short.class.equals(cl) || Short.class.equals(cl)) {
                resultList[i] = Short.decode(value);
            } else if (char.class.equals(cl) || Character.class.equals(cl)) {
                resultList[i] = Character.valueOf(value.charAt(0));
            } else if (int.class.equals(cl) || Integer.class.equals(cl)) {
                resultList[i] = Integer.decode(value);
            } else if (long.class.equals(cl) || Long.class.equals(cl)) {
                resultList[i] = Long.decode(value);
            } else if (float.class.equals(cl) || Float.class.equals(cl)) {
                resultList[i] = Float.valueOf(value);
            } else if (double.class.equals(cl) || Double.class.equals(cl)) {
                resultList[i] = Double.valueOf(value);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return resultList;
    }

    private void parseHeader(FullHttpRequest httpRequest, SofaRequest sofaRequest) {
        HttpHeaders headers = httpRequest.headers();

        // 获取序列化类型
        byte serializeType;
        if (httpRequest.method() == HttpMethod.GET) {
            serializeType = 0;
        } else {
            String codeName = headers.get(RemotingConstants.HEAD_SERIALIZE_TYPE);
            if (codeName != null) {
                serializeType = HttpTransportUtils.getSerializeTypeByName(codeName);
            } else {
                String contentType = headers.get(HttpHeaderNames.CONTENT_TYPE);
                serializeType = HttpTransportUtils.getSerializeTypeByContentType(contentType);
            }
        }
        sofaRequest.setSerializeType(serializeType);
        // 服务端应用
        sofaRequest.setTargetAppName(headers.get(RemotingConstants.HEAD_TARGET_APP));
    }

    protected int sendHttp1Response(ChannelHandlerContext ctx, HttpResponseStatus status, String resultStr,
                                    boolean isKeepAlive) {
        ByteBuf content = Unpooled.copiedBuffer(resultStr, RpcConstants.DEFAULT_CHARSET);
        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status, content);
        res.headers().set(CONTENT_TYPE, "text/html; charset=" + RpcConstants.DEFAULT_CHARSET.displayName());
        HttpUtil.setContentLength(res, content.readableBytes());
        try {
            ChannelFuture f = ctx.channel().writeAndFlush(res);
            if (isKeepAlive) {
                HttpUtil.setKeepAlive(res, true);
            } else {
                HttpUtil.setKeepAlive(res, false); //set keepalive closed
                f.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Exception e2) {
            LOGGER.warn("Failed to send HTTP response to remote, cause by:", e2);
        }

        return content.readableBytes();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error(LogCodes.getLog(LogCodes.ERROR_CATCH_EXCEPTION), cause);
        ctx.close();
    }
}
