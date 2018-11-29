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
import com.alipay.sofa.rpc.common.cache.ReflectCache;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.transport.AbstractChannel;
import com.alipay.sofa.rpc.transport.AbstractProxyClientTransport;
import com.alipay.sofa.rpc.transport.ClientTransport;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.09 12:10 PM
 */
@Extension("grpc")
public class GrpcClientTransport extends ClientTransport {

    private ProviderInfo providerInfo;

    private ManagedChannel channel;

    private Object grpcStub;

    /**
     * 构造函数
     *
     * @param transportConfig 客户端配置
     */
    public GrpcClientTransport(ClientTransportConfig transportConfig) {
        super(transportConfig);
        this.providerInfo = transportConfig.getProviderInfo();
    }

    @Override
    public void connect() {
        if (isAvailable()) {
            return;
        }

        ProviderInfo providerInfo = transportConfig.getProviderInfo();
        String serviceName = transportConfig.getConsumerConfig().getInterfaceId();
        String host = providerInfo.getHost();
        int port = providerInfo.getPort();

        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        try {
            grpcStub = GrpcUtil.getStub(serviceName, host, port);
        } catch (SofaRpcException e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_NETWORK, e);
        }
    }

    @Override
    public void disconnect() {
        ManagedChannel channel = GrpcUtil.getChannel(providerInfo.getHost(), providerInfo.getPort());
        if (channel != null) {
            channel.shutdown();
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isAvailable() {
        if (grpcStub == null || channel == null) {
            return false;
        }

        ConnectivityState state = channel.getState(true);
        return state == ConnectivityState.IDLE || state == ConnectivityState.READY || state == ConnectivityState.CONNECTING;
    }

    @Override
    public void setChannel(AbstractChannel channel) {

    }

    @Override
    public AbstractChannel getChannel() {
        return null;
    }

    @Override
    public int currentRequests() {
        return 0;
    }

    @Override
    public ResponseFuture asyncSend(SofaRequest message, int timeout) throws SofaRpcException {
        return null;
    }

    @Override
    public SofaResponse syncSend(SofaRequest request, int timeout) throws SofaRpcException {

        String serviceName = request.getInterfaceName();
        String methodName = request.getMethodName();
        String[] methodSigns = request.getMethodArgSigs();

        Method method = ReflectCache.getOverloadMethodCache(serviceName, methodName, methodSigns);
        if (method == null) {
            try {
                method = grpcStub.getClass().getMethod(methodName, ClassTypeUtils.getClasses(methodSigns));
                ReflectCache.putOverloadMethodCache(serviceName, method);
            } catch (NoSuchMethodException e) {
                throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, "Method not found", e);
            }
        }

        try {

            method.invoke(grpcStub, request.getMethodArgs());

        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, "Method not found", e);
        } finally {
        }

        return new SofaResponse();
    }

    @Override
    public void oneWaySend(SofaRequest message, int timeout) throws SofaRpcException {

    }

    @Override
    public void receiveRpcResponse(SofaResponse response) {

    }

    @Override
    public void handleRpcRequest(SofaRequest request) {

    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    public InetSocketAddress localAddress() {
        return null;
    }

    //@Override
    //protected Object buildProxy(ClientTransportConfig transportConfig) throws SofaRpcException {
    //    if (grpcStub != null) {
    //        return grpcStub;
    //    }
    //
    //    ProviderInfo providerInfo = transportConfig.getProviderInfo();
    //    String serviceName = transportConfig.getConsumerConfig().getInterfaceId();
    //    String host = providerInfo.getHost();
    //    int port = providerInfo.getPort();
    //
    //    try {
    //        grpcStub = GrpcUtil.getStub(serviceName, host, port);
    //    } catch (SofaRpcException e) {
    //        throw e;
    //    }
    //
    //    return grpcStub;
    //}

    //@Override
    //protected Method getMethod(SofaRequest request) throws SofaRpcException {
    //    String serviceName = request.getInterfaceName();
    //    String methodName = request.getMethodName();
    //    String[] methodSigns = request.getMethodArgSigs();
    //
    //    Method method = ReflectCache.getOverloadMethodCache(serviceName, methodName, methodSigns);
    //    if (method == null) {
    //        try {
    //            method = grpcStub.getClass().getMethod(methodName, ClassTypeUtils.getClasses(methodSigns));
    //            ReflectCache.putOverloadMethodCache(serviceName, method);
    //        } catch (NoSuchMethodException e) {
    //            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, "Method not found", e);
    //        }
    //    }
    //
    //    return method;
    //}
}