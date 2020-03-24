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

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientBeforeSendEvent;
import com.alipay.sofa.rpc.event.ClientSyncReceiveEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.interceptor.ClientHeaderClientInterceptor;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.server.triple.TripleContants;
import com.alipay.sofa.rpc.transport.AbstractChannel;
import com.alipay.sofa.rpc.transport.ClientTransport;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * GRPC client transport
 *
 * @author LiangEn.LiWei; Yanqiang Oliver Luan (neokidd)
 * @date 2018.11.09 12:10 PM
 */
@Extension("tri")
public class TripleClientTransport extends ClientTransport {

    private final static Logger LOGGER = LoggerFactory.getLogger(TripleClientTransport.class);

    private ProviderInfo providerInfo;

    private ManagedChannel channel;

    private InetSocketAddress localAddress;

    private InetSocketAddress remoteAddress;

    /* <address, gRPC channels> */
    private final static ConcurrentMap<String, ReferenceCountManagedChannel> channelMap = new ConcurrentHashMap<>();

    private final Object lock = new Object();

    /**
     * The constructor
     *
     * @param transportConfig transport config
     */
    public TripleClientTransport(ClientTransportConfig transportConfig) {
        super(transportConfig);
        providerInfo = transportConfig.getProviderInfo();
        connect();
        remoteAddress = InetSocketAddress.createUnresolved(providerInfo.getHost(), providerInfo.getPort());
        localAddress = InetSocketAddress.createUnresolved(NetUtils.getLocalIpv4(), 0);// 端口不准
    }

    @Override
    public void connect() {
        if (isAvailable()) {
            return;
        }
        ProviderInfo providerInfo = transportConfig.getProviderInfo();
        channel = getSharedChannel(providerInfo);
    }

    @Override
    public void disconnect() {
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("GRPC channel shut down interrupted.");
            }
            channel = null;
        }
        channelMap.remove(providerInfo.toString());
    }

    @Override
    public void destroy() {
        disconnect();
    }

    @Override
    public boolean isAvailable() {
        if (channel == null) {
            return false;
        }

        return !channel.isShutdown() && !channel.isTerminated();
    }

    @Override
    public void setChannel(AbstractChannel channel) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public AbstractChannel getChannel() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int currentRequests() {
        return 0;
    }

    @Override
    public ResponseFuture asyncSend(SofaRequest message, int timeout) throws SofaRpcException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public SofaResponse syncSend(SofaRequest request, int timeout) throws SofaRpcException {
        SofaResponse sofaResponse = null;
        SofaRpcException throwable = null;

        try {
            RpcInternalContext context = RpcInternalContext.getContext();

            beforeSend(context, request);

            RpcInvokeContext invokeContext = RpcInvokeContext.getContext();
            invokeContext.put(TripleContants.SOFA_REQUEST_KEY, request);
            invokeContext.put(TripleContants.SOFA_CONSUMER_CONFIG_KEY, transportConfig.getConsumerConfig());
            final TripleClientInvoker tripleClientInvoker = new TripleClientInvoker(request, channel);
            sofaResponse = tripleClientInvoker.invoke(transportConfig.getConsumerConfig(), timeout);
            return sofaResponse;
        } catch (Exception e) {
            throwable = convertToRpcException(e);
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, "Grpc invoke error", e);
        } finally {
            if (EventBus.isEnable(ClientSyncReceiveEvent.class)) {
                EventBus.post(new ClientSyncReceiveEvent(transportConfig.getConsumerConfig(),
                        transportConfig.getProviderInfo(), request, sofaResponse, throwable));
            }
        }
    }

    @Override
    public void oneWaySend(SofaRequest message, int timeout) throws SofaRpcException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void receiveRpcResponse(SofaResponse response) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void handleRpcRequest(SofaRequest request) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public InetSocketAddress localAddress() {
        return localAddress;
    }

    /**
     * Get shared channel connection
     */
    private ReferenceCountManagedChannel getSharedChannel(ProviderInfo url) {
        String key = url.toString();
        ReferenceCountManagedChannel channel = channelMap.get(key);

        if (channel != null && !channel.isTerminated()) {
            channel.incrementAndGetCount();
            return channel;
        }

        synchronized (lock) {
            channel = channelMap.get(key);
            // double check
            if (channel != null && !channel.isTerminated()) {
                channel.incrementAndGetCount();
            } else {
                channel = new ReferenceCountManagedChannel(initChannel(url));
                channelMap.put(key, channel);
            }
        }

        return channel;
    }

    /**
     * Create new connection
     *
     * @param url
     */
    private ManagedChannel initChannel(ProviderInfo url) {
        ClientHeaderClientInterceptor clientHeaderClientInterceptor = new ClientHeaderClientInterceptor();
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(url.getHost(), url.getPort());
        builder.usePlaintext();
        builder.disableRetry();
        builder.intercept(clientHeaderClientInterceptor);
        return builder.build();
    }

    /**
     * 调用前设置一些属性
     *
     * @param context RPC上下文
     * @param request 请求对象
     */
    protected void beforeSend(RpcInternalContext context, SofaRequest request) {
        context.setLocalAddress(localAddress());
        if (EventBus.isEnable(ClientBeforeSendEvent.class)) {
            EventBus.post(new ClientBeforeSendEvent(request));
        }
    }

    /**
     * 转换调用出现的异常为RPC异常
     *
     * @param e 异常
     * @return RPC异常
     */
    protected SofaRpcException convertToRpcException(Exception e) {
        SofaRpcException exception;
        if (e instanceof SofaRpcException) {
            exception = (SofaRpcException) e;
        }
        // 客户端未知
        else {
            exception = new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, e);
        }
        return exception;
    }
}