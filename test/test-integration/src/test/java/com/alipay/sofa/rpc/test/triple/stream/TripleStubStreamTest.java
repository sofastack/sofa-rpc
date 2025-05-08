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
package com.alipay.sofa.rpc.test.triple.stream;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.triple.GreeterImpl;
import com.alipay.sofa.rpc.test.triple.NativeGrpcGreeterImpl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.SofaGreeterTriple;
import io.grpc.stub.StreamObserver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Even
 * @date 2024/4/10 14:45
 */
public class TripleStubStreamTest {

    private static final Logger                               LOGGER = LoggerFactory
                                                                         .getLogger(TripleStubStreamTest.class);

    private static ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig;
    private static ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig;
    private static SofaGreeterTriple.IGreeter                 greeterStub;

    private static GreeterGrpc.GreeterBlockingStub            nativeBlockingStub;
    private static GreeterGrpc.GreeterStub                    nativeAsyncStub;

    private static ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfigToNative;
    private static SofaGreeterTriple.IGreeter                 greeterStubToNative;

    @BeforeClass
    public static void beforeClass() throws InterruptedException, IOException {
        RpcRunningState.setUnitTestMode(true);
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server");
        int port = 50052;
        ServerConfig serverConfig = new ServerConfig()
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setPort(port);

        providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
                .setApplication(applicationConfig)
                .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
                .setRef(new GreeterImpl())
                .setServer(serverConfig);
        providerConfig.export();

        ApplicationConfig consumerApp = new ApplicationConfig().setAppName("triple-client");
        consumerConfig = new ConsumerConfig<>();
        consumerConfig.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
                .setApplication(consumerApp)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setDirectUrl("tri://127.0.0.1:" + port + "?appName=triple-server");

        greeterStub = consumerConfig.refer();

        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", 50052).usePlaintext()
                .build();
        nativeBlockingStub = GreeterGrpc.newBlockingStub(channel);
        nativeAsyncStub = GreeterGrpc.newStub(channel);

        Server server = ServerBuilder.forPort(50051)
                .addService(new NativeGrpcGreeterImpl())
                .build();
        server.start();

        consumerConfigToNative = new ConsumerConfig<>();
        consumerConfigToNative.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
                .setApplication(consumerApp)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setDirectUrl("tri://127.0.0.1:" + 50051 + "?appName=triple-server");
        greeterStubToNative = consumerConfigToNative.refer();
        Thread.sleep(10000);
    }

    @AfterClass
    public static void afterClass() {
        consumerConfig.unRefer();
        providerConfig.unExport();
        consumerConfigToNative.unRefer();
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }

    @Test
    public void testTripleSayHello() {
        HelloRequest request = HelloRequest.newBuilder().setName("Jack").build();
        HelloReply helloReply = greeterStub.sayHello(request);
        Assert.assertEquals("Hello Jack", helloReply.getMessage());
    }

