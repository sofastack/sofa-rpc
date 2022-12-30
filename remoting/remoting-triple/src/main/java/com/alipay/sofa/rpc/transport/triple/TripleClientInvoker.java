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
import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientAsyncReceiveEvent;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.message.triple.TripleResponseFuture;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.google.protobuf.ByteString;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import triple.Request;
import triple.Response;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.alipay.sofa.rpc.common.RpcConstants.SERIALIZE_HESSIAN2;
import static com.alipay.sofa.rpc.constant.TripleConstant.UNIQUE_ID;
import static com.alipay.sofa.rpc.utils.SofaProtoUtils.checkIfUseGeneric;
import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * Invoker for Grpc
 *
 * @author LiangEn.LiWei; Yanqiang Oliver Luan (neokidd)
 * @date 2018.12.15 7:06 PM
 */
public class TripleClientInvoker implements TripleInvoker {
    private final static Logger LOGGER                = LoggerFactory.getLogger(TripleClientInvoker.class);

    private final static String DEFAULT_SERIALIZATION = SERIALIZE_HESSIAN2;

    protected Channel           channel;

    protected ConsumerConfig    consumerConfig;

    protected ProviderInfo providerInfo;

    protected Method            sofaStub;

    protected boolean           useGeneric;

    private Serializer          serializer;
    private String              serialization;

    private Map<String, Method> methodMap = new ConcurrentHashMap<>();

    public TripleClientInvoker(ConsumerConfig consumerConfig, ProviderInfo providerInfo, Channel channel) {
        this.channel = channel;
        this.consumerConfig = consumerConfig;
        this.providerInfo = providerInfo;

        useGeneric = checkIfUseGeneric(consumerConfig);
        cacheCommonData(consumerConfig);

        if (!useGeneric) {
            Class enclosingClass = consumerConfig.getProxyClass().getEnclosingClass();
            try {
                sofaStub = enclosingClass.getDeclaredMethod("getSofaStub", Channel.class, CallOptions.class, int.class);
            } catch (NoSuchMethodException e) {
                LOGGER.error("getSofaStub not found in enclosingClass" + enclosingClass.getName());
            }
        }
    }

    private void cacheCommonData(ConsumerConfig consumerConfig) {
        String serialization = consumerConfig.getSerialization();
        if (StringUtils.isBlank(serialization)) {
            serialization = getDefaultSerialization();
        }
        this.serialization = serialization;
        this.serializer = SerializerFactory.getSerializer(serialization);
    }

    protected String getDefaultSerialization() {
        return DEFAULT_SERIALIZATION;
    }

    @Override
    public SofaResponse invoke(SofaRequest sofaRequest, int timeout)
        throws Exception {
        if (!useGeneric) {
            SofaResponse sofaResponse = new SofaResponse();
            Object stub = sofaStub.invoke(null, channel, buildCustomCallOptions(sofaRequest, timeout),
                timeout);
            final Method method = sofaRequest.getMethod();
            Object appResponse = method.invoke(stub, sofaRequest.getMethodArgs()[0]);
            sofaResponse.setAppResponse(appResponse);
            return sofaResponse;
        } else {
            MethodDescriptor methodDescriptor = getMethodDescriptor(sofaRequest);
            Request request = getRequest(sofaRequest, serialization, serializer);
            Response response = (Response) ClientCalls.blockingUnaryCall(channel, methodDescriptor,
                buildCustomCallOptions(sofaRequest, timeout), request);

            SofaResponse sofaResponse = new SofaResponse();
            byte[] responseDate = response.getData().toByteArray();
            Class returnType = sofaRequest.getMethod().getReturnType();
            if (returnType != void.class) {
                if (responseDate != null && responseDate.length > 0) {
                    Serializer responseSerializer = SerializerFactory.getSerializer(response.getSerializeType());
                    Object appResponse = responseSerializer.decode(new ByteArrayWrapperByteBuf(responseDate), returnType, null);
                    sofaResponse.setAppResponse(appResponse);
                }
            }

            return sofaResponse;
        }

    }

    @Override
    public ResponseFuture asyncInvoke(SofaRequest sofaRequest, int timeout) throws Exception {
        SofaResponseCallback sofaResponseCallback = sofaRequest.getSofaResponseCallback();
        TripleResponseFuture future = new TripleResponseFuture(sofaRequest, timeout);

        RpcInternalContext context = RpcInternalContext.getContext();

        if (!useGeneric) {
            Method m = methodMap.get(sofaRequest.getMethodName());
            if (m == null) {
                synchronized (this) {
                    m = methodMap.get(sofaRequest.getMethodName());
                    if (m == null) {
                        Class<?> clazz = Class.forName(sofaRequest.getInterfaceName());
                        Method[] declaredMethods = clazz.getDeclaredMethods();
                        for (Method tempM : declaredMethods) {
                            if (StringUtils.equals(tempM.getName(), sofaRequest.getMethodName()) && tempM.getParameterCount() == 2
                                    && StringUtils.equals(tempM.getParameterTypes()[1].getCanonicalName(), StreamObserver.class.getCanonicalName())) {
                                m = tempM;
                                methodMap.put(sofaRequest.getMethodName(), m);
                                break;
                            }
                        }
                    }
                }
            }
            Object stub = sofaStub.invoke(null, channel, buildCustomCallOptions(sofaRequest, timeout),
                    null, consumerConfig, timeout);
            m.invoke(stub, sofaRequest.getMethodArgs()[0], new StreamObserver<Object>() {
                @Override
                public void onNext(Object o) {
                    processSuccess(false, context, sofaRequest, o, sofaResponseCallback, future);
                }

                @Override
                public void onError(Throwable throwable) {
                    processError(context, sofaRequest, throwable, sofaResponseCallback, future);
                }

                @Override
                public void onCompleted() {

                }
            });
        } else {
            MethodDescriptor methodDescriptor = getMethodDescriptor(sofaRequest);
            Request request = getRequest(sofaRequest, serialization, serializer);
            ClientCalls.asyncUnaryCall(channel.newCall(methodDescriptor, buildCustomCallOptions(sofaRequest, timeout)), request, new StreamObserver<Object>() {
                @Override
                public void onNext(Object o) {
                    processSuccess(true, context, sofaRequest, o, sofaResponseCallback, future);
                }

                @Override
                public void onError(Throwable throwable) {
                    processError(context, sofaRequest, throwable, sofaResponseCallback, future);
                }

                @Override
                public void onCompleted() {

                }
            });
        }
        return future;
    }

