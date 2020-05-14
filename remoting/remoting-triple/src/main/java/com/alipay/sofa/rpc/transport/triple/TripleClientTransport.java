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
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
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
import io.grpc.ClientInterceptor;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusException;
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

    protected ProviderInfo providerInfo;

    protected ManagedChannel channel;

    protected InetSocketAddress localAddress;

    protected InetSocketAddress remoteAddress;

    protected TripleInvoker tripleClientInvoker;

    /* <address, gRPC channels> */
    protected final static ConcurrentMap<String, ReferenceCountManagedChannel> channelMap = new ConcurrentHashMap<>();

    protected final Object lock = new Object();

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
        tripleClientInvoker = buildClientInvoker();
    }

    protected TripleClientInvoker buildClientInvoker() {
        return new TripleClientInvoker(transportConfig.getConsumerConfig(), channel);
    }

    @Override
    public void disconnect() {
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("Triple channel shut down interrupted.");
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
        return channelAvailable(channel);
    }

    protected boolean channelAvailable(ManagedChannel channel) {
        if (channel == null) {
            return false;
        }
        ConnectivityState state = channel.getState(false);
        if (ConnectivityState.READY == state) {
            return true;
        }
        if (ConnectivityState.SHUTDOWN == state || ConnectivityState.TRANSIENT_FAILURE == state) {
            return false;
        }
        if (ConnectivityState.IDLE == state || ConnectivityState.CONNECTING == state) {
            return true;
        }
        return false;
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
            sofaResponse = tripleClientInvoker.invoke(request, timeout);
            return sofaResponse;
        } catch (Exception e) {
            throwable = convertToRpcException(e);
            throw throwable;
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

        if (channelAvailable(channel)) {
            channel.incrementAndGetCount();
            return channel;
        } else if (channel != null) {
            channel.shutdownNow();
        }

        synchronized (lock) {
            channel = channelMap.get(key);
            // double check
            if (channelAvailable(channel)) {
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
        ClientInterceptor clientHeaderClientInterceptor = buildClientHeaderClientInterceptor();
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(url.getHost(), url.getPort());
        builder.usePlaintext();
        builder.disableRetry();
        builder.intercept(clientHeaderClientInterceptor);
        return builder.build();
    }

    protected ClientInterceptor buildClientHeaderClientInterceptor() {
        return new ClientHeaderClientInterceptor();
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
            return exception;
        }
        Status status = Status.fromThrowable(e);
        StatusException grpcException = status.asException();

        if (status.getCode() == Status.DEADLINE_EXCEEDED.getCode()) {
            exception = new SofaTimeOutException(grpcException);
        } else if (status.getCode() == Status.NOT_FOUND.getCode()) {
            exception = new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, grpcException);
        } else if (status.getCode() == Status.UNAVAILABLE.getCode()) {
            exception = new SofaRpcException(RpcErrorType.CLIENT_NETWORK, grpcException);
        } else if (status.getCode() == Status.RESOURCE_EXHAUSTED.getCode()) {
            exception = new SofaRpcException(RpcErrorType.SERVER_BUSY, grpcException);
        } else {
            exception = new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, grpcException);
        }
        return exception;
    }
}