    @Test
    public void testTripleStubBiStream() throws InterruptedException {
        HelloRequest request = HelloRequest.newBuilder().setName("Hello world!").build();
        CountDownLatch biCountDownLatch = new CountDownLatch(5);
        StreamObserver<HelloRequest> requestStreamObserver = greeterStub
            .sayHelloBinary(new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply value) {
                    Assert.assertEquals(value.getMessage(), request.getName());
                    LOGGER.info("bi stream resp onNext");
                    biCountDownLatch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.error("bi stream resp error", t);
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("bi stream resp onCompleted");
                    biCountDownLatch.countDown();
                }
            });
        requestStreamObserver.onNext(request);
        requestStreamObserver.onNext(request);
        requestStreamObserver.onCompleted();
        Assert.assertTrue(biCountDownLatch.await(10, TimeUnit.SECONDS));

    }

    @Test
    public void testTripleStubClientStream() throws InterruptedException {
        HelloRequest request = HelloRequest.newBuilder().setName("Hello world!").build();
        CountDownLatch clientStreamCountDownLatch = new CountDownLatch(2);
        StreamObserver<HelloRequest> helloRequestStreamObserver = greeterStub
            .sayHelloClientStream(new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply value) {
                    LOGGER.info("client stream resp onCompleted");
                    Assert.assertEquals(value.getMessage(), request.getName() + 2);
                    clientStreamCountDownLatch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.error("client stream resp error", t);
                }

                @Override
                public void onCompleted() {
                    clientStreamCountDownLatch.countDown();
                    LOGGER.info("client stream resp onCompleted");
                }
            });

        helloRequestStreamObserver.onNext(request);
        helloRequestStreamObserver.onNext(request);
        helloRequestStreamObserver.onCompleted();
        Assert.assertTrue(clientStreamCountDownLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testTripleStubServerStream() throws InterruptedException {
        HelloRequest request = HelloRequest.newBuilder().setName("Hello world!").build();
        CountDownLatch blockServerStreamCountDownLatch = new CountDownLatch(3);
        Iterator<HelloReply> helloReplyIterator = greeterStub.sayHelloServerStream(request);
        int i = 0;
        while (helloReplyIterator.hasNext()) {
            i++;
            blockServerStreamCountDownLatch.countDown();
            Assert.assertEquals(helloReplyIterator.next().getMessage(), request.getName() + i);
        }
        Assert.assertTrue(blockServerStreamCountDownLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch serverStreamCountDownLatch = new CountDownLatch(3);
        greeterStub.sayHelloServerStream(request, new StreamObserver<HelloReply>() {

            int i = 0;

            @Override
            public void onNext(HelloReply value) {
                LOGGER.info("server stream resp onNext");
                i++;
                serverStreamCountDownLatch.countDown();
                Assert.assertEquals(value.getMessage(), request.getName() + i);
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.info("server stream resp onError");
            }

            @Override
            public void onCompleted() {
                LOGGER.info("server stream resp onCompleted");
            }
        });
        Assert.assertTrue(serverStreamCountDownLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testGrpcClientToTripleServer() throws InterruptedException {
        // UNARY
        HelloRequest request = HelloRequest.newBuilder().setName("world!").build();
        Assert.assertEquals("Hello world!", nativeBlockingStub.sayHello(request).getMessage());

        // BIDI_STREAMING
        CountDownLatch biCountDownLatch = new CountDownLatch(5);
        StreamObserver<HelloRequest> helloRequestStreamObserver = nativeAsyncStub
            .sayHelloBinary(new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply value) {
                    Assert.assertEquals(value.getMessage(), request.getName());
                    LOGGER.info("nativeAsyncStub sayHelloBinary resp onNext");
                    biCountDownLatch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.info("nativeAsyncStub sayHelloBinary resp onError");
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("nativeAsyncStub sayHelloBinary resp onCompleted");
                    biCountDownLatch.countDown();
                }
            });
        helloRequestStreamObserver.onNext(request);
        helloRequestStreamObserver.onNext(request);
        helloRequestStreamObserver.onCompleted();
        Assert.assertTrue(biCountDownLatch.await(10, TimeUnit.SECONDS));

        // CLIENT_STREAMING
        CountDownLatch clientStreamCountDownLatch = new CountDownLatch(2);
        StreamObserver<HelloRequest> clientStreamObserver = nativeAsyncStub
            .sayHelloClientStream(new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply value) {
                    LOGGER.info("nativeAsyncStub sayHelloClientStream resp onNext");
                    Assert.assertEquals(value.getMessage(), request.getName() + 2);
                    clientStreamCountDownLatch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.info("nativeAsyncStub sayHelloClientStream resp onError");
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("nativeAsyncStub sayHelloClientStream resp onCompleted");
                    clientStreamCountDownLatch.countDown();
                }
            });
        clientStreamObserver.onNext(request);
        clientStreamObserver.onNext(request);
        clientStreamObserver.onCompleted();
        Assert.assertTrue(clientStreamCountDownLatch.await(10, TimeUnit.SECONDS));

        // SERVER_STREAMING
        CountDownLatch serverStreamCountDownLatch = new CountDownLatch(3);
        Iterator<HelloReply> helloReplyIterator = nativeBlockingStub.sayHelloServerStream(request);
        int i = 0;
        while (helloReplyIterator.hasNext()) {
            i++;
            serverStreamCountDownLatch.countDown();
            Assert.assertEquals(helloReplyIterator.next().getMessage(), request.getName() + i);
        }
        Assert.assertTrue(serverStreamCountDownLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch serverAsyncStreamCountDownLatch = new CountDownLatch(4);
        nativeAsyncStub.sayHelloServerStream(request, new StreamObserver<HelloReply>() {
            @Override
            public void onNext(HelloReply value) {
                LOGGER.info("nativeAsyncStub sayHelloServerStream resp onNext");
                serverAsyncStreamCountDownLatch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.info("nativeAsyncStub sayHelloServerStream resp onError");
            }

            @Override
            public void onCompleted() {
                LOGGER.info("nativeAsyncStub sayHelloServerStream resp onCompleted");
                serverAsyncStreamCountDownLatch.countDown();
            }
        });
        Assert.assertTrue(serverAsyncStreamCountDownLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testTripleClientToGrpcServer() throws InterruptedException {
        // UNARY
        HelloRequest request = HelloRequest.newBuilder().setName("world!").build();
        Assert.assertEquals("Hello world!", greeterStubToNative.sayHello(request).getMessage());

        // BIDI_STREAMING
        CountDownLatch biCountDownLatch = new CountDownLatch(5);
        StreamObserver<HelloRequest> helloRequestStreamObserver = greeterStubToNative
            .sayHelloBinary(new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply value) {
                    Assert.assertEquals(value.getMessage(), request.getName());
                    LOGGER.info("greeterStubToNative sayHelloBinary resp onNext");
                    biCountDownLatch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.info("greeterStubToNative sayHelloBinary resp onError");
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("greeterStubToNative sayHelloBinary resp onCompleted");
                    biCountDownLatch.countDown();
                }
            });
        helloRequestStreamObserver.onNext(request);
        helloRequestStreamObserver.onNext(request);
        helloRequestStreamObserver.onCompleted();
        Assert.assertTrue(biCountDownLatch.await(10, TimeUnit.SECONDS));

        // CLIENT_STREAMING
        CountDownLatch clientStreamCountDownLatch = new CountDownLatch(2);
        StreamObserver<HelloRequest> clientStreamObserver = greeterStubToNative
            .sayHelloClientStream(new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply value) {
                    LOGGER.info("greeterStubToNative sayHelloClientStream resp onNext");
                    Assert.assertEquals(value.getMessage(), request.getName() + 2);
                    clientStreamCountDownLatch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.info("greeterStubToNative sayHelloServerStream resp onError");
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("greeterStubToNative sayHelloClientStream resp onCompleted");
                    clientStreamCountDownLatch.countDown();
                }
            });
        clientStreamObserver.onNext(request);
        clientStreamObserver.onNext(request);
        clientStreamObserver.onCompleted();
        Assert.assertTrue(clientStreamCountDownLatch.await(10, TimeUnit.SECONDS));

        // SERVER_STREAMING
        CountDownLatch blockServerStreamCountDownLatch = new CountDownLatch(3);
        Iterator<HelloReply> helloReplyIterator = greeterStubToNative.sayHelloServerStream(request);
        int i = 0;
        while (helloReplyIterator.hasNext()) {
            i++;
            blockServerStreamCountDownLatch.countDown();
            Assert.assertEquals(helloReplyIterator.next().getMessage(), request.getName() + i);
        }
        Assert.assertTrue(blockServerStreamCountDownLatch.await(10, TimeUnit.SECONDS));

        CountDownLatch serverStreamCountDownLatch = new CountDownLatch(3);
        greeterStubToNative.sayHelloServerStream(request, new StreamObserver<HelloReply>() {

            int i = 0;

            @Override
            public void onNext(HelloReply value) {
                LOGGER.info("server stream resp onNext");
                i++;
                serverStreamCountDownLatch.countDown();
                Assert.assertEquals(value.getMessage(), request.getName() + i);
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.info("server stream resp onError");
            }

            @Override
            public void onCompleted() {
                LOGGER.info("server stream resp onCompleted");
            }
        });
        Assert.assertTrue(serverStreamCountDownLatch.await(10, TimeUnit.SECONDS));
    }

}
