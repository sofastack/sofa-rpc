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
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.tracer.sofatracer.TracingContextKey;
import com.alipay.sofa.rpc.tracer.sofatracer.TripleTracerAdapter;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端收请求Header的拦截器
 * <p>
 * Created by zhanggeng on 2017/1/25.
 */
public class ServerReqHeaderInterceptor extends TripleServerInterceptor {

    public static final Logger LOGGER = LoggerFactory
                                          .getLogger(ServerReqHeaderInterceptor.class);

    public ServerReqHeaderInterceptor(ServerServiceDefinition serverServiceDefinition) {
        super(serverServiceDefinition);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
                                                                 final Metadata requestHeaders,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        try {
            final ServerServiceDefinition serverServiceDefinition = this.getServerServiceDefinition();

            SofaResponse sofaResponse = new SofaResponse();
            SofaRequest sofaRequest = new SofaRequest();

            Context ctxWithSpan = convertHeaderToContext(call, requestHeaders, sofaRequest, serverServiceDefinition);

            //这里和下面不在一个线程
            if (RpcRunningState.isDebugMode()) {
                LOGGER.info("[1]header received from client:{}", requestHeaders);
            }

            ServerCall<ReqT, RespT> realCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                @Override
                public void sendHeaders(Metadata responseHeaders) {
                    try {
                        if (RpcRunningState.isDebugMode()) {
                            LOGGER.info("[4]send response header:{}", responseHeaders);
                        }
                        super.sendHeaders(responseHeaders);
                    } catch (Throwable t) {
                        LOGGER.error("Server invoke grpc sendHeaders meet error:", t);
                        throw t;
                    }
                }

                //服务端发完了
                @Override
                public void sendMessage(RespT message) {
                    try {
                        if (RpcRunningState.isDebugMode()) {
                            LOGGER.info("[5]send response message:{}", message);
                        }
                        super.sendMessage(message);

                        sofaResponse.setAppResponse(message);
                    } catch (Throwable t) {
                        LOGGER.error("Server invoke grpc sendMessage meet error:", t);
                        throw t;
                    }
                }

                @Override
                public void close(Status status, Metadata trailers) {
                    // onError -> close
                    try {
                        if (RpcRunningState.isDebugMode()) {
                            LOGGER.info("[6]send response message:{},trailers:{}", status, trailers);
                        }
                        if (status.getCause() != null) {
                            status = status.withDescription(status.getCause().getMessage());
                        }
                        super.close(status, trailers);
                    } finally {
                        if (!status.isOk()) {
                            Throwable cause = status.getCause();
                            TripleTracerAdapter.serverSend(sofaRequest, requestHeaders, sofaResponse, cause,
                                ctxWithSpan);
                        }
                    }

                }
            };
            ServerCall.Listener<ReqT> listenerWithContext;
            try {
                listenerWithContext = Contexts.interceptCall(ctxWithSpan, realCall, requestHeaders, next);
            } catch (Throwable t) {
                LOGGER.error("Server invoke grpc interceptCall meet error:", t);
                TripleTracerAdapter.serverSend(sofaRequest, requestHeaders, sofaResponse, t, ctxWithSpan);
                Status status = Status.UNKNOWN.withDescription(t.getMessage()).withCause(t);
                throw new StatusRuntimeException(status);
            }
            RpcInvokeContext invokeContext = RpcInvokeContext.getContext();
            return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                listenerWithContext) {

                @Override
                public void onCancel() {
                    // onCancel -> onError()
                    try {
                        SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
                        SofaTracerSpan originalSpan = (SofaTracerSpan) TracingContextKey.getKey().get(ctxWithSpan);
                        sofaTraceContext.push(originalSpan);
                        RpcInvokeContext.setContext(invokeContext);
                        super.onCancel();
                    } finally {
                        TripleTracerAdapter.serverSend(sofaRequest, requestHeaders, sofaResponse,
                            new StatusRuntimeException(Status.CANCELLED, new Metadata()), ctxWithSpan);
                    }
                }

                //完成的时候走到这里
                @Override
                public void onComplete() {
                    try {
                        // 和代码执行不一定在一个线程池
                        super.onComplete();
                        if (RpcRunningState.isDebugMode()) {
                            LOGGER.info("[7]server processed done received from client:" + requestHeaders);
                        }
                    } finally {
                        TripleTracerAdapter.serverSend(sofaRequest, requestHeaders, sofaResponse, null, ctxWithSpan);
                    }
                }

                @Override
                public void onMessage(ReqT message) {
                    // onMessage -> onNext()
                    SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
                    try {
                        SofaTracerSpan originalSpan = (SofaTracerSpan) TracingContextKey.getKey().get(ctxWithSpan);
                        sofaTraceContext.push(originalSpan);
                        RpcInvokeContext.setContext(invokeContext);
                        super.onMessage(message);
                    } catch (Throwable t) {
                        LOGGER.error("Server invoke grpc onMessage meet error:", t);
                        throw t;
                    } finally {
                        RpcInvokeContext.removeContext();
                        sofaTraceContext.clear();
                    }
                }

                //客户端发完了
                @Override
                public void onHalfClose() {
                    // onHalfClose -> onComplete() -> close
                    SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
                    try {
                        SofaTracerSpan originalSpan = (SofaTracerSpan) TracingContextKey.getKey().get(ctxWithSpan);
                        sofaTraceContext.push(originalSpan);
                        RpcInvokeContext.setContext(invokeContext);
                        doOnHalfClose();
                    } finally {
                        RpcInvokeContext.removeContext();
                        sofaTraceContext.clear();
                    }
                }

                private void doOnHalfClose() {
                    if (RpcRunningState.isDebugMode()) {
                        LOGGER.info("[2]body received done from client:" + requestHeaders);
                    }
                    try {
                        super.onHalfClose();
                    } catch (Throwable t) {
                        // 统一处理异常
                        final Metadata trailers = new Metadata();
                        Status status = Status.UNKNOWN.withDescription(t.getMessage()).withCause(t);
                        realCall.close(status, trailers);
                    }
                }
            };
        } finally {
            RpcInvokeContext.removeContext();
            SofaTraceContextHolder.getSofaTraceContext().clear();
        }
    }

    protected <ReqT, RespT> Context convertHeaderToContext(ServerCall<ReqT, RespT> call,
                                                           Metadata requestHeaders, SofaRequest sofaRequest,
                                                           ServerServiceDefinition serverServiceDefinition) {
        TripleTracerAdapter.serverReceived(sofaRequest, serverServiceDefinition, call, requestHeaders);
        String userId = TripleTracerAdapter.getUserId(requestHeaders);
        SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
        SofaTracerSpan serverSpan = sofaTraceContext.getCurrentSpan();
        return Context.current()
            .withValue(TracingContextKey.getKey(), serverSpan)
            .withValue(TracingContextKey.getSpanContextKey(), serverSpan.context())
            .withValue(TracingContextKey.getKeySofaRequest(), sofaRequest)
            .withValue(TracingContextKey.getKeyMetadata(), requestHeaders)
            .withValue(TracingContextKey.getKeyUserId(), userId);
    }
}