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
import com.alipay.sofa.rpc.context.ServerAsyncResponseSender;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.transport.ByteArrayWrapperByteBuf;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import triple.Response;

/**
 * Triple protocol implementation of ServerAsyncResponseSender.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class TripleServerAsyncResponseSender implements ServerAsyncResponseSender {

    /**
     * The underlying Triple StreamObserver
     */
    private final StreamObserver<Response> responseObserver;

    /**
     * Flag to track if response has been sent
     */
    private volatile boolean               sent = false;

    /**
     * Constructor
     *
     * @param responseObserver the Triple StreamObserver
     */
    public TripleServerAsyncResponseSender(StreamObserver<Response> responseObserver) {
        this.responseObserver = responseObserver;
    }

    @Override
    public void sendResponse(SofaResponse response) {
        checkState();

        Object appResponse = response.getAppResponse();
        if (appResponse instanceof Throwable) {
            // For errors, send through onError
            responseObserver.onError(new RuntimeException(appResponse.toString()));
        } else {
            // For normal response, build and send Triple Response
            try {
                // Use hessian2 as default serializer
                Serializer serializer = SerializerFactory.getSerializer("hessian2");
                byte[] data = serializer.encode(appResponse, null).array();

                Response.Builder builder = Response.newBuilder();
                builder.setSerializeType("hessian2");
                builder.setData(ByteString.copyFrom(data));
                Response tripleResponse = builder.build();

                responseObserver.onNext(tripleResponse);
                responseObserver.onCompleted();
            } catch (Exception e) {
                responseObserver.onError(e);
            }
        }
    }

    @Override
    public boolean isSent() {
        return sent;
    }

    /**
     * Check if response has been sent, throw exception if already sent
     */
    private void checkState() {
        if (sent) {
            throw new IllegalStateException("Async response has already been sent");
        }
        sent = true;
    }
}