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

import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

/**
 *  Invoker for Grpc
 *
 * @author LiangEn.LiWei
 * @date 2018.12.15 7:06 PM
 */
public class GrpcClientInvoker {

    private final MethodDescriptor methodDescriptor;

    private final Channel          channel;

    private final Object           request;

    private final StreamObserver   responseObserver;

    private final Integer          timeout;

    /**
     * The constructor
     * @param sofaRequest The SofaRequest
     * @param methodDescriptor The MethodDescriptor
     * @param channel The Channel
     */
    public GrpcClientInvoker(SofaRequest sofaRequest, MethodDescriptor methodDescriptor, Channel channel) {
        this.methodDescriptor = methodDescriptor;
        this.channel = channel;
        Object[] methodArgs = sofaRequest.getMethodArgs();
        request = methodArgs[0];
        responseObserver = methodArgs.length == 2 ? (StreamObserver) methodArgs[1] : null;
        this.timeout = sofaRequest.getTimeout();
    }

    /**
     * Grpc invoke
     * @return Grpc response streamObserver
     */
    public StreamObserver invoke() {
        MethodType methodType = methodDescriptor.getType();
        ClientCall clientCall = channel.newCall(methodDescriptor, buildCallOptions());

        if (methodType == MethodType.UNARY) {
            ClientCalls.asyncUnaryCall(clientCall, request, responseObserver);
        } else if (methodType == MethodType.SERVER_STREAMING) {
            ClientCalls.asyncServerStreamingCall(clientCall, request, responseObserver);
        } else {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, "Unsupported Grpc invocation mode");
        }
        //stream calls are processed in the future
        return null;
    }

    private CallOptions buildCallOptions() {
        CallOptions callOptions = CallOptions.DEFAULT;
        if (timeout != null) {
            callOptions = callOptions.withDeadlineAfter(timeout, TimeUnit.SECONDS);
        }
        return callOptions;
    }
}