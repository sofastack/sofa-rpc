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

import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.server.triple.TripleContants;
import com.alipay.sofa.rpc.tracer.sofatracer.TripleTracerAdapter;
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
        //这里和下面不在一个线程
        if (RpcRunningState.isDebugMode()) {
            LOGGER.info("[1]header received from client:" + requestHeaders);
        }

        final ServerServiceDefinition serverServiceDefinition = this.getServerServiceDefinition();
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
            next.startCall(call, requestHeaders)) {

            //完成的时候走到这里
            @Override
            public void onComplete() {
                // 和代码执行不一定在一个线程池
                super.onComplete();
                if (RpcRunningState.isDebugMode()) {
                    LOGGER.info("[7]server processed done received from client:" + requestHeaders);
                }
                SofaResponse sofaResponse = new SofaResponse();
                final RpcInvokeContext context = RpcInvokeContext.getContext();
                Throwable exp = (Throwable) context.get(TripleContants.SOFA_APP_EXCEPTION_KEY);
                Object appResponse = context.get(TripleContants.SOFA_APP_RESPONSE_KEY);
                sofaResponse.setAppResponse(appResponse);
                TripleTracerAdapter.serverSend(requestHeaders, sofaResponse, exp);
            }

            //客户端发完了
            @Override
            public void onHalfClose() {
                if (RpcRunningState.isDebugMode()) {
                    LOGGER.info("[2]body received done from client:" + requestHeaders);
                }
                // 服务端收到所有信息
                TripleTracerAdapter.serverReceived(serverServiceDefinition, call, requestHeaders);
                try {
                    super.onHalfClose();
                } catch (Throwable t) {
                    // 统一处理异常
                    StatusRuntimeException exception = fromThrowable(t);
                    // 调用 call.close() 发送 Status 和 metadata
                    // 这个方式和 onError()本质是一样的
                    call.close(exception.getStatus(), exception.getTrailers());

                    final RpcInvokeContext context = RpcInvokeContext.getContext();
                    context.put(TripleContants.SOFA_APP_EXCEPTION_KEY, t);
                }
            }

            private StatusRuntimeException fromThrowable(Throwable t) {
                final Metadata trailers = new Metadata();
                return new StatusRuntimeException(Status.UNKNOWN, trailers);
            }
        };
    }
}
