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
package com.alipay.sofa.rpc.model.grpc.impl;

import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc;
import com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String;
import com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String;
import io.grpc.stub.StreamObserver;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.22 8:03 PM
 */
public class GrpcTestServiceImpl extends GrpcTestServiceGrpc.GrpcTestServiceImplBase {

    @Override
    public void reqString(GrpcTestService_Request_String request,
                          StreamObserver<GrpcTestService_Response_String> responseObserver) {
        System.out.println("yes");
        //try {
        //    Thread.sleep(10000000);
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}
        responseObserver.onNext(GrpcTestService_Response_String.newBuilder().setResult("success").build());
        responseObserver.onCompleted();

    }

    @Override
    public StreamObserver<GrpcTestService_Request_String> reqStrinClientStream(final StreamObserver<GrpcTestService_Response_String> responseObserver) {
        return new StreamObserver<GrpcTestService_Request_String>() {
            @Override
            public void onNext(GrpcTestService_Request_String grpcTestService_request_string) {
                System.out.println("onNext:" + grpcTestService_request_string);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("onError:" + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("onCompleted");
                responseObserver.onNext(GrpcTestService_Response_String.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void reqStringServerStream(GrpcTestService_Request_String request,
                                      StreamObserver<GrpcTestService_Response_String> responseObserver) {
        System.out.println("yes");

        responseObserver.onNext(GrpcTestService_Response_String.newBuilder().build());
        responseObserver.onNext(GrpcTestService_Response_String.newBuilder().build());
        responseObserver.onNext(GrpcTestService_Response_String.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<GrpcTestService_Request_String> reqStringBothStream(final StreamObserver<GrpcTestService_Response_String> responseObserver) {
        return new StreamObserver<GrpcTestService_Request_String>() {
            @Override
            public void onNext(GrpcTestService_Request_String grpcTestService_request_string) {
                System.out.println("both_onNext:" + grpcTestService_request_string);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("both_onError:" + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("both_onCompleted");
                responseObserver.onNext(GrpcTestService_Response_String.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }
}