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
package com.alipay.sofa.rpc.interceptor;

import com.alipay.common.tracer.core.context.trace.SofaTraceContext;
import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientAsyncReceiveEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.server.triple.TripleContants;
import com.alipay.sofa.rpc.tracer.sofatracer.TripleTracerAdapter;
import com.alipay.sofa.rpc.utils.TripleExceptionUtils;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Grpc客户端侧的拦截器，主要是发送隐式传参，状态记录等
 * <p>
 * Created by zhanggeng on 2017/2/8.
 */
public class ClientHeaderClientInterceptor implements ClientInterceptor {

    public static final Logger LOGGER = LoggerFactory
                                          .getLogger(ClientHeaderClientInterceptor.class);

    public ClientHeaderClientInterceptor() {
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        //这里和下面不在一个线程
        if (RpcRunningState.isDebugMode()) {
            LOGGER.info("[1]header send from client:");
        }
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(
            method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata requestHeader) {
                RpcInternalContext internalContext = RpcInternalContext.getContext();
                RpcInvokeContext context = RpcInvokeContext.getContext();
                SofaRequest sofaRequest = (SofaRequest) context.get(TripleContants.SOFA_REQUEST_KEY);

                ConsumerConfig<?> consumerConfig = (ConsumerConfig<?>) context
                    .get(TripleContants.SOFA_CONSUMER_CONFIG_KEY);
                TripleTracerAdapter.beforeSend(sofaRequest, consumerConfig, requestHeader, method);
                SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
                SofaTracerSpan clientSpan = sofaTraceContext.getCurrentSpan();
                if (RpcRunningState.isDebugMode()) {
                    LOGGER.info("[2]prepare to send from client:{}", requestHeader);
                }
                super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata responseHeader) {
                        // 客户端收到响应Header
                        if (RpcRunningState.isDebugMode()) {
                            LOGGER.info("[3]response header received from server:{}", responseHeader);
                        }
                        super.onHeaders(responseHeader);
                    }

                    @Override
                    public void onMessage(RespT message) {
                        // onMessage -> onNext()
                        try {
                            if (sofaRequest.isAsync()) {
                                RpcInvokeContext.setContext(context);
                                sofaTraceContext.push(clientSpan);
                            }
                            if (RpcRunningState.isDebugMode()) {
                                LOGGER.info("[4]response message received from server:{}", message);
                            }
                            super.onMessage(message);
                        } finally {
                            if (sofaRequest.isAsync()) {
                                sofaTraceContext.clear();
                                RpcInvokeContext.removeContext();
                            }
                        }
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        // onClose -> onComplete() or onError()
                        try {
                            if (sofaRequest.isAsync()) {
                                RpcInvokeContext.setContext(context);
                                sofaTraceContext.push(clientSpan);
                            }
                            if (RpcRunningState.isDebugMode()) {
                                LOGGER.info("[5]response close received from server:{},trailers:{}", status, trailers);
                            }
                            super.onClose(status, trailers);
                        } finally {
                            if (sofaRequest.isAsync()) {
                                Throwable throwable = TripleExceptionUtils.getThrowableFromStatus(status);
                                RpcInternalContext.setContext(internalContext);
                                if (EventBus.isEnable(ClientAsyncReceiveEvent.class)) {
                                    EventBus.post(new ClientAsyncReceiveEvent(consumerConfig, null,
                                        sofaRequest, new SofaResponse(), throwable));
                                }
                                RpcInvokeContext.removeContext();
                                RpcInternalContext.removeAllContext();
                            }
                        }
                    }

                    @Override
                    public void onReady() {
                        if (RpcRunningState.isDebugMode()) {
                            LOGGER.info("[5]client is ready");
                        }
                        super.onReady();
                    }
                }, requestHeader);
            }

            @Override
            public void sendMessage(ReqT message) {
                try {
                    super.sendMessage(message);
                } catch (Throwable t) {
                    LOGGER.error("Client invoke grpc sendMessage meet error:", t);
                    throw t;
                }
            }
        };
    }
}