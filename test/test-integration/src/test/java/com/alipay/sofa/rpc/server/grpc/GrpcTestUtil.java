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

import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc.GrpcTestServiceImplBase;
import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc.GrpcTestServiceStub;
import com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String;
import com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.30 12:15 PM
 */
public class GrpcTestUtil {
    public static String[] invoke(Object invoke) throws InterruptedException {
        final String[] result = { "", "", "" };
        final CountDownLatch countDownLatch = new CountDownLatch(3);

        if (invoke instanceof GrpcTestServiceStub) {
            ((GrpcTestServiceStub) invoke).reqString(
                GrpcTestService_Request_String.newBuilder().setName("AAA").build(),
                new StreamObserver<GrpcTestService_Response_String>() {
                    @Override
                    public void onNext(GrpcTestService_Response_String value) {
                        result[0] = value.getResult();
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                        result[1] = t.getMessage();
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        result[2] = "onCompleted";
                        countDownLatch.countDown();
                    }
                });
        } else if (invoke instanceof GrpcTestServiceImplBase) {
            ((GrpcTestServiceImplBase) invoke).reqString(GrpcTestService_Request_String.newBuilder().setName("AAA")
                .build(),
                new StreamObserver<GrpcTestService_Response_String>() {
                    @Override
                    public void onNext(GrpcTestService_Response_String value) {
                        result[0] = value.getResult();
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                        result[1] = t.getMessage();
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        result[2] = "onCompleted";
                        countDownLatch.countDown();
                    }
                });
        }

        countDownLatch.await(1, TimeUnit.SECONDS);
        return result;
    }
}