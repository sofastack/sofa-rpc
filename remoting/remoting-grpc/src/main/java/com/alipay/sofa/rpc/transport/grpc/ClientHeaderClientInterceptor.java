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
package com.alipay.sofa.rpc.transport.grpc;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.server.grpc.GrpcHeadKeys;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Grpc客户端侧的拦截器，主要是发送隐式传参，状态记录等
 * <p>
 * Created by zhanggeng on 2017/2/8.
 */
public class ClientHeaderClientInterceptor implements ClientInterceptor {

    public static final Logger LOGGER = LoggerFactory
                                          .getLogger(ClientHeaderClientInterceptor.class);

    /**
     * 服务配置信息
     */
    private ConsumerConfig     serviceMetadata;
    /**
     * 当前拦截器对应的RpcUrl关键字
     */
    private ProviderInfo       rpcUrl;

    private SofaRequest        sofaRequest;

    public ClientHeaderClientInterceptor(SofaRequest sofaRequest, ConsumerConfig metadata, ProviderInfo rpcUrl) {
        this.sofaRequest = sofaRequest;
        this.serviceMetadata = metadata;
        this.rpcUrl = rpcUrl;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {

        //LOGGER.info("header received from client:" + requestHeaders); 这里和下面不在一个线程

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(
            method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata requestHeader) {
                // 客户端设置请求服务端的Header
                Map<String, String> header = new HashMap<String, String>();
                header.put(RemotingConstants.HEAD_METHOD_NAME, sofaRequest.getMethodName());
                header.put(RemotingConstants.HEAD_TARGET_SERVICE, sofaRequest.getTargetServiceUniqueName());
                header.put(RemotingConstants.HEAD_TARGET_APP, sofaRequest.getTargetAppName());
                String tracerStr = (String) sofaRequest.getRequestProp(RemotingConstants.NEW_RPC_TRACE_NAME);
                header.put(RemotingConstants.NEW_RPC_TRACE_NAME, tracerStr);
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    requestHeader.put(GrpcHeadKeys.getKey(entry.getKey()), entry.getValue());
                }

                // LOGGER.info("[2]response header received from server:{}", requestHeader);

                super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata responseHeader) {
                        // 客户端收到响应Header  TODO
                        //LOGGER.info("[3]response header received from server:{}", responseHeader);
                        super.onHeaders(responseHeader);
                    }

                    @Override
                    public void onMessage(RespT message) {
                        //LOGGER.info("[4]response message received from server:{}", message);
                        super.onMessage(message);
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        //LOGGER.info("------------onClose--------------");
                        if (status.getCode() == Status.UNAVAILABLE.getCode()) { // 当前连接已经不可用
                            /*  GrpcConnectionHolder.getInstance().refresh(serviceMetadata,
                                  rpcUrl.toString(), false);*/
                        }
                        super.onClose(status, trailers);
                    }

                }, requestHeader);
            }

        };
    }
}