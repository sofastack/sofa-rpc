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

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.grpc.CallOptions;
import io.grpc.Channel;
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

    public SofaResponse invoke() {
        SofaResponse sofaResponse = new SofaResponse();
        try {
            Object response = invokeRequestMethod();
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

    public io.grpc.stub.AbstractStub getBlockingStub() {
        io.grpc.stub.AbstractStub stub = null;
        try {
            Method newBlockingStubMethod = Class.forName(serviceName)
                .getDeclaredMethod("newBlockingStub", Channel.class);
            newBlockingStubMethod.setAccessible(true);
            stub = (io.grpc.stub.AbstractStub) newBlockingStubMethod.invoke(null, channel);

        } catch (Throwable e) {
            throw new SofaRpcException(RpcErrorType.UNKNOWN, e.getMessage(), e);
        }
        return stub;
    }

    public Object invokeRequestMethod() {
        Object r = null;
        try {
            Method requestMethod = Class.forName(interfaceName)
                .getDeclaredMethod(method.getName(), Class.forName(methodArgSigs[0]));
            requestMethod.setAccessible(true);
            r = requestMethod.invoke(getBlockingStub(), methodArgs[0]);

        } catch (Throwable e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, e.getMessage(), e);
        }
        return r;
    }

}