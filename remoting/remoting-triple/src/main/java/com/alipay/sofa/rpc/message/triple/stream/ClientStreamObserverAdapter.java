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
import com.alipay.sofa.rpc.transport.StreamHandler;
import io.grpc.stub.StreamObserver;

/**
 * ClientStreamObserverAdapter.
 */
public class ClientStreamObserverAdapter implements StreamObserver<triple.Response> {

    private final StreamHandler<Object> streamHandler;

    private final Serializer            serializer;

    private volatile Class<?>           returnType;

    public ClientStreamObserverAdapter(StreamHandler<Object> streamHandler, byte serializeType) {
        this.streamHandler = streamHandler;
        this.serializer = SerializerFactory.getSerializer(serializeType);
    }

    @Override
    public void onNext(triple.Response response) {
        byte[] responseDate = response.getData().toByteArray();
        Object appResponse = null;
        String returnTypeName = response.getType();
        if (responseDate != null && responseDate.length > 0) {
            if (returnType == null && !returnTypeName.isEmpty()) {
                try {
                    returnType = Class.forName(returnTypeName);
                } catch (ClassNotFoundException e) {
                    throw new SofaRpcException(RpcErrorType.CLIENT_SERIALIZE, "Can not find return type :" + returnType);
                }
            }
            appResponse = serializer.decode(new ByteArrayWrapperByteBuf(responseDate), returnType, null);
        }

        streamHandler.onMessage(appResponse);
    }

    @Override
    public void onError(Throwable t) {
        streamHandler.onException(t);
    }

    @Override
    public void onCompleted() {
        streamHandler.onFinish();
    }
}
