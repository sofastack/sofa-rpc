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
import com.alipay.sofa.rpc.server.triple.TripleContants;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端设置返回响应Header的拦截器
 * <p>
 * Created by zhanggeng on 2017/1/25.
 */
public class ServerResHeaderInterceptor extends TripleServerInterceptor {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServerResHeaderInterceptor.class);

    public ServerResHeaderInterceptor(ServerServiceDefinition serverServiceDefinition) {
        super(serverServiceDefinition);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
                                                                 final Metadata requestHeaders,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(new SimpleForwardingServerCall<ReqT, RespT>(call) {
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

                final RpcInvokeContext context = RpcInvokeContext.getContext();
                context.put(TripleContants.SOFA_APP_RESPONSE_KEY, message);
            }

            @Override
            public void close(Status status, Metadata trailers) {
                if (RpcRunningState.isDebugMode()) {
                    LOGGER.info("[6]send response message:{},trailers:{}", status, trailers);
                }
                super.close(status, trailers);
            }
        }, requestHeaders);
    }
}