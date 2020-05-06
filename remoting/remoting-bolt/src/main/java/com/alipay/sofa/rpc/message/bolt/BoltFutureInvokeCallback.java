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

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.AsyncRuntime;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientAsyncReceiveEvent;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.filter.FilterChain;

import java.util.concurrent.Executor;

/**
 * 为了使Future模式下，也能正常的记录相关信息，采用Callback模式进行包装
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltFutureInvokeCallback extends AbstractInvokeCallback {

    /**
     * 请求结果Future
     */
    public BoltResponseFuture rpcFuture;

    /**
     * Instantiates a new Bolt future invoke callback.
     *
     * @param consumerConfig the consumer config
     * @param providerInfo   the provider info
     * @param rpcFuture      the rpc future
     * @param request        the request
     * @param context        the context
     * @param classLoader    the class loader
     */
    public BoltFutureInvokeCallback(ConsumerConfig consumerConfig, ProviderInfo providerInfo,
                                    BoltResponseFuture rpcFuture, SofaRequest request,
                                    RpcInternalContext context, ClassLoader classLoader) {
        super(consumerConfig, providerInfo, request, context, classLoader);
        this.rpcFuture = rpcFuture;
    }

    @Override
    public void onResponse(Object result) {
        if (rpcFuture == null) {
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

            Object appResp = response.getAppResponse();
            if (response.isError()) { // rpc层异常
                SofaRpcException sofaRpcException = new SofaRpcException(
                    RpcErrorType.SERVER_UNDECLARED_ERROR, response.getErrorMsg());
                rpcFuture.setFailure(sofaRpcException);
            } else if (appResp instanceof Throwable) { // 业务层异常
                throwable = (Throwable) appResp;
                rpcFuture.setFailure(throwable);
            } else {
                rpcFuture.setSuccess(appResp);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
        }
    }

    @Override
    public void onException(Throwable e) {
        if (rpcFuture == null) {
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

            rpcFuture.setFailure(e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
        }
    }

    @Override
    public Executor getExecutor() {
        return AsyncRuntime.getAsyncThreadPool();
    }
}
