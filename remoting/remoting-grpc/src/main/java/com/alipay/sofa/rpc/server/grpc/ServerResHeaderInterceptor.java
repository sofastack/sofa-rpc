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
package com.alipay.sofa.rpc.server.grpc;

import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * 服务端设置返回响应Header的拦截器
 * <p>
 * Created by zhanggeng on 2017/1/25.
 */
public class ServerResHeaderInterceptor implements ServerInterceptor {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServerResHeaderInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
                                                                 final Metadata requestHeaders,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(new SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                super.sendHeaders(responseHeaders);
            }

            @Override
            public void sendMessage(RespT message) {
                //                LOGGER.info("[5]send response message:{}", message);
                super.sendMessage(message);
                if (EventBus.isEnable(ServerSendEvent.class)) {
                    SocketAddress address = (SocketAddress) RpcInvokeContext.getContext().get(
                        GrpcContants.SOFA_REMOTE_ADDR_KEY);
                    SofaRequest request = (SofaRequest) RpcInvokeContext.getContext()
                        .get(GrpcContants.SOFA_REQUEST_KEY);
                    if (request == null) {
                        request = new SofaRequest();
                    }
                    if (request.getTargetServiceUniqueName() == null) {
                        request.setTargetServiceUniqueName(requestHeaders.get(GrpcHeadKeys.HEAD_KEY_TARGET_SERVICE));
                    }
                    if (request.getMethodName() == null) {
                        request.setMethodName(requestHeaders.get(GrpcHeadKeys.HEAD_KEY_METHOD_NAME));
                    }
                    //添加requst TODO
                    EventBus.post(new ServerSendEvent(request, null, null));
                }
            }
        }, requestHeaders);
    }
}