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
package com.alipay.sofa.rpc.server.triple;

import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.cache.ReflectCache;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.triple.stream.ResponseSerializeSofaStreamObserver;
import com.alipay.sofa.rpc.tracer.sofatracer.TracingContextKey;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.alipay.sofa.rpc.transport.SofaStreamObserver;
import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import triple.Request;
import triple.Response;
import triple.SofaGenericServiceTriple;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author zhaowang
 * @version : GenericServiceImpl.java, v 0.1 2020年05月27日 9:19 下午 zhaowang Exp $
 */
public class GenericServiceImpl extends SofaGenericServiceTriple.GenericServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericServiceImpl.class);

    protected UniqueIdInvoker   invoker;

    public GenericServiceImpl(UniqueIdInvoker invoker) {
        super();
        this.invoker = invoker;
    }

    @Override
    public void generic(Request request, StreamObserver<Response> responseObserver) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        SofaRequest sofaRequest = TracingContextKey.getKeySofaRequest().get(Context.current());
        String methodName = sofaRequest.getMethodName();
        Method declaredMethod = invoker.getDeclaredMethod(sofaRequest, request, RpcConstants.INVOKER_TYPE_UNARY);
        if (declaredMethod == null) {
            throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find invoke method " +
                methodName);
        }

        try {
            ClassLoader serviceClassLoader = invoker.getServiceClassLoader(sofaRequest);
            Thread.currentThread().setContextClassLoader(serviceClassLoader);
            Serializer serializer = SerializerFactory.getSerializer(request.getSerializeType());
            setUnaryOrServerRequestParams(sofaRequest, request, serializer, declaredMethod, false);

            SofaResponse response = invoker.invoke(sofaRequest);
            Object ret = getAppResponse(declaredMethod, response);

            Response.Builder builder = Response.newBuilder();
            builder.setSerializeType(request.getSerializeType());
            builder.setType(declaredMethod.getReturnType().getName());
            builder.setData(ByteString.copyFrom(serializer.encode(ret, null).array()));
            Response build = builder.build();
            responseObserver.onNext(build);
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Invoke " + methodName + " error:", e);
            throw new SofaRpcRuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public StreamObserver<Request> genericBiStream(StreamObserver<Response> responseObserver) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        //通过上下文创建请求
        SofaRequest sofaRequest = TracingContextKey.getKeySofaRequest().get(Context.current());
        String uniqueName = invoker.getServiceUniqueName(sofaRequest);
        Method serviceMethod = ReflectCache.getOverloadMethodCache(uniqueName, sofaRequest.getMethodName(),
            new String[] { SofaStreamObserver.class.getCanonicalName() });

        if (serviceMethod == null) {
            throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find invoke method " +
                sofaRequest.getMethodName());
        }
        String methodName = serviceMethod.getName();
        try {
            ClassLoader serviceClassLoader = invoker.getServiceClassLoader(sofaRequest);
            Thread.currentThread().setContextClassLoader(serviceClassLoader);
            ResponseSerializeSofaStreamObserver serverResponseHandler = new ResponseSerializeSofaStreamObserver(
                responseObserver,
                null);

            setBidirectionalStreamRequestParams(sofaRequest, serviceMethod, serverResponseHandler);

            SofaResponse sofaResponse = invoker.invoke(sofaRequest);

            Object appResponse = sofaResponse.getAppResponse();
            if (appResponse instanceof Exception) {
                throw (Exception) appResponse;
            }
            SofaStreamObserver<Object> clientHandler = (SofaStreamObserver<Object>) appResponse;

            return new StreamObserver<Request>() {
                private volatile Serializer serializer    = null;

                private volatile String     serializeType = null;

                private volatile Class<?>[] argTypes      = null;

                @Override
                public void onNext(Request request) {
                    try {
                        Thread.currentThread().setContextClassLoader(serviceClassLoader);
                        checkInitialize(request);
                        Object message = getInvokeArgs(request, argTypes, serializer, false)[0];
                        serverResponseHandler.setSerializeType(serializeType);
                        clientHandler.onNext(message);
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    try {
                        Thread.currentThread().setContextClassLoader(serviceClassLoader);
                        clientHandler.onError(t);
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }

                }

                @Override
                public void onCompleted() {
                    try {
                        Thread.currentThread().setContextClassLoader(serviceClassLoader);
                        clientHandler.onCompleted();
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }

                }

                private void checkInitialize(Request request) {
                    if (serializer == null && argTypes == null) {
                        synchronized (this) {
                            if (serializer == null && argTypes == null) {
                                serializeType = request.getSerializeType();
                                serializer = SerializerFactory.getSerializer(request.getSerializeType());
                                argTypes = getArgTypes(request, false);
                            }
                        }
                    }
                }
            };
        } catch (Exception e) {
            LOGGER.error("Invoke " + methodName + " error:", e);
            throw new SofaRpcRuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public void genericServerStream(Request request, StreamObserver<Response> responseObserver) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        SofaRequest sofaRequest = TracingContextKey.getKeySofaRequest().get(Context.current());
        Method serviceMethod = invoker.getDeclaredMethod(sofaRequest, request, RpcConstants.INVOKER_TYPE_SERVER_STREAMING);

        if (serviceMethod == null) {
            throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find invoke method " +
                sofaRequest.getMethodName());
        }

        String methodName = serviceMethod.getName();
        try {
            ClassLoader serviceClassLoader = invoker.getServiceClassLoader(sofaRequest);
            Thread.currentThread().setContextClassLoader(serviceClassLoader);
            Serializer serializer = SerializerFactory.getSerializer(request.getSerializeType());

            setUnaryOrServerRequestParams(sofaRequest, request, serializer, serviceMethod, true);
            sofaRequest.getMethodArgs()[sofaRequest.getMethodArgs().length -1] = new ResponseSerializeSofaStreamObserver<>(responseObserver, request.getSerializeType());

            invoker.invoke(sofaRequest);
        } catch (Exception e) {
            LOGGER.error("Invoke " + methodName + " error:", e);
            throw new SofaRpcRuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    /**
     * Resolve method invoke args into request for unary or server-streaming calls.
     *
     * @param sofaRequest    SofaRequest
     * @param request        Request
     * @param serializer     Serializer
     * @param declaredMethod Target invoke method
     */
    private void setUnaryOrServerRequestParams(SofaRequest sofaRequest, Request request,
                                               Serializer serializer, Method declaredMethod, boolean isServerStreamCall) {
        Class[] argTypes = getArgTypes(request, isServerStreamCall);
        Object[] invokeArgs = getInvokeArgs(request, argTypes, serializer, isServerStreamCall);

        // fill sofaRequest
        sofaRequest.setMethod(declaredMethod);
        sofaRequest.setMethodArgs(invokeArgs);
        sofaRequest.setMethodArgSigs(ClassTypeUtils.getTypeStrs(argTypes, true));
    }

    /**
     * Resolve method invoke args into request for bidirectional stream calls.
     *
     * @param sofaRequest             SofaRequest
     * @param serviceMethod           Target service method
     * @param serverStreamPushHandler The StreamHandler used to push a message to a client. It's a wrapper for {@link StreamObserver}, and encode method return value to {@link Response}.
     */
    private void setBidirectionalStreamRequestParams(SofaRequest sofaRequest, Method serviceMethod,
                                                     SofaStreamObserver<Response> serverStreamPushHandler) {
        Class[] argTypes = new Class[] { SofaStreamObserver.class };
        Object[] invokeArgs = new Object[] { serverStreamPushHandler };

        sofaRequest.setMethod(serviceMethod);
        sofaRequest.setMethodArgs(invokeArgs);
        sofaRequest.setMethodArgSigs(ClassTypeUtils.getTypeStrs(argTypes, true));
    }

    private Object getAppResponse(Method method, SofaResponse response) {
        if (response.isError()) {
            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, response.getErrorMsg());
        }
        Object ret = response.getAppResponse();
        if (ret instanceof Throwable) {
            throw new SofaRpcRuntimeException((Throwable) ret);
        } else {
            if (ret == null) {
                ret = ClassUtils.getDefaultPrimitiveValue(method.getReturnType());
            }
        }
        return ret;
    }

    /**
     * Get argument types from request.
     * @param request original request
     * @param addStreamHandler Whether add StreamHandler as the first method param.
     * <p>
     * For server stream call, the StreamHandler won't be transported.
     * To make the argument list conform to the method definition, we need to add it as first method param manually.
     *
     * @return param types of target method
     */
    private Class[] getArgTypes(Request request, boolean addStreamHandler) {
        ProtocolStringList argTypesList = request.getArgTypesList();

        int size = addStreamHandler ? argTypesList.size() + 1 : argTypesList.size();
        Class[] argTypes = new Class[size];

        for (int i = 0; i < argTypesList.size(); i++) {
            String typeName = argTypesList.get(i);
            argTypes[i] = ClassTypeUtils.getClass(typeName);
        }

        if (addStreamHandler) {
            argTypes[size - 1] = SofaStreamObserver.class;
        }
        return argTypes;
    }

    /**
     * Get arguments from request.
     * @param addStreamHandler if addStreamHandler == true, the first arg will be left blank and set later.
     *
     * @return params of target method.
     */
    private Object[] getInvokeArgs(Request request, Class[] argTypes, Serializer serializer, boolean addStreamHandler) {
        List<ByteString> argsList = request.getArgsList();
        int size = addStreamHandler ? argsList.size() + 1 : argsList.size();

        Object[] args = new Object[size];
        for (int i = 0; i < argsList.size(); i++) {
            byte[] data = argsList.get(i).toByteArray();
            args[i] = serializer.decode(new ByteArrayWrapperByteBuf(data), argTypes[i],
                null);
        }
        return args;
    }
}