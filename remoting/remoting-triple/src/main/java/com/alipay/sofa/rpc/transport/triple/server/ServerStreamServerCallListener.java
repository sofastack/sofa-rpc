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
package com.alipay.sofa.rpc.transport.triple.server;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.transport.triple.http.ServerHttpChannelObserver;

/**
 * Server call listener for server streaming calls.
 * Single request, multiple responses (stream).
 */
public class ServerStreamServerCallListener extends AbstractServerCallListener {

    private boolean invoked = false;

    public ServerStreamServerCallListener(SofaRequest request, Invoker invoker,
                                          ServerHttpChannelObserver<?> responseObserver) {
        super(request, invoker, responseObserver);
    }

    @Override
    public void onMessage(Object message) {
        if (message instanceof Object[]) {
            request.setMethodArgs((Object[]) message);
        } else {
            request.setMethodArgs(new Object[] { message });
        }
    }

    @Override
    public void onComplete() {
        // All request data received, invoke the service
        if (!invoked) {
            invoke();
        }
    }

    @Override
    public void onReturn(Object value) {
        // For server streaming, each onNext call sends a stream message
        // The service implementation should call onNext multiple times
        responseObserver.onNext(value);
    }

    @Override
    public void onCancel(long code) {
        // Handle cancellation
        responseObserver.close();
    }

    @Override
    protected SofaResponse invoke() {
        invoked = true;
        try {
            // For server streaming, the result is typically the response observer
            // The service method will call onNext multiple times
            Object result = invoker.invoke(request);
            if (result instanceof SofaResponse) {
                SofaResponse response = (SofaResponse) result;
                if (response.getAppResponse() != null) {
                    responseObserver.onNext(response.getAppResponse());
                }
            }
            // Complete the stream after invocation
            responseObserver.onCompleted();
        } catch (Throwable t) {
            responseObserver.onError(t);
        }
        return null;
    }
}