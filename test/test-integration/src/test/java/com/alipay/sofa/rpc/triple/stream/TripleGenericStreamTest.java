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
package com.alipay.sofa.rpc.triple.stream;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.transport.StreamHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TripleGenericStreamTest {

    static final String          HELLO_MSG = "Hello, world!";
    ConsumerConfig<HelloService> consumerConfig;
    ProviderConfig<HelloService> providerConfig;
    HelloService                 helloServiceInst;

    ConsumerConfig<HelloService> consumerRefer() {
        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setProtocol("tri")
            .setDirectUrl("triple://127.0.0.1:12200");
        consumerConfig.refer();
        return consumerConfig;
    }

    ProviderConfig<HelloService> providerExport() {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("tri")
            .setPort(12200)
            .setDaemon(false);

        helloServiceInst = Mockito.spy(new HelloServiceImpl());

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(helloServiceInst)
            .setServer(serverConfig);

        providerConfig.export();
        return providerConfig;
    }

    @Before
    public void bootStrap() {
        RpcRunningState.setUnitTestMode(true);
        providerConfig = providerExport();
        consumerConfig = consumerRefer();
    }

    @After
    public void shutdown() {
        consumerConfig.unRefer();
        providerConfig.unExport();
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }

    public void testTripleBiStream(boolean endWithException) throws InterruptedException {

        int requestTimes = 5;
        CountDownLatch countDownLatch = new CountDownLatch(requestTimes + 1);

        AtomicBoolean receivedFinish = new AtomicBoolean(false);
        AtomicBoolean receivedException = new AtomicBoolean(false);

        HelloService helloServiceRef = consumerConfig.refer();

        StreamHandler<ClientRequest> streamHandler = helloServiceRef
                .sayHelloBiStream(new StreamHandler<ServerResponse>() {
                    final AtomicInteger requestCount = new AtomicInteger(0);

                    @Override
                    public void onMessage(ServerResponse message) {
                        Assert.assertEquals(requestCount.getAndIncrement(), message.getCount());
                        Assert.assertEquals(HELLO_MSG, message.getMsg());
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onFinish() {
                        receivedFinish.set(true);
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        Assert.assertTrue(throwable.getMessage().contains(HelloService.ERROR_MSG));
                        receivedException.set(true);
                        countDownLatch.countDown();
                    }
                });
        for (int k = 0; k < requestTimes; k++) {
            streamHandler.onMessage(new ClientRequest(HELLO_MSG, k));
        }
        if (!endWithException) {
            streamHandler.onMessage(new ClientRequest(HelloService.CMD_TRIGGER_STREAM_FINISH, -2));
            Assert.assertTrue(countDownLatch.await(20, TimeUnit.SECONDS));
            Assert.assertTrue(receivedFinish.get());
            streamHandler.onFinish();
            Assert.assertFalse(receivedException.get());
            Assert.assertThrows(Throwable.class, () -> streamHandler.onMessage(new ClientRequest("", 123)));
        } else {
            streamHandler.onMessage(new ClientRequest(HelloService.CMD_TRIGGER_STEAM_ERROR, -2));
            Assert.assertTrue(countDownLatch.await(20, TimeUnit.SECONDS));
            streamHandler.onException(new RuntimeException(HelloService.ERROR_MSG));
            Assert.assertThrows(Throwable.class,()->streamHandler.onMessage(new ClientRequest(HELLO_MSG,0)));
            Assert.assertFalse(receivedFinish.get());
            Assert.assertTrue(receivedException.get());
        }
        verify(helloServiceInst, times(1)).sayHelloBiStream(any());
    }

    @Test
    public void testTripleBiStreamException() throws InterruptedException {
        testTripleBiStream(true);
    }

    @Test
    public void testTripleBiStreamFinish() throws InterruptedException {
        testTripleBiStream(false);
    }

    public void testTripleServerStream(boolean endWithException) throws InterruptedException {
        HelloService helloServiceRef = consumerConfig.refer();
        AtomicInteger count = new AtomicInteger(0);
        int responseTimes = 5;
        CountDownLatch countDownLatch = new CountDownLatch(responseTimes + 1);
        AtomicBoolean responseFinished = new AtomicBoolean(false);
        AtomicBoolean responseException = new AtomicBoolean(false);

        helloServiceRef.sayHelloServerStream(new StreamHandler<ServerResponse>() {
            @Override
            public void onMessage(ServerResponse message) {
                Assert.assertEquals(endWithException ? HelloService.CMD_TRIGGER_STEAM_ERROR : HELLO_MSG,
                    message.getMsg());
                Assert.assertEquals(count.getAndIncrement(), message.getCount());
                countDownLatch.countDown();
            }

            @Override
            public void onFinish() {
                responseFinished.set(true);
                countDownLatch.countDown();
            }

            @Override
            public void onException(Throwable throwable) {
                Assert.assertTrue(throwable.getMessage().contains(HelloService.ERROR_MSG));
                responseException.set(true);
                countDownLatch.countDown();
            }
        }, new ClientRequest(endWithException ? HelloService.CMD_TRIGGER_STEAM_ERROR : HELLO_MSG, 0));

        Assert.assertTrue(countDownLatch.await(20, TimeUnit.SECONDS));
        if (endWithException) {
            Assert.assertTrue(responseException.get());
            Assert.assertFalse(responseFinished.get());
        } else {
            Assert.assertTrue(responseFinished.get());
            Assert.assertFalse(responseException.get());
        }
        Assert.assertEquals(responseTimes, count.get());
        verify(helloServiceInst, times(1)).sayHelloServerStream(any(), any());
    }

    @Test
    public void testTripleServerStreamFinish() throws InterruptedException {
        testTripleServerStream(false);
    }

    @Test
    public void testTripleServerStreamException() throws InterruptedException {
        testTripleServerStream(true);
    }
}
