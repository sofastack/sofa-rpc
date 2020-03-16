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

import com.alipay.sofa.rpc.tracer.sofatracer.GrpcTracerAdapter;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端收请求Header的拦截器
 * <p>
 * Created by zhanggeng on 2017/1/25.
 */
public class ServerReqHeaderInterceptor implements ServerInterceptor {

    public static final Logger               LOGGER        = LoggerFactory
                                                               .getLogger(ServerReqHeaderInterceptor.class);

    public static final Metadata.Key<String> GRPC_STATUS   = Metadata.Key.of("grpc-status",
                                                               Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> GRPC_MESSAGE  = Metadata.Key.of("grpc-message",
                                                               Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> GRPC_ENCODING = Metadata.Key.of("grpc-encoding",
                                                               Metadata.ASCII_STRING_MARSHALLER);

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

            //客户端发完了
            @Override
            public void onHalfClose() {
                // LOGGER.info("[1]header received from client:" + requestHeaders + " from " + socketAddress.toString());
                // 服务端收到请求Header 如果用户是代码中直接抛出异常，会走到这里的
                GrpcTracerAdapter.serverReceived(call, requestHeaders);
                try {
                    super.onHalfClose();
                } catch (Throwable t) {
                    // 统一处理异常
                    StatusRuntimeException exception = fromThrowable(t);
                    // 调用 call.close() 发送 Status 和 metadata
                    // 这个方式和 onError()本质是一样的
                    call.close(exception.getStatus(), exception.getTrailers());
                }
            }

            private StatusRuntimeException fromThrowable(Throwable t) {
                final Metadata trailers = new Metadata();
                return new StatusRuntimeException(Status.UNKNOWN, trailers);
            }
        };
    }
}
