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
 * Abstract base class for server call listeners.
 * Handles the invocation flow for different call types (unary, streaming).
 */
public abstract class AbstractServerCallListener {

    protected final SofaRequest                  request;
    protected final Invoker                      invoker;
    protected final ServerHttpChannelObserver<?> responseObserver;

    protected AbstractServerCallListener(SofaRequest request, Invoker invoker,
                                         ServerHttpChannelObserver<?> responseObserver) {
        this.request = request;
        this.invoker = invoker;
        this.responseObserver = responseObserver;
    }

    /**
     * Called when a message is received from the client.
     *
     * @param message the received message
     */
    public abstract void onMessage(Object message);

    /**
     * Called when the request is complete (all messages received).
     */
    public abstract void onComplete();

    /**
     * Called when a response is ready to be sent.
     *
     * @param value the response value
     */
    public abstract void onReturn(Object value);

    /**
     * Called when the call is cancelled.
     *
     * @param code cancellation code
     */
    public abstract void onCancel(long code);

    /**
     * Invoke the actual service method.
     *
     * @return the response
     */
    protected SofaResponse invoke() {
        SofaResponse response = new SofaResponse();
        try {
            Object result = invoker.invoke(request);
            if (result instanceof SofaResponse) {
                response = (SofaResponse) result;
            } else {
                response.setAppResponse(result);
            }
            onReturn(response.getAppResponse());
        } catch (Throwable t) {
            response.setErrorMsg(t.getMessage());
            responseObserver.onError(t);
        }
        return response;
    }

    /**
     * Get the request.
     *
     * @return request
     */
    public SofaRequest getRequest() {
        return request;
    }

    /**
     * Get the response observer.
     *
     * @return response observer
     */
    public ServerHttpChannelObserver<?> getResponseObserver() {
        return responseObserver;
    }
}