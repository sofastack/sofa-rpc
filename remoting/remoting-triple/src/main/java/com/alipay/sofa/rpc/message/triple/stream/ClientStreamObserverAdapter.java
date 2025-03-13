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
package com.alipay.sofa.rpc.message.triple.stream;

import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.alipay.sofa.rpc.transport.SofaStreamObserver;
import io.grpc.stub.StreamObserver;

/**
 * ClientStreamObserverAdapter.
 */
public class ClientStreamObserverAdapter implements StreamObserver<triple.Response> {

    private final SofaStreamObserver<Object> sofaStreamObserver;

    private final Serializer                 serializer;

    private volatile Class<?>                returnType;

    private final ClassLoader                classLoader;

    /**
     * Instantiates a new triple stream invoker callback adapter.
     *
     * @param sofaStreamObserver stream callback
     * @param serializeType serialize type
     * @param classLoader Classloader of the rpc calling thread
     */
    public ClientStreamObserverAdapter(SofaStreamObserver<Object> sofaStreamObserver, byte serializeType,
                                       ClassLoader classLoader) {
        this.sofaStreamObserver = sofaStreamObserver;
        this.serializer = SerializerFactory.getSerializer(serializeType);
        this.classLoader = classLoader;
    }

    @Override
    public void onNext(triple.Response response) {
        byte[] responseData = response.getData().toByteArray();
        Object appResponse;
        String returnTypeName = response.getType();
        if (responseData != null && responseData.length > 0) {
            ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                if (returnType == null && !returnTypeName.isEmpty()) {
                    returnType = Class.forName(returnTypeName, true, classLoader);
                }
                appResponse = serializer.decode(new ByteArrayWrapperByteBuf(responseData), returnType, null);
                sofaStreamObserver.onNext(appResponse);
            } catch (ClassNotFoundException e) {
                throw new SofaRpcException(RpcErrorType.CLIENT_DESERIALIZE, "Can not find return type :" + returnType,
                    e);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassloader);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            sofaStreamObserver.onError(t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }

    @Override
    public void onCompleted() {
        ClassLoader oldClassloader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            sofaStreamObserver.onCompleted();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader);
        }
    }
}
