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
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.triple.stream.ResponseSerializeStreamHandler;
import com.alipay.sofa.rpc.tracer.sofatracer.TracingContextKey;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.alipay.sofa.rpc.transport.StreamHandler;
import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import triple.Request;
import triple.Response;
import triple.SofaGenericServiceTriple;

import java.lang.reflect.Method;
import java.util.List;

import static com.alipay.sofa.rpc.common.RpcOptions.DEFAULT_SERIALIZATION;

/**
 * @author zhaowang
 * @version : GenericServiceImpl.java, v 0.1 2020年05月27日 9:19 下午 zhaowang Exp $
 */
public class GenericServiceImpl extends SofaGenericServiceTriple.GenericServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericServiceImpl.class);

    protected UniqueIdInvoker   invoker;

    private ProviderConfig<?>   providerConfig;

    public GenericServiceImpl(UniqueIdInvoker invoker, ProviderConfig<?> serverConfig) {
        super();
        this.invoker = invoker;
        this.providerConfig = serverConfig;
    }

    @Override
    public void generic(Request request, StreamObserver<Response> responseObserver) {

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        SofaRequest sofaRequest = TracingContextKey.getKeySofaRequest().get(Context.current());
        String methodName = sofaRequest.getMethodName();
        try {
            Method declaredMethod = setClassLoaderAndGetRequestMethod(sofaRequest, request,
                RpcConstants.INVOKER_TYPE_UNARY);
            if (declaredMethod == null) {
                throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find invoke method " +
                    methodName);
            }
            Serializer serializer = SerializerFactory.getSerializer(request.getSerializeType());
            setUnaryOrServerRequestParams(sofaRequest, request, methodName, serializer, declaredMethod, false);

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
        Method serviceMethod = getBidirectionalStreamRequestMethod();
        //通过上下文创建请求
        SofaRequest sofaRequest = TracingContextKey.getKeySofaRequest().get(Context.current());

        if (serviceMethod == null) {
            throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find invoke method " +
                sofaRequest.getMethodName());
        }
        String methodName = serviceMethod.getName();
        try {
            ResponseSerializeStreamHandler serverResponseHandler = new ResponseSerializeStreamHandler(responseObserver,
                getSerialization());

            setBidirectionalStreamRequestParams(sofaRequest, serviceMethod, serverResponseHandler);

            SofaResponse sofaResponse = invoker.invoke(sofaRequest);

            StreamHandler<Object> clientHandler = (StreamHandler<Object>) sofaResponse.getAppResponse();

            return new StreamObserver<Request>() {
                volatile Serializer serializer = null;

                volatile Class<?>[] argTypes   = null;

                @Override
                public void onNext(Request request) {
                    checkInitialize(request);
                    Object message = getInvokeArgs(request, argTypes, serializer, false)[0];
                    clientHandler.onMessage(message);
                }

                private void checkInitialize(Request request) {
                    if (serializer == null && argTypes == null) {
                        synchronized (this) {
                            if (serializer == null && argTypes == null) {
                                serializer = SerializerFactory.getSerializer(request.getSerializeType());
                                argTypes = getArgTypes(request, false);
                            }
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    clientHandler.onException(t);
                }

                @Override
                public void onCompleted() {
                    clientHandler.onFinish();
                }
            };
        } catch (Exception e) {
            LOGGER.error("Invoke " + methodName + " error:", e);
            throw new SofaRpcRuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }
    }

    @Override
    public void genericServerStream(Request request, StreamObserver<Response> responseObserver) {
        SofaRequest sofaRequest = TracingContextKey.getKeySofaRequest().get(Context.current());
        Method serviceMethod = setClassLoaderAndGetRequestMethod(sofaRequest, request,RpcConstants.INVOKER_TYPE_SERVER_STREAMING);

        if (serviceMethod == null) {
            throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find invoke method " +
                sofaRequest.getMethodName());
        }

        String methodName = serviceMethod.getName();
        try {
            Serializer serializer = SerializerFactory.getSerializer(request.getSerializeType());

            setUnaryOrServerRequestParams(sofaRequest, request, methodName, serializer, serviceMethod, true);
            sofaRequest.getMethodArgs()[0] = new ResponseSerializeStreamHandler<>(responseObserver, getSerialization());

            invoker.invoke(sofaRequest);
        } catch (Exception e) {
            LOGGER.error("Invoke " + methodName + " error:", e);
            throw new SofaRpcRuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }
    }

    private Method setClassLoaderAndGetRequestMethod(SofaRequest sofaRequest, Request request, String callType) {
        ClassLoader serviceClassLoader = invoker.getServiceClassLoader(sofaRequest);
        Thread.currentThread().setContextClassLoader(serviceClassLoader);
        return invoker.getDeclaredMethod(sofaRequest, request, callType);
    }

    private Method getBidirectionalStreamRequestMethod() {
        SofaRequest sofaRequest = TracingContextKey.getKeySofaRequest().get(Context.current());
        String uniqueName = invoker.getServiceUniqueName(sofaRequest);
        return ReflectCache.getOverloadMethodCache(uniqueName, sofaRequest.getMethodName(),
            new String[] { StreamHandler.class.getCanonicalName() });
    }

    /**
     * Resolve method invoke args into request for unary or server-streaming calls.
     *
     * @param sofaRequest    SofaRequest
     * @param request        Request
     * @param methodName     MethodName
     * @param serializer     Serializer
     * @param declaredMethod Target invoke method
     */
    private void setUnaryOrServerRequestParams(SofaRequest sofaRequest, Request request, String methodName,
                                               Serializer serializer, Method declaredMethod, boolean isServerStreamCall) {
        setClassLoader(sofaRequest);
        if (declaredMethod == null) {
            throw new SofaRpcException(RpcErrorType.SERVER_NOT_FOUND_INVOKER, "Cannot find invoke method " +
                methodName);
        }
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
                                                     StreamHandler<Response> serverStreamPushHandler) {
        setClassLoader(sofaRequest);

        Class[] argTypes = new Class[] { StreamHandler.class };
        Object[] invokeArgs = new Object[] { serverStreamPushHandler };

        sofaRequest.setMethod(serviceMethod);
        sofaRequest.setMethodArgs(invokeArgs);
        sofaRequest.setMethodArgSigs(ClassTypeUtils.getTypeStrs(argTypes, true));
    }

    private void setClassLoader(SofaRequest sofaRequest) {
        ClassLoader serviceClassLoader = invoker.getServiceClassLoader(sofaRequest);
        Thread.currentThread().setContextClassLoader(serviceClassLoader);
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

        if (addStreamHandler) {
            argTypes[0] = StreamHandler.class;
        }
        for (int i = addStreamHandler ? 1 : 0; i < size; i++) {
            String typeName;
            if (addStreamHandler) {
                typeName = argTypesList.get(i - 1);
            } else {
                typeName = argTypesList.get(i);
            }
            argTypes[i] = ClassTypeUtils.getClass(typeName);
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
        int start = addStreamHandler ? 1 : 0;
        Object[] args = new Object[size];

        for (int i = start; i < size; i++) {
            byte[] data;
            if (addStreamHandler) {
                data = argsList.get(i - 1).toByteArray();
            } else {
                data = argsList.get(i).toByteArray();
            }
            args[i] = serializer.decode(new ByteArrayWrapperByteBuf(data), argTypes[i],
                null);
        }
        return args;
    }

    private String getSerialization() {
        String serialization = providerConfig.getSerialization();
        if (StringUtils.isBlank(serialization)) {
            serialization = getDefaultSerialization();
        }
        return serialization;
    }

    private String getDefaultSerialization() {
        return DEFAULT_SERIALIZATION;
    }
}