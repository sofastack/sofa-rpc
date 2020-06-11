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
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
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
        final ServerServiceDefinition serverServiceDefinition = this.getServerServiceDefinition();

        SofaResponse sofaResponse = new SofaResponse();
        final Throwable[] throwable = { null };
        SofaRequest sofaRequest = new SofaRequest();
        TripleTracerAdapter.serverReceived(sofaRequest, serverServiceDefinition, call, requestHeaders);
        SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
        SofaTracerSpan serverSpan = sofaTraceContext.getCurrentSpan();

        Context ctxWithSpan = Context.current()
            .withValue(TracingContextKey.getKey(), serverSpan)
            .withValue(TracingContextKey.getSpanContextKey(), serverSpan.context())
            .withValue(TracingContextKey.getKeySofaRequest(), sofaRequest);

        //这里和下面不在一个线程
        if (RpcRunningState.isDebugMode()) {
            LOGGER.info("[1]header received from client:" + requestHeaders);
        }

        ServerCall<ReqT, RespT> realCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                if (RpcRunningState.isDebugMode()) {
                    LOGGER.info("[4]send response header:{}", responseHeaders);
                }
                super.sendHeaders(responseHeaders);
            }

            //服务端发完了
            @Override
            public void sendMessage(RespT message) {
                if (RpcRunningState.isDebugMode()) {
                    LOGGER.info("[5]send response message:{}", message);
                }
                super.sendMessage(message);

                sofaResponse.setAppResponse(message);
            }

            @Override
            public void close(Status status, Metadata trailers) {
                if (RpcRunningState.isDebugMode()) {
                    LOGGER.info("[6]send response message:{},trailers:{}", status, trailers);
                }
                super.close(status, trailers);
            }
        };

        ServerCall.Listener<ReqT> listenerWithContext =
                Contexts.interceptCall(ctxWithSpan, realCall, requestHeaders, next);

        ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> result = new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
            listenerWithContext) {

            //完成的时候走到这里
            @Override
            public void onComplete() {
                // 和代码执行不一定在一个线程池
                super.onComplete();
                if (RpcRunningState.isDebugMode()) {
                    LOGGER.info("[7]server processed done received from client:" + requestHeaders);
                }
                TripleTracerAdapter.serverReceived(sofaRequest, serverServiceDefinition, call, requestHeaders);

                //进行一下补偿
                SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
                SofaTracerSpan serverSpan = sofaTraceContext.getCurrentSpan();
                SofaTracerSpan originalSpan = (SofaTracerSpan) TracingContextKey.getKey().get(ctxWithSpan);
                serverSpan.setStartTime(originalSpan.getStartTime());
                serverSpan.setTag("remote.ip", originalSpan.getTagsWithStr().get("remote.ip"));
                long endTime = RpcRuntimeContext.now();
                serverSpan.setTag("biz.impl.time", endTime - originalSpan.getStartTime());
                TripleTracerAdapter.serverSend(sofaRequest, requestHeaders, sofaResponse, throwable[0]);
            }

            //客户端发完了
            @Override
            public void onHalfClose() {
                if (RpcRunningState.isDebugMode()) {
                    LOGGER.info("[2]body received done from client:" + requestHeaders);
                }
                // 服务端收到所有信息
                TripleTracerAdapter.serverReceived(sofaRequest, serverServiceDefinition, call, requestHeaders);
                try {
                    super.onHalfClose();
                } catch (Throwable t) {
                    // 统一处理异常
                    StatusRuntimeException exception = fromThrowable(t);
                    // 调用 call.close() 发送 Status 和 metadata
                    // 这个方式和 onError()本质是一样的
                    call.close(exception.getStatus(), exception.getTrailers());
                    throwable[0] = t;
                }
            }

            private StatusRuntimeException fromThrowable(Throwable t) {
                final Metadata trailers = new Metadata();
                return new StatusRuntimeException(Status.UNKNOWN, trailers);
            }
        };
        return result;
    }
}