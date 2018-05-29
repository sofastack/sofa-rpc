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
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientAsyncReceiveEvent;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.filter.FilterChain;
import com.alipay.sofa.rpc.message.http.HttpResponseFuture;

/**
 * Future调用的响应处理器
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class FutureInvokeClientHandler extends AbstractHttpClientHandler {

    /**
     * 请求结果Future
     */
    public HttpResponseFuture rpcFuture;

    /**
     * Instantiates a new FutureInvokeClientHandler.
     *
     * @param consumerConfig the consumer config
     * @param providerInfo   the provider info
     * @param rpcFuture      the rpc future
     * @param request        the request
     * @param context        the context
     * @param classLoader    the class loader
     */
    public FutureInvokeClientHandler(ConsumerConfig consumerConfig, ProviderInfo providerInfo,
                                     HttpResponseFuture rpcFuture, SofaRequest request,
                                     RpcInternalContext context, ClassLoader classLoader) {
        super(consumerConfig, providerInfo, request, context, classLoader);
        this.rpcFuture = rpcFuture;
    }

    @Override
    public void doOnResponse(Object result) {
        if (rpcFuture == null) {
            return;
        }
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        SofaResponse response = (SofaResponse) result;
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
            rpcFuture.setSuccess(response);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
        }
    }

    @Override
    public void doOnException(Throwable e) {
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
}
