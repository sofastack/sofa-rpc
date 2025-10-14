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
import com.alipay.sofa.rpc.transport.SofaStreamObserver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TripleGenericStreamTest {

    private static final Logger                 LOGGER    = LoggerFactory.getLogger(TripleGenericStreamTest.class);
    private static final String                 HELLO_MSG = "Hello, world!";
    private static ConsumerConfig<HelloService> consumerConfig;
    private static ProviderConfig<HelloService> providerConfig;
    private static HelloService                 helloServiceInst;
    private static HelloService                 helloServiceRef;

    @BeforeClass
    public static void beforeClass() throws InterruptedException {
        RpcRunningState.setUnitTestMode(true);
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("tri")
            .setPort(50066)
            .setDaemon(false);

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("triple-server");
        helloServiceInst = Mockito.spy(new HelloServiceImpl());
        providerConfig = new ProviderConfig<HelloService>()
            .setApplication(applicationConfig)
            .setInterfaceId(HelloService.class.getName())
            .setRef(helloServiceInst)
            .setServer(serverConfig);
        providerConfig.export();

        ApplicationConfig consumerApp = new ApplicationConfig().setAppName("triple-client");
        consumerConfig = new ConsumerConfig<HelloService>()
            .setApplication(consumerApp)
            .setInterfaceId(HelloService.class.getName())
            .setProtocol("tri")
            .setTimeout(1000000)
            .setDirectUrl("triple://127.0.0.1:50066?appName=triple-server");
        helloServiceRef = consumerConfig.refer();
    }

    @AfterClass
    public static void afterClass() {
        consumerConfig.unRefer();
        providerConfig.unExport();
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }

    @Test
    public void testTripleParentCall() throws InterruptedException {
        ClientRequest clientRequest = new ClientRequest("hello world", 5);
        ServerResponse serverResponse = helloServiceRef.sayHello(clientRequest);
        Assert.assertEquals("hello world", serverResponse.getMsg());

        CountDownLatch countDownLatch = new CountDownLatch(6);
        AtomicBoolean receivedFinish = new AtomicBoolean(false);
        List<ServerResponse> list = new ArrayList<>();
        helloServiceRef.parentSayHelloServerStream(clientRequest, new SofaStreamObserver<ServerResponse>() {
            @Override
            public void onNext(ServerResponse message) {
                list.add(message);
                countDownLatch.countDown();
            }

            @Override
            public void onCompleted() {
                receivedFinish.set(true);
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                countDownLatch.countDown();
            }
        });

        Assert.assertTrue(countDownLatch.await(20, TimeUnit.SECONDS));
        Assert.assertEquals(5, list.size());
        Assert.assertTrue(receivedFinish.get());
    }

    @Test
    public void testTripleBiStreamFinish() throws InterruptedException {
        testTripleBiStream(false);
    }

    @Test
    public void testTripleBiStreamException() throws InterruptedException {
        testTripleBiStream(true);
    }

    public void testTripleBiStream(boolean endWithException) throws InterruptedException {
        int requestTimes = 5;
        CountDownLatch countDownLatch = new CountDownLatch(requestTimes + 1);

        AtomicBoolean receivedFinish = new AtomicBoolean(false);
        AtomicBoolean receivedException = new AtomicBoolean(false);

        List<ServerResponse> serverResponseList = new ArrayList<>();
        SofaStreamObserver<ClientRequest> sofaStreamObserver = helloServiceRef
                .sayHelloBiStream(new SofaStreamObserver<ServerResponse>() {
                    final AtomicInteger requestCount = new AtomicInteger(0);

                    @Override
                    public void onNext(ServerResponse message) {
                        LOGGER.info("bi stream resp onMessage");
                        Assert.assertEquals(requestCount.getAndIncrement(), message.getCount());
                        Assert.assertEquals(HELLO_MSG, message.getMsg());
                        serverResponseList.add(message);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.info("bi stream resp onFinish");
                        receivedFinish.set(true);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOGGER.error("bi stream resp onException", throwable);
                        Assert.assertTrue(throwable.getMessage().contains(HelloService.ERROR_MSG));
                        receivedException.set(true);
                        countDownLatch.countDown();
                    }
                });

        for (int k = 0; k < requestTimes; k++) {
            if (k % 2 == 0) {
                sofaStreamObserver.onNext(new ClientRequest(HELLO_MSG, k));
            } else {
                sofaStreamObserver.onNext(new ExtendClientRequest(HELLO_MSG, k, "testExtendString"));
            }
        }
        if (!endWithException) {
            sofaStreamObserver.onNext(new ClientRequest(HelloService.CMD_TRIGGER_STREAM_FINISH, -2));
            Assert.assertTrue(countDownLatch.await(20, TimeUnit.SECONDS));
            Assert.assertTrue(receivedFinish.get());
            sofaStreamObserver.onCompleted();
            assertServerResponseType(serverResponseList);
            Assert.assertFalse(receivedException.get());
            Assert.assertThrows(Throwable.class, () -> sofaStreamObserver.onNext(new ClientRequest("", 123)));
        } else {
            sofaStreamObserver.onNext(new ClientRequest(HelloService.CMD_TRIGGER_STREAM_ERROR, -2));
            Assert.assertTrue(countDownLatch.await(20, TimeUnit.SECONDS));
            sofaStreamObserver.onError(new RuntimeException(HelloService.ERROR_MSG));
            Assert.assertThrows(Throwable.class, () -> sofaStreamObserver.onNext(new ClientRequest(HELLO_MSG, 0)));
            Assert.assertFalse(receivedFinish.get());
            Assert.assertTrue(receivedException.get());
        }
        verify(helloServiceInst, times(1)).sayHelloBiStream(any());
    }

    @Test
    public void testTripleServerStreamFinish() throws InterruptedException {
        testTripleServerStream(false);
    }

    @Test
    public void testTripleServerStreamException() throws InterruptedException {
        testTripleServerStream(true);
    }

    public void testTripleServerStream(boolean endWithException) throws InterruptedException {
        reset(helloServiceInst);
        AtomicInteger count = new AtomicInteger(0);
        int responseTimes = 5;
        CountDownLatch countDownLatch = new CountDownLatch(responseTimes + 1);
        AtomicBoolean responseFinished = new AtomicBoolean(false);
        AtomicBoolean responseException = new AtomicBoolean(false);

        List<ServerResponse> serverResponseList = new ArrayList<>();
        helloServiceRef.sayHelloServerStream(new ClientRequest(endWithException ? HelloService.CMD_TRIGGER_STREAM_ERROR : HELLO_MSG, 0), new SofaStreamObserver<ServerResponse>() {
            @Override
            public void onNext(ServerResponse message) {
                Assert.assertEquals(endWithException ? HelloService.CMD_TRIGGER_STREAM_ERROR : HELLO_MSG,
                        message.getMsg());
                Assert.assertEquals(count.getAndIncrement(), message.getCount());
                serverResponseList.add(message);
                countDownLatch.countDown();
            }

            @Override
            public void onCompleted() {
                responseFinished.set(true);
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                Assert.assertTrue(throwable.getMessage().contains(HelloService.ERROR_MSG));
                responseException.set(true);
                countDownLatch.countDown();
            }
        });

        Assert.assertTrue(countDownLatch.await(20, TimeUnit.SECONDS));
        if (endWithException) {
            Assert.assertTrue(responseException.get());
            Assert.assertFalse(responseFinished.get());
            assertServerResponseType(serverResponseList);
        } else {
            Assert.assertTrue(responseFinished.get());
            Assert.assertFalse(responseException.get());
        }
        Assert.assertEquals(responseTimes, count.get());
        verify(helloServiceInst, times(1)).sayHelloServerStream(any(), any());
    }

    private void assertServerResponseType(List<ServerResponse> serverResponseList) {
        for (int i = 0; i < serverResponseList.size(); i++) {
            if (i % 2 != 0) {
                Assert.assertTrue(serverResponseList.get(i) instanceof ExtendServerResponse);
            }
        }
    }

}