    private void processSuccess(boolean needDecode, RpcInternalContext context, SofaRequest sofaRequest, Object o, SofaResponseCallback sofaResponseCallback, TripleResponseFuture future) {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            RpcInternalContext.setContext(context);

            SofaResponse sofaResponse = new SofaResponse();
            sofaResponse.setAppResponse(o);
            if (EventBus.isEnable(ClientAsyncReceiveEvent.class)) {
                EventBus.post(new ClientAsyncReceiveEvent(consumerConfig, providerInfo,
                        sofaRequest, sofaResponse, null));
            }
            if (EventBus.isEnable(ClientEndInvokeEvent.class)) {
                EventBus.post(new ClientEndInvokeEvent(sofaRequest, sofaResponse, null));
            }

            Object appResponse = o;
            if (needDecode) {
                Response response = (Response) o;
                byte[] responseDate = response.getData().toByteArray();
                Class returnType = sofaRequest.getMethod().getReturnType();
                if (returnType != void.class) {
                    if (responseDate != null && responseDate.length > 0) {
                        Serializer responseSerializer = SerializerFactory.getSerializer(response.getSerializeType());
                        appResponse = responseSerializer.decode(new ByteArrayWrapperByteBuf(responseDate), returnType, null);
                    }
                }
            }

            if (sofaResponseCallback != null) {
                sofaResponseCallback.onAppResponse(appResponse, sofaRequest.getMethodName(), sofaRequest);
            } else {
                future.setSuccess(appResponse);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
        }
    }

    private void processError(RpcInternalContext context, SofaRequest sofaRequest, Throwable throwable, SofaResponseCallback sofaResponseCallback, TripleResponseFuture future) {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            RpcInternalContext.setContext(context);

            if (EventBus.isEnable(ClientAsyncReceiveEvent.class)) {
                EventBus.post(new ClientAsyncReceiveEvent(consumerConfig, providerInfo,
                        sofaRequest, null, throwable));
            }
            if (EventBus.isEnable(ClientEndInvokeEvent.class)) {
                EventBus.post(new ClientEndInvokeEvent(sofaRequest, null, throwable));
            }

            if (sofaResponseCallback != null) {
                Status status = Status.fromThrowable(throwable);
                if (status.getCode() == Status.Code.UNKNOWN) {
                    sofaResponseCallback.onAppException(throwable, sofaRequest.getMethodName(), sofaRequest);
                }else {
                    sofaResponseCallback.onSofaException(new SofaRpcException(RpcErrorType.UNKNOWN, status.getCause()), sofaRequest.getMethodName(), sofaRequest);
                }
            } else {
                future.setFailure(throwable);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
        }
    }

    private MethodDescriptor getMethodDescriptor(SofaRequest sofaRequest) {
        String serviceName = sofaRequest.getInterfaceName();
        String methodName = sofaRequest.getMethodName();
        MethodDescriptor.Marshaller<?> requestMarshaller = ProtoUtils.marshaller(Request.getDefaultInstance());
        MethodDescriptor.Marshaller<?> responseMarshaller = ProtoUtils.marshaller(Response.getDefaultInstance());
        String fullMethodName = generateFullMethodName(serviceName, methodName);
        MethodDescriptor methodDescriptor = MethodDescriptor
                .newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethodName)
                .setSampledToLocalTracing(true)
                .setRequestMarshaller((MethodDescriptor.Marshaller<Object>) requestMarshaller)
                .setResponseMarshaller((MethodDescriptor.Marshaller<Object>) responseMarshaller)
                .build();
        return methodDescriptor;
    }

    public static Request getRequest(SofaRequest sofaRequest, String serialization, Serializer serializer) {
        Request.Builder builder = Request.newBuilder();
        builder.setSerializeType(serialization);

        String[] methodArgSigs = sofaRequest.getMethodArgSigs();
        Object[] methodArgs = sofaRequest.getMethodArgs();

        for (int i = 0; i < methodArgSigs.length; i++) {
            Object arg = methodArgs[i];
            ByteString argByteString = ByteString.copyFrom(serializer.encode(arg, null).array());
            builder.addArgs(argByteString);
            builder.addArgTypes(methodArgSigs[i]);
        }
        return builder.build();
    }

    /**
     * set some custom info
     * @param sofaRequest
     * @param timeout
     * @return
     */
    protected CallOptions buildCustomCallOptions(SofaRequest sofaRequest, int timeout) {
        CallOptions tripleCallOptions = CallOptions.DEFAULT;
        final String target = consumerConfig.getParameter("interworking.target");
        if (StringUtils.isNotBlank(target)) {
            tripleCallOptions = tripleCallOptions.withAuthority(target);
        }
        if (timeout >= 0) {
            tripleCallOptions = tripleCallOptions.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS);
        }
        if (StringUtils.isNotBlank(consumerConfig.getUniqueId())) {
            tripleCallOptions = tripleCallOptions.withOption(UNIQUE_ID, consumerConfig.getUniqueId());
        }
        return tripleCallOptions;
    }
}