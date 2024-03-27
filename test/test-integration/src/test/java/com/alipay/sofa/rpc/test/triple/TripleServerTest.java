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
package com.alipay.sofa.rpc.test.triple;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.SofaGreeterTriple;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public class TripleServerTest {

    @Test
    //同步调用,直连
    public void testSync() {

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server");

        int port = 50052;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig);

        providerConfig.export();

        ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig = new ConsumerConfig<SofaGreeterTriple.IGreeter>();
        consumerConfig.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("tri://127.0.0.1:" + port);

        SofaGreeterTriple.IGreeter greeterBlockingStub = consumerConfig.refer();

        HelloRequest.DateTime dateTime = HelloRequest.DateTime.newBuilder().setDate("2018-12-28").setTime("11:13:00")
            .build();
        HelloReply reply = null;
        HelloRequest request = HelloRequest.newBuilder().setName("world").setDateTime(dateTime).build();
        reply = greeterBlockingStub.sayHello(request);

        Assert.assertNotNull(reply);

    }

    @Test
    //同步调用,直连 有uniqueId
    public void testSyncWithUniqueId() {

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server");

        int port = 50052;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
            .setApplication(applicationConfig)
            .setUniqueId("abc")
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig);

        providerConfig.export();

        ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig = new ConsumerConfig<SofaGreeterTriple.IGreeter>();
        consumerConfig.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setUniqueId("abc")
            .setDirectUrl("tri://127.0.0.1:" + port);

        SofaGreeterTriple.IGreeter greeterBlockingStub = consumerConfig.refer();

        HelloRequest.DateTime dateTime = HelloRequest.DateTime.newBuilder().setDate("2018-12-28").setTime("11:13:00")
            .build();
        HelloReply reply = null;
        HelloRequest request = HelloRequest.newBuilder().setName("world").setDateTime(dateTime).build();
        reply = greeterBlockingStub.sayHello(request);

        Assert.assertNotNull(reply);

        consumerConfig = new ConsumerConfig<SofaGreeterTriple.IGreeter>();
        consumerConfig.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("tri://127.0.0.1:" + port);

        greeterBlockingStub = consumerConfig.refer();

        dateTime = HelloRequest.DateTime.newBuilder().setDate("2018-12-28").setTime("11:13:00")
            .build();
        request = HelloRequest.newBuilder().setName("world").setDateTime(dateTime).build();
        reply = greeterBlockingStub.sayHello(request);

        Assert.assertNotNull(reply);

    }

    @Test
    public void testExposeTwice() {
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server");

        int port = 50052;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
            .setApplication(applicationConfig)
            .setUniqueId("abc")
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig);

        providerConfig.export();
        applicationConfig = new ApplicationConfig().setAppName("triple-server");

        serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);
        providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
            .setApplication(applicationConfig)
            .setUniqueId("abcd")
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig);
        providerConfig.export();
    }

    @Test
    public void testSyncSampleService() {
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server");
        int port = 50052;
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        ProviderConfig<SampleService> providerConfig = new ProviderConfig<SampleService>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(SampleService.class.getName())
            .setRef(new SampleService() {
                @Override
                public String hello(String name) {
                    return "Hello! " + name;
                }

                @Override
                public String messageSize(String msg, int responseSize) {
                    return "";
                }
            })
            .setServer(serverConfig);

        providerConfig.export();

        ConsumerConfig<SampleService> consumerConfig = new ConsumerConfig<SampleService>();
        consumerConfig.setInterfaceId(SampleService.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("tri://127.0.0.1:" + port);

        SampleService sampleService = consumerConfig.refer();

        String reply = sampleService.hello("world");
        Assert.assertNotNull(reply);
        Assert.assertEquals(reply, "Hello! world");
    }

    @Test
    public void testBiStream() throws InterruptedException {
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server");

        int port = 50052;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig);

        providerConfig.export();

        ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig = new ConsumerConfig<SofaGreeterTriple.IGreeter>();
        consumerConfig.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("tri://127.0.0.1:" + port);

        SofaGreeterTriple.IGreeter greeterBlockingStub = consumerConfig.refer();

        HelloRequest request = HelloRequest.newBuilder().setName("Hello world!").build();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        StreamObserver<HelloRequest> requestStreamObserver = greeterBlockingStub
            .sayHelloBinary(new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply value) {
                    Assert.assertEquals(value.getMessage(), request.getName());
                    countDownLatch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {
                }
            });
        requestStreamObserver.onNext(request);
        Assert.assertTrue(countDownLatch.await(20, TimeUnit.SECONDS));
        requestStreamObserver.onCompleted();
    }

    @Test
    //同步调用,直连
    public void testSyncTimeout() {

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server");

        int port = 50052;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig);

        providerConfig.export();

        ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig = new ConsumerConfig<SofaGreeterTriple.IGreeter>();
        consumerConfig.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("tri://127.0.0.1:" + port)
            .setTimeout(1);

        SofaGreeterTriple.IGreeter greeterBlockingStub = consumerConfig.refer();

        HelloRequest.DateTime dateTime = HelloRequest.DateTime.newBuilder().setDate("2018-12-28").setTime("11:13:00")
            .build();
        HelloReply reply = null;
        HelloRequest request = HelloRequest.newBuilder().setName("world").setDateTime(dateTime).build();

        boolean exp = false;
        try {
            reply = greeterBlockingStub.sayHello(request);
        } catch (SofaTimeOutException e) {
            exp = true;
        }
        Assert.assertTrue(exp);
    }

    @Test
    public void testExposeTwoUniqueId() {
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server1");

        int port = 50052;

        ServerConfig serverConfig = new ServerConfig()
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setPort(port);

        ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
                .setApplication(applicationConfig)
                .setUniqueId("abc")
                .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
                .setRef(new GreeterImpl())
                .setServer(serverConfig);

        providerConfig.export();

        ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig2 = new ProviderConfig<SofaGreeterTriple.IGreeter>()
                .setApplication(applicationConfig)
                .setUniqueId("abcd")
                .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
                .setRef(new GreeterImpl2())
                .setServer(serverConfig);
        providerConfig2.export();

        ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setUniqueId("abc")
                .setDirectUrl("tri://127.0.0.1:" + port);
        SofaGreeterTriple.IGreeter greeterBlockingStub = consumerConfig.refer();
        HelloRequest.DateTime dateTime = HelloRequest.DateTime.newBuilder().setDate("2018-12-28").setTime("11:13:00")
                .build();
        HelloRequest request = HelloRequest.newBuilder().setName("world").setDateTime(dateTime).build();
        Assert.assertEquals("Hello world", greeterBlockingStub.sayHello(request).getMessage());

        ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig2 = new ConsumerConfig<>();
        consumerConfig2.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setUniqueId("abcd")
                .setDirectUrl("tri://127.0.0.1:" + port);
        SofaGreeterTriple.IGreeter greeterBlockingStub2 = consumerConfig2.refer();
        Assert.assertEquals("Hello2 world", greeterBlockingStub2.sayHello(request).getMessage());
    }

    @Test
    public void testDefaultMessageSize() {
        boolean originDebugMode = RpcRunningState.isDebugMode();
        RpcRunningState.setDebugMode(false);
        try {
            int originInboundMessageSize = RpcConfigs.getIntValue(RpcOptions.TRANSPORT_GRPC_MAX_INBOUND_MESSAGE_SIZE);
            Assert.assertEquals(4194304, originInboundMessageSize);

            ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server1");
            int port = 50052;
            ServerConfig serverConfig = new ServerConfig()
                    .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                    .setPort(port);
            ProviderConfig<SampleService> providerConfig = new ProviderConfig<SampleService>()
                    .setApplication(applicationConfig)
                    .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                    .setInterfaceId(SampleService.class.getName())
                    .setRef(new SampleServiceImpl())
                    .setServer(serverConfig);
            providerConfig.export();

            ConsumerConfig<SampleService> consumerConfig = new ConsumerConfig<>();
            consumerConfig.setInterfaceId(SampleService.class.getName())
                    .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                    .setDirectUrl("tri://127.0.0.1:" + port);

            SampleService sampleService = consumerConfig.refer();
            String msg = buildMsg(1);
            try {
                sampleService.messageSize(msg, 5 * 1024);
                Assert.fail();
            } catch (Exception e) {
                Assert.assertTrue(e.getMessage().contains("gRPC message exceeds maximum size 4194304:"));
            }
            msg = buildMsg(5 * 1024);

            try {
                sampleService.messageSize(msg, 1);
                Assert.fail();
            } catch (Exception e) {
                // The client actively cancelled the request, resulting in the server returning a CANCELLED error.
                Assert.assertTrue(((StatusException) e.getCause()).getStatus().getCode().equals(Status.CANCELLED.getCode()));
            }
        } finally {
            RpcRunningState.setDebugMode(originDebugMode);
        }

    }

    @Test
    public void testSetInboundMessageSize() {
        boolean originDebugMode = RpcRunningState.isDebugMode();
        RpcRunningState.setDebugMode(false);
        int originInboundMessageSize = RpcConfigs.getIntValue(RpcOptions.TRANSPORT_GRPC_MAX_INBOUND_MESSAGE_SIZE);
        Assert.assertEquals(4194304, originInboundMessageSize);
        RpcConfigs.putValue(RpcOptions.TRANSPORT_GRPC_MAX_INBOUND_MESSAGE_SIZE, "8388608");
        try {
            ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server1");
            int port = 50052;
            ServerConfig serverConfig = new ServerConfig()
                    .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                    .setPort(port);

            ProviderConfig<SampleService> providerConfig = new ProviderConfig<SampleService>()
                    .setApplication(applicationConfig)
                    .setUniqueId("maxInbound")
                    .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                    .setInterfaceId(SampleService.class.getName())
                    .setRef(new SampleServiceImpl())
                    .setServer(serverConfig);
            providerConfig.export();

            ConsumerConfig<SampleService> consumerConfig = new ConsumerConfig<>();
            consumerConfig.setInterfaceId(SampleService.class.getName())
                    .setUniqueId("maxInbound")
                    .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                    .setDirectUrl("tri://127.0.0.1:" + port);
            SampleService sampleService2 = consumerConfig.refer();
            String msg = buildMsg(5 * 1024);
            try {
                sampleService2.messageSize(msg, 5 * 1024);
            } catch (Exception e) {
                Assert.fail();
            }
        } finally {
            RpcRunningState.setDebugMode(originDebugMode);
            RpcConfigs.putValue(RpcOptions.TRANSPORT_GRPC_MAX_INBOUND_MESSAGE_SIZE, originInboundMessageSize);
        }
    }

    private String buildMsg(int messageSize) {
        StringBuilder sb = new StringBuilder();
        // 1KB
        for (int i = 0; i < messageSize * 1024; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @BeforeClass
    public static void adBeforeClass() {
        RpcRunningState.setUnitTestMode(true);
    }

    @After
    public void afterMethod() {
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeAllContext();
        RpcInvokeContext.removeContext();
    }
}