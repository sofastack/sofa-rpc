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
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.transport.SofaStreamObserver;
import com.alipay.sofa.rpc.utils.TripleExceptionUtils;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import triple.Response;

/**
 * Response serialize stream handler.
 */
public class ResponseSerializeSofaStreamObserver<T> implements SofaStreamObserver<T> {

    private final StreamObserver<triple.Response> streamObserver;

    private Serializer                            serializer;

    private String                                serializeType;

    public ResponseSerializeSofaStreamObserver(StreamObserver<triple.Response> streamObserver, String serializeType) {
        this.streamObserver = streamObserver;
        if (StringUtils.isNotBlank(serializeType)) {
            this.serializer = SerializerFactory.getSerializer(serializeType);
            this.serializeType = serializeType;
        }
    }

    @Override
    public void onNext(T message) {
        Response.Builder builder = Response.newBuilder();
        builder.setType(message.getClass().getName());
        builder.setSerializeType(serializeType);
        builder.setData(ByteString.copyFrom(serializer.encode(message, null).array()));

        streamObserver.onNext(builder.build());
    }

    @Override
    public void onCompleted() {
        streamObserver.onCompleted();
    }

    @Override
    public void onError(Throwable throwable) {
        streamObserver.onError(TripleExceptionUtils.asStatusRuntimeException(throwable));
    }

    public void setSerializeType(String serializeType) {
        this.serializer = SerializerFactory.getSerializer(serializeType);
        this.serializeType = serializeType;
    }

}
