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

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.common.utils.ClassLoaderUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientAfterSendEvent;
import com.alipay.sofa.rpc.event.ClientBeforeSendEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.message.http.HttpResponseFuture;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.alipay.sofa.rpc.transport.AbstractChannel;
import com.alipay.sofa.rpc.transport.ClientHandler;
import com.alipay.sofa.rpc.transport.ClientTransport;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import com.alipay.sofa.rpc.transport.netty.NettyChannel;
import com.alipay.sofa.rpc.transport.netty.NettyHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.AsciiString;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * h2和h2c通用的客户端传输层
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public abstract class AbstractHttp2ClientTransport extends ClientTransport {

    /**
     * Logger for AbstractHttpClientTransport
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttp2ClientTransport.class);

    /**
     * 客户端配置
     *
     * @param transportConfig 客户端配置
     */
    public AbstractHttp2ClientTransport(ClientTransportConfig transportConfig) {
        super(transportConfig);
        this.providerInfo = transportConfig.getProviderInfo();
    }

    /**
     * 服务端提供者信息
     */
    protected final ProviderInfo        providerInfo;
    /**
     * Start from 3 (because 1 is setting stream)
     */
    private final static int            START_STREAM_ID = 3;
    /**
     * StreamId, start from 3 (because 1 is setting stream)
     */
    protected final AtomicInteger       streamId        = new AtomicInteger();
    /**
     * 正在发送的调用数量
     */
    protected volatile AtomicInteger    currentRequests = new AtomicInteger(0);

    /**
     * Channel
     */
    protected NettyChannel              channel;

    /**
     * Response channel handler
     */
    protected Http2ClientChannelHandler responseChannelHandler;

    /**
     * 超时处理器
     */
    private static final Timer          TIMEOUT_TIMER   = new HashedWheelTimer(new NamedThreadFactory("HTTP-TIMER"),
                                                            10, TimeUnit.MILLISECONDS);

    @Override
    public void connect() {
        if (isAvailable()) {
            return;
        }
        EventLoopGroup workerGroup = NettyHelper.getClientIOEventLoopGroup();
        Http2ClientInitializer initializer = new Http2ClientInitializer(transportConfig);
        try {
            String host = providerInfo.getHost();
            int port = providerInfo.getPort();
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(transportConfig.isUseEpoll() ? EpollSocketChannel.class : NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.remoteAddress(host, port);
            b.handler(initializer);

            // Start the client.
            Channel channel = b.connect().syncUninterruptibly().channel();
            this.channel = new NettyChannel(channel);

            // Wait for the HTTP/2 upgrade to occur.
            Http2SettingsHandler http2SettingsHandler = initializer.settingsHandler();
            http2SettingsHandler.awaitSettings(transportConfig.getConnectTimeout(), TimeUnit.MILLISECONDS);

            responseChannelHandler = initializer.responseHandler();
            // RESET streamId
            streamId.set(START_STREAM_ID);
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_NETWORK, e);
        }
    }

    @Override
    public void disconnect() {
        if (channel != null) {
            Channel nettyChannel = channel.channel();
            if (nettyChannel != null) {
                nettyChannel.close().syncUninterruptibly();
            }
        }
    }

    @Override
    public void destroy() {
        NettyHelper.closeClientIOEventGroup();
    }

    @Override
    public boolean isAvailable() {
        return channel != null && channel.isAvailable();
    }

    @Override
    public void setChannel(AbstractChannel channel) {
        this.channel = (NettyChannel) channel;
    }

    @Override
    public AbstractChannel getChannel() {
        return channel;
    }

    @Override
    public int currentRequests() {
        return currentRequests.get();
    }

    @Override
    public ResponseFuture asyncSend(SofaRequest request, int timeout) throws SofaRpcException {
        checkConnection();
        RpcInternalContext context = RpcInternalContext.getContext();
        try {
            beforeSend(context, request);
            return doInvokeAsync(request, context, timeout);
        } catch (SofaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, e.getMessage(), e);
        } finally {
            afterSend(context, request);
        }
    }

    /**
     * 异步调用
     *
     * @param request       请求对象
     * @param rpcContext    RPC内置上下文
     * @param timeoutMillis 超时时间（毫秒）
     */
    protected ResponseFuture doInvokeAsync(SofaRequest request, RpcInternalContext rpcContext, int timeoutMillis) {
        SofaResponseCallback listener = request.getSofaResponseCallback();
        if (listener != null) {
            AbstractHttpClientHandler callback = new CallbackInvokeClientHandler(transportConfig.getConsumerConfig(),
                transportConfig.getProviderInfo(), listener, request, rpcContext,
                ClassLoaderUtils.getCurrentClassLoader());
            doSend(request, callback, timeoutMillis);
            return null;
        } else {
            HttpResponseFuture future = new HttpResponseFuture(request, timeoutMillis);
            AbstractHttpClientHandler callback = new FutureInvokeClientHandler(transportConfig.getConsumerConfig(),
                transportConfig.getProviderInfo(), future, request, rpcContext,
                ClassLoaderUtils.getCurrentClassLoader());
            doSend(request, callback, timeoutMillis);
            future.setSentTime();
            return future;
        }
    }

    @Override
    public SofaResponse syncSend(SofaRequest request, int timeout) throws SofaRpcException {
        checkConnection();
        RpcInternalContext context = RpcInternalContext.getContext();
        try {
            beforeSend(context, request);
            return doInvokeSync(request, timeout);
        } catch (TimeoutException e) {
            throw timeoutException(request, timeout, e);
        } catch (SofaRpcException e) {
            throw e;
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, e.getMessage(), e);
        } finally {
            afterSend(context, request);
        }
    }

    /**
     * 同步调用
     *
     * @param request 请求对象
     * @param timeout 超时时间（毫秒）
     * @return 返回对象
     * @throws InterruptedException 中断异常
     * @throws ExecutionException   执行异常
     * @throws TimeoutException     超时异常
     */
    protected SofaResponse doInvokeSync(SofaRequest request, int timeout) throws InterruptedException,
        ExecutionException, TimeoutException {
        HttpResponseFuture future = new HttpResponseFuture(request, timeout);
        AbstractHttpClientHandler callback = new SyncInvokeClientHandler(transportConfig.getConsumerConfig(),
            transportConfig.getProviderInfo(), future, request, RpcInternalContext.getContext(),
            ClassLoaderUtils.getCurrentClassLoader());
        future.setSentTime();
        doSend(request, callback, timeout);
        future.setSentTime();
        return future.getSofaResponse(timeout, TimeUnit.MILLISECONDS);
    }

    protected void doSend(final SofaRequest request, AbstractHttpClientHandler callback, final int timeoutMills) {
        AbstractByteBuf data = null;
        try {
            // 序列化
            byte serializeType = request.getSerializeType();
            Serializer serializer = SerializerFactory.getSerializer(serializeType);
            data = serializer.encode(request, null);
            request.setData(data);
            // 记录请求序列化大小 不是很准，没有记录HTTP头
            RpcInternalContext.getContext().setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, data.readableBytes());

            // 转换请求
            FullHttpRequest httpRequest = convertToHttpRequest(request);

            // 发送请求
            final int requestId = sendHttpRequest(httpRequest, callback);

            if (request.isAsync()) {
                TIMEOUT_TIMER.newTimeout(new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        Map.Entry<ChannelFuture, AbstractHttpClientHandler> entry = responseChannelHandler
                            .removePromise(requestId);
                        if (entry != null) {
                            ClientHandler handler = entry.getValue();
                            Exception e = timeoutException(request, timeoutMills, null);
                            handler.onException(e);
                        }
                    }

                }, timeoutMills, TimeUnit.MILLISECONDS);
            }
        } finally {
            if (data != null) {
                data.release();
            }
        }
    }

    protected FullHttpRequest convertToHttpRequest(SofaRequest request) {
        HttpScheme scheme = SslContextBuilder.SSL ? HttpScheme.HTTPS : HttpScheme.HTTP;
        AsciiString hostName = new AsciiString(providerInfo.getHost() + ':' + providerInfo.getPort());
        String url = "/" + request.getTargetServiceUniqueName() + "/" + request.getMethodName();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("send request to url :{}", url);
        }

        // Create a simple POST request with a body.
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, url,
            wrappedBuffer(request.getData().array()));
        HttpHeaders headers = httpRequest.headers();
        addToHeader(headers, HttpHeaderNames.HOST, hostName);
        addToHeader(headers, HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), scheme.name());
        addToHeader(headers, HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        addToHeader(headers, HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
        addToHeader(headers, RemotingConstants.HEAD_SERIALIZE_TYPE,
            SerializerFactory.getAliasByCode(request.getSerializeType()));
        addToHeader(headers, RemotingConstants.HEAD_TARGET_APP, request.getTargetAppName());
        Map<String, Object> requestProps = request.getRequestProps();
        if (requestProps != null) {
            // <String, Object> 转扁平化 <String, String>
            flatCopyTo("", requestProps, headers);
        }
        return httpRequest;
    }

    protected int sendHttpRequest(FullHttpRequest httpRequest, AbstractHttpClientHandler callback) {
        final int requestId = streamId.getAndAdd(2);
        Channel channel = this.channel.channel();
        responseChannelHandler.put(requestId, channel.write(httpRequest), callback);
        channel.flush();
        return requestId;
    }

    @Override
    public void oneWaySend(SofaRequest request, int timeout) throws SofaRpcException {
        throw new UnsupportedOperationException();
    }

    /**
     * 调用前设置一些属性
     *
     * @param context RPC上下文
     * @param request 请求对象
     */
    protected void beforeSend(RpcInternalContext context, SofaRequest request) {
        currentRequests.incrementAndGet();
        context.getStopWatch().tick().read();
        context.setLocalAddress(localAddress());
        if (EventBus.isEnable(ClientBeforeSendEvent.class)) {
            EventBus.post(new ClientBeforeSendEvent(request));
        }
    }

    /**
     * 调用后设置一些属性（注意，在异步的情况较多下）
     *
     * @param context RPC上下文
     * @param request 请求对象
     */
    protected void afterSend(RpcInternalContext context, SofaRequest request) {
        currentRequests.decrementAndGet();
        int cost = context.getStopWatch().tick().read();
        context.setAttachment(RpcConstants.INTERNAL_KEY_REQ_SERIALIZE_TIME, cost);
        if (EventBus.isEnable(ClientAfterSendEvent.class)) {
            EventBus.post(new ClientAfterSendEvent(request));
        }
    }

    @Override
    public void receiveRpcResponse(SofaResponse response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleRpcRequest(SofaRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return channel == null ? null : channel.remoteAddress();
    }

    @Override
    public InetSocketAddress localAddress() {
        return channel == null ? null : channel.localAddress();
    }

    protected void checkConnection() throws SofaRpcException {
        if (!isAvailable()) {
            throw new SofaRpcException(RpcErrorType.CLIENT_NETWORK, "channel is not available");
        }
    }

    protected SofaTimeOutException timeoutException(SofaRequest request, int timeout, Throwable e) {
        return new SofaTimeOutException(LogCodes.getLog(LogCodes.ERROR_INVOKE_TIMEOUT,
            providerInfo.getProtocolType(), request.getTargetServiceUniqueName(), request.getMethodName(),
            providerInfo.toString(), StringUtils.objectsToString(request.getMethodArgs()), timeout), e);
    }

    /**
     * 扁平化复制
     *
     * @param prefix    前缀
     * @param sourceMap 原始map
     * @param headers   目标map
     */
    protected void flatCopyTo(String prefix, Map<String, Object> sourceMap, HttpHeaders headers) {
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = prefix + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                addToHeader(headers, key, (CharSequence) value);
            } else if (value instanceof Number) {
                addToHeader(headers, key, value.toString());
            } else if (value instanceof Map) {
                flatCopyTo(key + ".", (Map<String, Object>) value, headers);
            }
        }
    }

    private void addToHeader(HttpHeaders headers, CharSequence key, CharSequence value) {
        if (StringUtils.isNotEmpty(value)) {
            headers.add(key, value);
        }
    }
}
