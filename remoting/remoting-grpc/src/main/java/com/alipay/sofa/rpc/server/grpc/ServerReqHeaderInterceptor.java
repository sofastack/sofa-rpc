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

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerReceiveEvent;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务端收请求Header的拦截器
 * <p>
 * Created by zhanggeng on 2017/1/25.
 */
public class ServerReqHeaderInterceptor implements ServerInterceptor {

    public static final Logger  LOGGER         = LoggerFactory
                                                   .getLogger(ServerReqHeaderInterceptor.class);

    private final static String CONTEXT_PRRFIX = RemotingConstants.RPC_TRACE_NAME + ".";

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
                                                                 final Metadata requestHeaders,
                                                                 ServerCallHandler<ReqT, RespT> next) {

        //LOGGER.info("header received from client:" + requestHeaders); 这里和下面不在一个线程
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
            next.startCall(call, requestHeaders)) {

            @Override
            public void onComplete() {
                // 和代码执行不一定在一个线程池
                super.onComplete();
            }

            @Override
            public void onHalfClose() {
                // LOGGER.info("[1]header received from client:" + requestHeaders + " from " + socketAddress.toString());
                // 服务端收到请求Header
                SofaRequest sofaRequest = new SofaRequest();
                if (requestHeaders.containsKey(GrpcHeadKeys.HEAD_KEY_TARGET_SERVICE)) {
                    sofaRequest.setTargetServiceUniqueName(requestHeaders
                        .get(GrpcHeadKeys.HEAD_KEY_TARGET_SERVICE));
                }
                if (requestHeaders.containsKey(GrpcHeadKeys.HEAD_KEY_TARGET_APP)) {
                    sofaRequest.setTargetAppName(requestHeaders
                        .get(GrpcHeadKeys.HEAD_KEY_TARGET_APP));
                }
                if (requestHeaders.containsKey(GrpcHeadKeys.HEAD_KEY_METHOD_NAME)) {
                    sofaRequest
                        .setMethodName(requestHeaders.get(GrpcHeadKeys.HEAD_KEY_METHOD_NAME));
                }

                if (requestHeaders.containsKey(GrpcHeadKeys.HEAD_KEY_TRACE_ID_APP)) {
                    sofaRequest.addRequestProp(RemotingConstants.NEW_RPC_TRACE_NAME,
                        requestHeaders.get(GrpcHeadKeys.HEAD_KEY_TRACE_ID_APP));
                }

                Map<String, String> traceMap = new HashMap<String, String>();
                for (String traceKey : requestHeaders.keys()) {
                    if (traceKey.startsWith(CONTEXT_PRRFIX)) {
                        Metadata.Key<String> k = GrpcHeadKeys.getKey(traceKey);
                        traceMap.put(traceKey.substring(CONTEXT_PRRFIX.length()),
                            requestHeaders.get(k));
                    }
                }
                if (!traceMap.isEmpty()) {
                    sofaRequest.addRequestProp(RemotingConstants.RPC_TRACE_NAME, traceMap);
                }
                if (EventBus.isEnable(ServerReceiveEvent.class)) {
                    EventBus.post(new ServerReceiveEvent(sofaRequest));
                }

                super.onHalfClose();
            }
        };
    }
}
