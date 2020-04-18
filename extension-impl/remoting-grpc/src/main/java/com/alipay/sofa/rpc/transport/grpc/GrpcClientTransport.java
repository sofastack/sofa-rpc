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
package com.alipay.sofa.rpc.transport.grpc;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.transport.AbstractChannel;
import com.alipay.sofa.rpc.transport.ClientTransport;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * GRPC client transport
 *
 * @author LiangEn.LiWei; Yanqiang Oliver Luan (neokidd)
 * @date 2018.11.09 12:10 PM
 */
@Extension("grpc")
public class GrpcClientTransport extends ClientTransport {

    private ProviderInfo        providerInfo;

    private ManagedChannel      channel;

    private InetSocketAddress   localAddress;

    private InetSocketAddress   remoteAddress;

    private final static Logger LOGGER = LoggerFactory.getLogger(GrpcClientTransport.class);

    /**
     * The constructor
     *
     * @param transportConfig transport config
     */
    public GrpcClientTransport(ClientTransportConfig transportConfig) {
        super(transportConfig);
        providerInfo = transportConfig.getProviderInfo();
        connect();
        remoteAddress = InetSocketAddress.createUnresolved(providerInfo.getHost(), providerInfo.getPort());
        localAddress = InetSocketAddress.createUnresolved(SystemInfo.getLocalHost(), 0);// 端口不准
    }

    @Override
    public void connect() {
        if (isAvailable()) {
            return;
        }
        ProviderInfo providerInfo = transportConfig.getProviderInfo();
        channel = ManagedChannelBuilder.forAddress(providerInfo.getHost(), providerInfo.getPort()).usePlaintext()
            .build();
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

        ConnectivityState state = channel.getState(true);
        return state == ConnectivityState.IDLE || state == ConnectivityState.READY ||
            state == ConnectivityState.CONNECTING;
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
        try {
            SofaResponse r = new GrpcClientInvoker(request, channel).invoke();
            return r;
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, "Grpc invoke error", e);
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
}