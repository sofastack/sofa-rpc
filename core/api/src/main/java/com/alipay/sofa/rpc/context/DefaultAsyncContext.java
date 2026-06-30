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
package com.alipay.sofa.rpc.context;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerEndHandleEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;

/**
 * Default implementation of AsyncContext.
 * Uses the ServerAsyncResponseSender to send responses.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class DefaultAsyncContext implements AsyncContext {

    /**
     * Server async response sender for sending responses
     */
    private final ServerAsyncResponseSender responseSender;

    /**
     * The original request
     */
    private final SofaResponse              sofaResponse;

    /**
     * Whether response has been sent
     */
    private volatile boolean                sent;

    /**
     * Constructor
     *
     * @param responseSender the server async response sender
     * @param sofaResponse  the response object to be populated
     */
    public DefaultAsyncContext(ServerAsyncResponseSender responseSender, SofaResponse sofaResponse) {
        this.responseSender = responseSender;
        this.sofaResponse = sofaResponse;
    }

    @Override
    public void write(Object response) {
        checkState();
        SofaResponse resp = buildResponse(response, null);
        sendResponse(resp, null);
    }

    @Override
    public void writeError(Throwable throwable) {
        checkState();
        SofaResponse resp = buildResponse(throwable, throwable);
        sendResponse(resp, null);
    }

    @Override
    public boolean isSent() {
        return sent;
    }

    /**
     * Build the response object
     */
    private SofaResponse buildResponse(Object response, Throwable throwable) {
        SofaResponse resp = new SofaResponse();
        if (throwable != null) {
            resp.setAppResponse(throwable);
        } else {
            resp.setAppResponse(response);
        }
        return resp;
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

    /**
     * Send response using the server async response sender
     */
    private void sendResponse(SofaResponse response, SofaRpcException sofaException) {
        try {
            if (RpcInvokeContext.isBaggageEnable()) {
                // Carry baggage with response
                BaggageResolver.carryWithResponse(RpcInvokeContext.peekContext(), response);
            }

            // Send response using the sender
            responseSender.sendResponse(response);
        } finally {
            if (EventBus.isEnable(ServerSendEvent.class)) {
                EventBus.post(new ServerSendEvent(null, response, sofaException));
            }
            if (EventBus.isEnable(ServerEndHandleEvent.class)) {
                EventBus.post(new ServerEndHandleEvent());
            }
        }
    }
}