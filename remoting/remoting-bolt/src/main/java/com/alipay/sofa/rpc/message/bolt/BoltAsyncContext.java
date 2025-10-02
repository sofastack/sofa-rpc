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
package com.alipay.sofa.rpc.message.bolt;

import com.alipay.remoting.AsyncContext;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerEndHandleEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;

public class BoltAsyncContext {
    private volatile boolean         sent = false;

    private final AsyncContext       asyncContext;
    private final SofaRequest        sofaRequest;

    private final RpcInternalContext internalContext;
    private final RpcInvokeContext   invokeCtx;
    private final ClassLoader        restoreClassLoader;
    private volatile ClassLoader     stagedClassLoader;

    public BoltAsyncContext() {
        internalContext = RpcInternalContext.getContext();
        asyncContext = (AsyncContext) internalContext.getAttachment(RpcConstants.HIDDEN_KEY_ASYNC_CONTEXT);
        sofaRequest = (SofaRequest) internalContext.getAttachment(RpcConstants.HIDDEN_KEY_ASYNC_REQUEST);
        invokeCtx = RpcInvokeContext.getContext();
        restoreClassLoader = Thread.currentThread().getContextClassLoader();
        invokeCtx.put(RemotingConstants.INVOKE_CTX_IS_ASYNC_CHAIN, true);
    }

    public synchronized void signalContextSwitch() {
        RpcInvokeContext.setContext(invokeCtx);
        RpcInternalContext.setContext(internalContext);
        if (restoreClassLoader != null) {
            Thread.currentThread().setContextClassLoader(restoreClassLoader);
            stagedClassLoader = Thread.currentThread().getContextClassLoader();
        }
    }

    public synchronized void resetContext() {
        RpcInvokeContext.removeContext();
        RpcInternalContext.removeContext();
        // 修复 ClassLoader 恢复逻辑
        if (stagedClassLoader != null) {
            Thread.currentThread().setContextClassLoader(stagedClassLoader);
        }
    }

    private synchronized void checkState() {
        if (sent) {
            throw new IllegalStateException("Current async context has already sent response");
        }
        sent = true;
    }

    public void write(Object result) {
        checkState();
        try {
            SofaResponse response = new SofaResponse();
            response.setAppResponse(result);
            sendResponse(response, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send async response", e);
        }
    }

    public void writeException(Throwable throwable) {
        checkState();
        try {
            SofaResponse response = new SofaResponse();
            if (throwable instanceof SofaRpcException) {
                SofaRpcException sofaRpcException = (SofaRpcException) throwable;
                response.setErrorMsg(sofaRpcException.getMessage());
                sendResponse(response, sofaRpcException);
            } else {
                response.setAppResponse(throwable);
                sendResponse(response, null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send async exception response", e);
        }
    }

    private void sendResponse(SofaResponse response, SofaRpcException sofaRpcException) {
        try {
            asyncContext.sendResponse(response);
        } finally {
            if (EventBus.isEnable(ServerSendEvent.class)) {
                EventBus.post(new ServerSendEvent(sofaRequest, response, sofaRpcException));
            }
            if (EventBus.isEnable(ServerEndHandleEvent.class)) {
                EventBus.post(new ServerEndHandleEvent());
            }
        }
    }
}
