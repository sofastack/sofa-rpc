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

import java.util.concurrent.locks.ReentrantLock;

/**
 * Response serialize stream handler.
 */
public class ResponseSerializeSofaStreamObserver<T> implements SofaStreamObserver<T> {

    private final StreamObserver<triple.Response> streamObserver;

    // ReentrantLock instead of synchronized to avoid virtual thread pinning (JDK 21+)
    private final ReentrantLock                   writeLock = new ReentrantLock();

    // volatile ensures safe publication when setSerializeType() is called from another thread
    private volatile Serializer                   serializer;

    private volatile String                       serializeType;

    public ResponseSerializeSofaStreamObserver(StreamObserver<triple.Response> streamObserver, String serializeType) {
        this.streamObserver = streamObserver;
        if (StringUtils.isNotBlank(serializeType)) {
            this.serializer = SerializerFactory.getSerializer(serializeType);
            this.serializeType = serializeType;
        }
    }

    // gRPC StreamObserver is not thread-safe, use ReentrantLock to avoid virtual thread pinning (JDK 21+)
    @Override
    public void onNext(T message) {
        Response.Builder builder = Response.newBuilder();
        builder.setType(message.getClass().getName());
        builder.setSerializeType(serializeType);
        builder.setData(ByteString.copyFrom(serializer.encode(message, null).array()));

        writeLock.lock();
        try {
            streamObserver.onNext(builder.build());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void onCompleted() {
        writeLock.lock();
        try {
            streamObserver.onCompleted();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        writeLock.lock();
        try {
            streamObserver.onError(TripleExceptionUtils.asStatusRuntimeException(throwable));
        } finally {
            writeLock.unlock();
        }
    }

    public void setSerializeType(String serializeType) {
        this.serializer = SerializerFactory.getSerializer(serializeType);
        this.serializeType = serializeType;
    }

}
