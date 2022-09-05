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
import com.alipay.sofa.rpc.common.cache.ReflectCache;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.tracer.sofatracer.TracingContextKey;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
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

    protected Invoker           invoker;
    protected ProviderConfig    providerConfig;

    public GenericServiceImpl(Invoker invoker, ProviderConfig providerConfig) {
        super();
        this.invoker = invoker;
        this.providerConfig = providerConfig;
        String key = ConfigUniqueNameGenerator.getUniqueName(providerConfig);
        // 缓存接口的方法
        for (Method m : providerConfig.getProxyClass().getMethods()) {
            ReflectCache.putOverloadMethodCache(key, m);
        }
    }

    @Override
    public void generic(Request request, StreamObserver<Response> responseObserver) {

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        SofaRequest sofaRequest = TracingContextKey.getKeySofaRequest().get(Context.current());
        String methodName = sofaRequest.getMethodName();
        try {
            String key = ConfigUniqueNameGenerator.getUniqueName(providerConfig);
            ClassLoader interfaceClassLoader = providerConfig.getProxyClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(interfaceClassLoader);

            Class[] argTypes = getArgTypes(request);
            Serializer serializer = SerializerFactory.getSerializer(request.getSerializeType());

            Method declaredMethod = ReflectCache.getOverloadMethodCache(key, methodName, request.getArgTypesList()
                .toArray(new String[0]));
            Object[] invokeArgs = getInvokeArgs(request, argTypes, serializer);

            // fill sofaRequest
            sofaRequest.setMethod(declaredMethod);
            sofaRequest.setMethodArgs(invokeArgs);
            sofaRequest.setMethodArgSigs(ClassTypeUtils.getTypeStrs(argTypes, true));

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

    private Class[] getArgTypes(Request request) {
        ProtocolStringList argTypesList = request.getArgTypesList();
        int size = argTypesList.size();
        Class[] argTypes = new Class[size];
        for (int i = 0; i < size; i++) {
            String typeName = argTypesList.get(i);
            argTypes[i] = ClassTypeUtils.getClass(typeName);
        }
        return argTypes;
    }

    private Object[] getInvokeArgs(Request request, Class[] argTypes, Serializer serializer) {
        List<ByteString> argsList = request.getArgsList();
        Object[] args = new Object[argsList.size()];

        for (int i = 0; i < argsList.size(); i++) {
            byte[] data = argsList.get(i).toByteArray();
            args[i] = serializer.decode(new ByteArrayWrapperByteBuf(data), argTypes[i],
                null);
        }
        return args;
    }
}