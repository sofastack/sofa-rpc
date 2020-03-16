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
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Invoker for Grpc
 *
 * @author LiangEn.LiWei; Yanqiang Oliver Luan (neokidd)
 * @date 2018.12.15 7:06 PM
 */
public class GrpcClientInvoker {

    private final Channel        channel;

    private final Object         request;
    private final StreamObserver responseObserver;
    private final Class          requestClass;

    private final Method         method;
    private final String[]       methodArgSigs;
    private final Object[]       methodArgs;

    private final String         serviceName;
    private final String         interfaceName;

    private final Integer        timeout;

    private final static Logger  LOGGER = LoggerFactory.getLogger(GrpcClientInvoker.class);

    /**
     * The constructor
     *
     * @param sofaRequest The SofaRequest
     * @param channel     The Channel
     */
    public GrpcClientInvoker(SofaRequest sofaRequest, Channel channel) {
        this.channel = channel;
        this.method = sofaRequest.getMethod();
        this.methodArgs = sofaRequest.getMethodArgs();
        this.methodArgSigs = sofaRequest.getMethodArgSigs();
        this.interfaceName = sofaRequest.getInterfaceName();
        this.serviceName = interfaceName.substring(0, interfaceName.indexOf('$'));
        this.request = methodArgs[0];
        this.responseObserver = methodArgs.length == 2 ? (StreamObserver) methodArgs[1] : null;
        this.requestClass = ClassUtils.forName(methodArgSigs[0]);

        try {
            requestClass.cast(request);
        } catch (ClassCastException e) {
            throw e;
        }
        this.timeout = sofaRequest.getTimeout();
    }

    public SofaResponse invoke(ConsumerConfig consumerConfig) {
        SofaResponse sofaResponse = new SofaResponse();
        try {
            Object response = invokeRequestMethod(consumerConfig);
            sofaResponse.setAppResponse(response);
        } catch (SofaRpcException e) {
            sofaResponse.setErrorMsg(e.getMessage());
        }
        return sofaResponse;
    }

    private CallOptions buildCallOptions() {
        CallOptions callOptions = CallOptions.DEFAULT;
        if (timeout != null) {
            callOptions = callOptions.withDeadlineAfter(timeout, TimeUnit.SECONDS);
        }
        return callOptions;
    }

    public Object invokeRequestMethod(ConsumerConfig consumerConfig) {
        Object r = null;
        try {

            Class enclosingClass = consumerConfig.getProxyClass().getEnclosingClass();

            Method sofaStub = enclosingClass.getDeclaredMethod("getSofaStub", Channel.class, CallOptions.class,
                ProviderInfo.class, ConsumerConfig.class, int.class);
            Object stub = sofaStub.invoke(null, channel, CallOptions.DEFAULT, null, null, 3000);

            r = method.invoke(stub, methodArgs[0]);

        } catch (Throwable e) {
            Status status = Status.fromThrowable(e);
            StatusException grpcException = status.asException();
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, grpcException);
        }
        return r;
    }

}