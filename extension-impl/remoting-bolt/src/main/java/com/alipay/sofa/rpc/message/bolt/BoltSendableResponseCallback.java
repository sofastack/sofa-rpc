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
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.BaggageResolver;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SendableResponseCallback;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerEndHandleEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;

/**
 * Async response callback, can send data to upstream when receive data from downstream.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public abstract class BoltSendableResponseCallback<T> implements SendableResponseCallback<T> {

    /**
     * 请求对应的上下文
     */
    protected AsyncContext asyncContext;

    /**
     *
     */
    protected SofaRequest  request;

    /**
     * 是否已发送
     */
    private boolean        sent;

    public BoltSendableResponseCallback() {
        init();
    }

    /**
     * 初始化数据
     */
    protected void init() {
        // 从ThreadLocal取出当前长连接，request等信息设置进去，需要接到请求时提前设置到ThreadLocal里
        RpcInternalContext context = RpcInternalContext.getContext();
        asyncContext = (AsyncContext) context.getAttachment(RpcConstants.HIDDEN_KEY_ASYNC_CONTEXT);
        request = (SofaRequest) context.getAttachment(RpcConstants.HIDDEN_KEY_ASYNC_REQUEST);
    }

    @Override
    public void onAppException(Throwable throwable, String methodName, RequestBase request) {
        sendAppException(throwable);
    }

    @Override
    public void onSofaException(SofaRpcException sofaException, String methodName,
                                RequestBase request) {
        sendSofaException(sofaException);
    }

    /**
     * A->B(当前)->C的场景下，将远程服务端C的结果异步返回给调用者A
     *
     * @see SofaResponseCallback#onAppResponse(Object, String, RequestBase)
     */
    @Override
    public void sendAppResponse(Object appResponse) {
        checkState();
        SofaResponse response = new SofaResponse();
        response.setAppResponse(appResponse);
        sendSofaResponse(response, null);
    }

    /**
     * A->B(当前)->C的场景下，将远程服务端C的业务异常异步返回给调用者A
     *
     * @see SofaResponseCallback#onAppException(Throwable, String, RequestBase)
     */
    @Override
    public void sendAppException(Throwable throwable) {
        checkState();
        SofaResponse response = new SofaResponse();
        response.setAppResponse(throwable);
        sendSofaResponse(response, null);
    }

    /**
     * A->B(当前)->C的场景下，将远程服务端C的RPc异常异步返回给调用者A
     *
     * @see SofaResponseCallback#onSofaException(SofaRpcException, String, RequestBase)
     */
    @Override
    public void sendSofaException(SofaRpcException sofaException) {
        checkState();
        SofaResponse response = new SofaResponse();
        response.setErrorMsg(sofaException.getMessage());
        sendSofaResponse(response, sofaException);
    }

    /**
     * 检测是否已经返回过响应，不能重复发送
     */
    protected void checkState() {
        if (sent) {
            throw new IllegalStateException("AsyncProxyResponseCallback has been sent response");
        }
        sent = true;
    }

    /**
     * 发送响应数据
     *
     * @param response 响应
     * @param sofaException SofaRpcException
     */
    protected void sendSofaResponse(SofaResponse response, SofaRpcException sofaException) {
        try {
            if (RpcInvokeContext.isBaggageEnable()) {
                BaggageResolver.carryWithResponse(RpcInvokeContext.peekContext(), response);
            }
            asyncContext.sendResponse(response);
        } finally {
            if (EventBus.isEnable(ServerSendEvent.class)) {
                EventBus.post(new ServerSendEvent(request, response, sofaException));
            }
            if (EventBus.isEnable(ServerEndHandleEvent.class)) {
                EventBus.post(new ServerEndHandleEvent());
            }
        }
    }
}
