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

import com.alibaba.triple.proto.GenericServiceGrpc;
import com.alibaba.triple.proto.Request;
import com.alibaba.triple.proto.Response;
import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import io.grpc.stub.StreamObserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.alipay.sofa.rpc.server.triple.TripleContants.SOFA_REQUEST_KEY;


/**
 * @author zhaowang
 * @version : GenericServiceImpl.java, v 0.1 2020年05月27日 9:19 下午 zhaowang Exp $
 */
public class GenericServiceImpl extends GenericServiceGrpc.GenericServiceImplBase {

    protected Object ref;
    protected Class proxyClass;
    protected Map<String, Method> methodMap = new ConcurrentHashMap<>();
    protected Serializer serializer;

    public GenericServiceImpl(ProviderConfig providerConfig) {
        super();
        ref = providerConfig.getRef();
        proxyClass = providerConfig.getProxyClass();
        serializer = SerializerFactory.getSerializer("hessian2");
        // TODO  调用 真正的方法
    }

    @Override
    public void generic(Request request, StreamObserver<Response> responseObserver) {
        // TODO 异常处理
        SofaRequest sofaRequest = (SofaRequest)RpcInvokeContext.getContext().get(SOFA_REQUEST_KEY);
        String methodName = sofaRequest.getMethodName();
        Class[] argTypes = getArgTypes(request);
        try {
            Method declaredMethod = proxyClass.getDeclaredMethod(methodName, argTypes);
            Object result = declaredMethod.invoke(ref, getInvokeArgs(request, argTypes));

            Response.Builder builder = Response.newBuilder();
            builder.setType(declaredMethod.getReturnType().getName());
            builder.setData(ByteString.copyFrom(serializer.encode(result, null).array()));
            Response build = builder.build();
            responseObserver.onNext(build);
            responseObserver.onCompleted();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }


    }

    private Class[] getArgTypes(Request request) {
        ProtocolStringList argTypesList = request.getArgTypesList();
        int size = argTypesList.size();
        Class[] argTypes = new Class[size];
        for (int i = 0; i < size; i++) {
            String typeName = argTypesList.get(i);
            argTypes[i] = ClassUtils.forName(typeName);
        }
        return argTypes;
    }

    private Object[] getInvokeArgs(Request request, Class[] argTypes) {
        List<ByteString> argsList = request.getArgsList();
        Object[] args = new Object[argsList.size()];

        for (int i = 0; i < argsList.size(); i++) {
            Object arg = serializer.decode(new ByteArrayWrapperByteBuf(argsList.get(i).toByteArray()), argTypes[i], null);
            args[i] = arg;
        }
        return args;
    }
}