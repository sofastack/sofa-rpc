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
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.transport.triple.http.ServerHttpChannelObserver;

/**
 * Server call listener for unary (request-response) calls.
 * This is the most common call type - single request, single response.
 */
public class UnaryServerCallListener extends AbstractServerCallListener {

    private Object requestMessage;

    public UnaryServerCallListener(SofaRequest request, Invoker invoker,
                                   ServerHttpChannelObserver<?> responseObserver) {
        super(request, invoker, responseObserver);
    }

    @Override
    public void onMessage(Object message) {
        this.requestMessage = message;
        if (message instanceof Object[]) {
            request.setMethodArgs((Object[]) message);
        } else {
            // Single argument
            request.setMethodArgs(new Object[] { message });
        }
    }

    @Override
    public void onComplete() {
        // All request data received, invoke the service
        invoke();
    }

    @Override
    public void onReturn(Object value) {
        responseObserver.onNext(value);
        responseObserver.onCompleted();
    }

    @Override
    public void onCancel(long code) {
        // Handle cancellation - no response needed
        responseObserver.close();
    }

    /**
     * Get the request message.
     *
     * @return request message
     */
    public Object getRequestMessage() {
        return requestMessage;
    }
}