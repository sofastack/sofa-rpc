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
package com.alipay.sofa.rpc.transport.http;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientAsyncReceiveEvent;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.filter.FilterChain;

/**
 * Callback调用的响应处理器
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class CallbackInvokeClientHandler extends AbstractHttpClientHandler {

    /**
     * 请求里的实际回调对象
     */
    protected final SofaResponseCallback callback;

    /**
     * Instantiates a CallbackInvokeClientHandler
     *
     * @param consumerConfig the consumer config
     * @param providerInfo   the provider info
     * @param listener       the listener
     * @param request        the request
     * @param context        the context
     * @param classLoader    the class loader
     */
    public CallbackInvokeClientHandler(ConsumerConfig consumerConfig, ProviderInfo providerInfo,
                                       SofaResponseCallback listener, SofaRequest request,
                                       RpcInternalContext context, ClassLoader classLoader) {
        super(consumerConfig, providerInfo, request, context, classLoader);
        this.callback = listener;
    }

    @Override
    public void doOnResponse(Object result) {
        if (callback == null) {
            return;
        }
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        SofaResponse response = (SofaResponse) result;
        Throwable throwable = null;
        try {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            RpcInternalContext.setContext(context);

            if (EventBus.isEnable(ClientAsyncReceiveEvent.class)) {
                EventBus.post(new ClientAsyncReceiveEvent(consumerConfig, providerInfo,
                    request, response, null));
            }

            pickupBaggage(response);

            // do async filter after respond server
            FilterChain chain = consumerConfig.getConsumerBootstrap().getCluster().getFilterChain();
            if (chain != null) {
                chain.onAsyncResponse(consumerConfig, request, response, null);
            }

            recordClientElapseTime();
            if (EventBus.isEnable(ClientEndInvokeEvent.class)) {
                EventBus.post(new ClientEndInvokeEvent(request, response, null));
            }

            decode(response);

            Object appResp = response.getAppResponse();
            if (response.isError()) { // rpc层异常
                SofaRpcException sofaRpcException = new SofaRpcException(
                    RpcErrorType.SERVER_UNDECLARED_ERROR, response.getErrorMsg());
                callback.onSofaException(sofaRpcException, request.getMethodName(), request);
            } else if (appResp instanceof Throwable) { // 业务层异常
                throwable = (Throwable) appResp;
                callback.onAppException(throwable, request.getMethodName(), request);
            } else {
                callback.onAppResponse(appResp, request.getMethodName(), request);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
        }
    }

    @Override
    public void doOnException(Throwable e) {
        if (callback == null) {
            return;
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            RpcInternalContext.setContext(context);

            if (EventBus.isEnable(ClientAsyncReceiveEvent.class)) {
                EventBus.post(new ClientAsyncReceiveEvent(consumerConfig, providerInfo,
                    request, null, e));
            }

            // do async filter after respond server
            FilterChain chain = consumerConfig.getConsumerBootstrap().getCluster().getFilterChain();
            if (chain != null) {
                chain.onAsyncResponse(consumerConfig, request, null, e);
            }

            recordClientElapseTime();
            if (EventBus.isEnable(ClientEndInvokeEvent.class)) {
                EventBus.post(new ClientEndInvokeEvent(request, null, e));
            }

            SofaRpcException sofaRpcException = e instanceof SofaRpcException ? (SofaRpcException) e :
                new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, e.getMessage(), e);
            callback.onSofaException(sofaRpcException, request.getMethodName(), request);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
        }
    }
}
