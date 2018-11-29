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

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc;
import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc.GrpcTestServiceImplBase;
import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc.GrpcTestServiceStub;
import com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String;
import com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String;
import com.alipay.sofa.rpc.model.grpc.impl.GrpcTestServiceImpl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.Test;

import java.io.IOException;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.22 8:00 PM
 */
public class GrpcProviderTest {

    @Test
    public void test() {
        ServerConfig serverConfig = new ServerConfig()
            .setPort(9090)
            .setProtocol("grpc");

        ProviderConfig<GrpcTestServiceImplBase> providerConfig = new ProviderConfig<GrpcTestServiceImplBase>()
            .setInterfaceId(GrpcTestServiceImplBase.class.getName())
            .setRef(new GrpcTestServiceImpl())
            .setServer(serverConfig)
            .setBootstrap("grpc")
            .setRegister(false);
        providerConfig.export();

        ConsumerConfig<GrpcTestServiceImplBase> consumerConfig = new ConsumerConfig<GrpcTestServiceImplBase>()
            .setInterfaceId(GrpcTestServiceImplBase.class.getName())
            .setDirectUrl("grpc://127.0.0.1:9090")
            .setProtocol("grpc")
            .setBootstrap("grpc")
            .setLazy(true)
            .setRegister(false);
        GrpcTestServiceImplBase grpcTestService = consumerConfig.refer();

        GrpcTestService_Request_String request = GrpcTestService_Request_String.newBuilder().setName("AAA").build();

        io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver = new io.grpc.stub
                .StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>() {
                    @Override
                    public void onNext(GrpcTestService_Response_String grpcTestService_response_string) {

                        System.out.println("response:" + grpcTestService_response_string);

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onCompleted() {

                    }
                };

        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            System.out.println("1):" + System.currentTimeMillis());

            grpcTestService.reqString(request, responseObserver);

            System.out.println(":::" + (System.currentTimeMillis() - start));
        }

        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGrpc() throws IOException {
        Server server = ServerBuilder.forPort(9090).addService(new GrpcTestServiceImpl()).build().start();



        GrpcTestService_Request_String request = GrpcTestService_Request_String.newBuilder().setName("AAA").build();

        io.grpc.stub.StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String> responseObserver = new io.grpc.stub
                .StreamObserver<com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String>() {
                    @Override
                    public void onNext(GrpcTestService_Response_String grpcTestService_response_string) {

                        System.out.println("response:" + grpcTestService_response_string);

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("throwable:" + throwable);

                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("response:" + "onCompleted");

                    }
                };

        //for (int i = 0; i < 10; i++) {
        //    long start = System.currentTimeMillis();
        //    System.out.println("dsfsdfsf:" + System.currentTimeMillis());
        //    ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("127.0.0.1", 9090).usePlaintext().build();
        //    GrpcTestServiceStub stub = GrpcTestServiceGrpc.newStub(managedChannel);
        //    stub.reqString(request, responseObserver);
        //
        //    System.out.println(":::" + (System.currentTimeMillis() - start));
        //
        //}

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("127.0.0.1", 9090).usePlaintext().build();
        GrpcTestServiceStub stub = GrpcTestServiceGrpc.newStub(managedChannel);
        stub.reqString(request, responseObserver);

        ManagedChannel managedChannel2 = ManagedChannelBuilder.forAddress("127.0.0.1", 9090).usePlaintext().build();
        GrpcTestServiceStub stub2 = GrpcTestServiceGrpc.newStub(managedChannel2);
        stub2.reqString(request, responseObserver);

        managedChannel2.shutdownNow();
        stub.reqString(request, responseObserver);
        //stub2.reqString(request, responseObserver);
        stub.reqString(request, responseObserver);

        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}