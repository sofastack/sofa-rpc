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
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.ClassLoaderUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.BaggageResolver;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientAsyncReceiveEvent;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.filter.FilterChain;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.MessageBuilder;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.message.triple.stream.ClientStreamObserverAdapter;
import com.alipay.sofa.rpc.message.triple.TripleResponseFuture;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.alipay.sofa.rpc.transport.StreamHandler;
import com.alipay.sofa.rpc.utils.SofaProtoUtils;
import com.alipay.sofa.rpc.utils.TripleExceptionUtils;
import com.google.protobuf.ByteString;
import io.grpc.*;
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
    private final static Logger LOGGER = LoggerFactory.getLogger(TripleClientInvoker.class);

    private final static String DEFAULT_SERIALIZATION = SERIALIZE_HESSIAN2;

    protected Channel channel;

    protected ConsumerConfig consumerConfig;

    protected ProviderInfo providerInfo;

    protected Method sofaStub;

    protected boolean useGeneric;

    private Serializer serializer;
    private String serialization;

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

    public static Request getRequest(SofaRequest sofaRequest, String serialization, Serializer serializer, int trueParamStart) {
        Request.Builder builder = Request.newBuilder();
        builder.setSerializeType(serialization);

        String[] methodArgSigs = sofaRequest.getMethodArgSigs();
        Object[] methodArgs = sofaRequest.getMethodArgs();

        for (int i = trueParamStart; i < methodArgSigs.length; i++) {
            Object arg = methodArgs[i];
            ByteString argByteString = ByteString.copyFrom(serializer.encode(arg, null).array());
            builder.addArgs(argByteString);
            builder.addArgTypes(methodArgSigs[i]);
        }
        return builder.build();
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

        MethodDescriptor.MethodType callType = mapCallType(sofaRequest);

        if(!useGeneric){
            return stubCall(sofaRequest,timeout);
        } else if (callType.equals(MethodDescriptor.MethodType.UNARY)) {
            return unaryCall(sofaRequest, timeout);
        } else {
            return streamCall(sofaRequest, timeout, callType);
        }
    }

    private MethodDescriptor.MethodType mapCallType(SofaRequest sofaRequest) {
        String sofaCallType = sofaRequest.getInvokeType();
        switch (sofaCallType) {
            case RpcConstants.INVOKER_TYPE_BI_STREAMING:
                return MethodDescriptor.MethodType.BIDI_STREAMING;
            case RpcConstants.INVOKER_TYPE_CLIENT_STREAMING:
                return MethodDescriptor.MethodType.CLIENT_STREAMING;
            case RpcConstants.INVOKER_TYPE_SERVER_STREAMING:
                return MethodDescriptor.MethodType.SERVER_STREAMING;
            default:
                return MethodDescriptor.MethodType.UNARY;
        }
    }

    private SofaResponse streamCall(SofaRequest sofaRequest, int timeout, MethodDescriptor.MethodType callType) {
        switch (callType) {
            case BIDI_STREAMING:
                return binaryStreamCall(sofaRequest, timeout);
            case CLIENT_STREAMING:
                return clientStreamCall(sofaRequest, timeout);
            case SERVER_STREAMING:
                return serverStreamCall(sofaRequest, timeout);
            default:
                throw new SofaRpcException(RpcErrorType.CLIENT_CALL_TYPE, "Unknown stream call type:" + callType);
        }
    }


    private SofaResponse unaryCall(SofaRequest sofaRequest, int timeout) throws Exception{
        MethodDescriptor methodDescriptor = getMethodDescriptor(sofaRequest);
        Request request = getRequest(sofaRequest, serialization, serializer, 0);
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

    private SofaResponse stubCall(SofaRequest sofaRequest, int timeout) throws Exception{
        SofaResponse sofaResponse = new SofaResponse();
        Object stub = sofaStub.invoke(null, channel, buildCustomCallOptions(sofaRequest, timeout),
                timeout);
        final Method method = sofaRequest.getMethod();
        Object appResponse = method.invoke(stub, sofaRequest.getMethodArgs()[0]);
        sofaResponse.setAppResponse(appResponse);
        return sofaResponse;
    }

    private SofaResponse binaryStreamCall(SofaRequest sofaRequest, int timeout) {
        StreamHandler streamHandler = (StreamHandler) sofaRequest.getMethodArgs()[0];

        MethodDescriptor<Request, Response> methodDescriptor = getMethodDescriptor(sofaRequest);
        ClientCall<Request, Response> call = channel.newCall(methodDescriptor, buildCustomCallOptions(sofaRequest, timeout));

        StreamObserver<Request> observer = ClientCalls.asyncBidiStreamingCall(
                call,
                new ClientStreamObserverAdapter(
                        streamHandler,
                        sofaRequest.getSerializeType()
                )
        );
        StreamHandler<Request> handler = new StreamHandler() {
            @Override
            public void onMessage(Object message) {
                SofaRequest request = MessageBuilder.copyEmptyRequest(sofaRequest);
                Object[] args = new Object[]{message};
                request.setMethodArgs(args);
                request.setMethodArgSigs(rebuildTrueRequestArgSigs(args));
                Request req = getRequest(request, serialization, serializer, 0);
                observer.onNext(req);
            }

            @Override
            public void onFinish() {
                observer.onCompleted();
            }

            @Override
            public void onException(Throwable throwable) {
                observer.onError(TripleExceptionUtils.asStatusRuntimeException(throwable));
            }
        };
        SofaResponse sofaResponse = new SofaResponse();
        sofaResponse.setAppResponse(handler);
        return sofaResponse;
    }

    private SofaResponse clientStreamCall(SofaRequest sofaRequest, int timeout) {
        return binaryStreamCall(sofaRequest, timeout);
    }

    private SofaResponse serverStreamCall(SofaRequest sofaRequest, int timeout) {
        StreamHandler streamHandler = (StreamHandler) sofaRequest.getMethodArgs()[0];

        MethodDescriptor<Request, Response> methodDescriptor = getMethodDescriptor(sofaRequest);
        ClientCall<Request, Response> call = channel.newCall(methodDescriptor, buildCustomCallOptions(sofaRequest, timeout));

        Request req = getRequest(sofaRequest, serialization, serializer, 1);

        ClientStreamObserverAdapter responseObserver = new ClientStreamObserverAdapter(streamHandler, sofaRequest.getSerializeType());

        ClientCalls.asyncServerStreamingCall(call, req, responseObserver);

        return new SofaResponse();
    }

    @Override
    public ResponseFuture asyncInvoke(SofaRequest sofaRequest, int timeout) throws Exception {
        SofaResponseCallback sofaResponseCallback = sofaRequest.getSofaResponseCallback();
        TripleResponseFuture future = new TripleResponseFuture(sofaRequest, timeout);

        ClassLoader currentClassLoader = ClassLoaderUtils.getCurrentClassLoader();
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
                    processSuccess(false, context, sofaRequest, o, sofaResponseCallback, future, currentClassLoader);
                }

                @Override
                public void onError(Throwable throwable) {
                    processError(context, sofaRequest, throwable, sofaResponseCallback, future, currentClassLoader);
                }

                @Override
                public void onCompleted() {

                }
            });
        } else {
            MethodDescriptor methodDescriptor = getMethodDescriptor(sofaRequest);
            Request request = getRequest(sofaRequest, serialization, serializer, 0);
            ClientCalls.asyncUnaryCall(channel.newCall(methodDescriptor, buildCustomCallOptions(sofaRequest, timeout)), request, new StreamObserver<Object>() {
                @Override
                public void onNext(Object o) {
                    processSuccess(true, context, sofaRequest, o, sofaResponseCallback, future, currentClassLoader);
                }

                @Override
                public void onError(Throwable throwable) {
                    processError(context, sofaRequest, throwable, sofaResponseCallback, future, currentClassLoader);
                }

                @Override
                public void onCompleted() {

                }
            });
        }
        return future;
    }

    /**
     * Build arg sigs for stream calls.
     *
     * @param requestArgs request args
     * @return arg sigs, arg.getClass().getName().
     */
    private String[] rebuildTrueRequestArgSigs(Object[] requestArgs) {
        String[] classes = new String[requestArgs.length];
        for (int k = 0; k < requestArgs.length; k++) {
            if (requestArgs[k] != null) {
                classes[k] = requestArgs[k].getClass().getName();
            } else {
                classes[k] = void.class.getName();
            }
        }
        return classes;
    }

    private void processSuccess(boolean needDecode, RpcInternalContext context, SofaRequest sofaRequest, Object o, SofaResponseCallback sofaResponseCallback, TripleResponseFuture future, ClassLoader classLoader) {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            RpcInternalContext.setContext(context);

            SofaResponse sofaResponse = new SofaResponse();
            sofaResponse.setAppResponse(o);
            if (EventBus.isEnable(ClientAsyncReceiveEvent.class)) {
                EventBus.post(new ClientAsyncReceiveEvent(consumerConfig, providerInfo,
                        sofaRequest, sofaResponse, null));
            }

            pickupBaggage(context, sofaResponse);

            // do async filter after respond server
            FilterChain chain = consumerConfig.getConsumerBootstrap().getCluster().getFilterChain();
            if (chain != null) {
                chain.onAsyncResponse(consumerConfig, sofaRequest, sofaResponse, null);
            }

            recordClientElapseTime(context);

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

    private void processError(RpcInternalContext context, SofaRequest sofaRequest, Throwable throwable, SofaResponseCallback sofaResponseCallback, TripleResponseFuture future, ClassLoader classLoader) {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            RpcInternalContext.setContext(context);

            if (EventBus.isEnable(ClientAsyncReceiveEvent.class)) {
                EventBus.post(new ClientAsyncReceiveEvent(consumerConfig, providerInfo,
                        sofaRequest, null, throwable));
            }

            // do async filter after respond server
            FilterChain chain = consumerConfig.getConsumerBootstrap().getCluster().getFilterChain();
            if (chain != null) {
                chain.onAsyncResponse(consumerConfig, sofaRequest, null, throwable);
            }

            recordClientElapseTime(context);

            if (EventBus.isEnable(ClientEndInvokeEvent.class)) {
                EventBus.post(new ClientEndInvokeEvent(sofaRequest, null, throwable));
            }

            if (sofaResponseCallback != null) {
                Status status = Status.fromThrowable(throwable);
                if (status.getCode() == Status.Code.UNKNOWN) {
                    sofaResponseCallback.onAppException(throwable, sofaRequest.getMethodName(), sofaRequest);
                } else {
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

    protected void recordClientElapseTime(RpcInternalContext context) {
        if (context != null) {
            Long startTime = (Long) context.removeAttachment(RpcConstants.INTERNAL_KEY_CLIENT_SEND_TIME);
            if (startTime != null) {
                context.setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE, RpcRuntimeContext.now() - startTime);
            }
        }
    }

    protected void pickupBaggage(RpcInternalContext context, SofaResponse response) {
        if (RpcInvokeContext.isBaggageEnable()) {
            RpcInvokeContext old = null;
            RpcInvokeContext newContext = null;
            if (context != null) {
                old = (RpcInvokeContext) context.getAttachment(RpcConstants.HIDDEN_KEY_INVOKE_CONTEXT);
            }
            if (old != null) {
                RpcInvokeContext.setContext(old);
            }
            newContext = RpcInvokeContext.getContext();
            BaggageResolver.pickupFromResponse(newContext, response);

            if (old != null) {
                old.getAllResponseBaggage().putAll(newContext.getAllResponseBaggage());
                old.getAllRequestBaggage().putAll(newContext.getAllRequestBaggage());
            }

        }
    }

    private MethodDescriptor<Request, Response> getMethodDescriptor(SofaRequest sofaRequest) {
        String serviceName = sofaRequest.getInterfaceName();
        String methodName = sofaRequest.getMethodName();
        MethodDescriptor.Marshaller<?> requestMarshaller = ProtoUtils.marshaller(Request.getDefaultInstance());
        MethodDescriptor.Marshaller<?> responseMarshaller = ProtoUtils.marshaller(Response.getDefaultInstance());
        String fullMethodName = generateFullMethodName(serviceName, methodName);

        MethodDescriptor.Builder builder = MethodDescriptor
                .newBuilder()
                .setFullMethodName(fullMethodName)
                .setSampledToLocalTracing(true)
                .setRequestMarshaller((MethodDescriptor.Marshaller<Object>) requestMarshaller)
                .setResponseMarshaller((MethodDescriptor.Marshaller<Object>) responseMarshaller);

        MethodDescriptor.MethodType callType = SofaProtoUtils.mapGrpcCallType(sofaRequest.getInvokeType());
        builder.setType(callType);
        return builder.build();
    }

    /**
     * set some custom info
     *
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