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

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.SofaStreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HelloServiceImpl implements HelloService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public SofaStreamObserver<ClientRequest> sayHelloBiStream(SofaStreamObserver<ServerResponse> sofaStreamObserver) {
        return new SofaStreamObserver<ClientRequest>() {
            private final AtomicInteger receivedCount = new AtomicInteger(0);

            @Override
            public void onNext(ClientRequest clientRequest) {
                LOGGER.info("bi stream req onMessage");
                if (clientRequest.getMsg().equals(CMD_TRIGGER_STREAM_FINISH)) {
                    sofaStreamObserver.onCompleted();
                } else if (clientRequest.getMsg().equals(CMD_TRIGGER_STREAM_ERROR)) {
                    sofaStreamObserver.onError(new RuntimeException(ERROR_MSG));
                } else {
                    receivedCount.incrementAndGet();
                    if (clientRequest instanceof ExtendClientRequest) {
                        sofaStreamObserver.onNext(new ExtendServerResponse(clientRequest.getMsg(), clientRequest
                            .getCount(), ((ExtendClientRequest) clientRequest).getExtendString()));
                    } else {
                        sofaStreamObserver.onNext(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount()));
                    }
                }
            }

            @Override
            public void onCompleted() {
                LOGGER.info("bi stream req onFinish");
                sofaStreamObserver.onCompleted();
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("bi stream req onException", throwable);
                sofaStreamObserver.onNext(new ServerResponse("Received exception:" + throwable.getMessage(), -2));
                sofaStreamObserver.onError(throwable);
            }
        };
    }

    @Override
    public void sayHelloServerStream(ClientRequest clientRequest, SofaStreamObserver<ServerResponse> sofaStreamObserver) {
        LOGGER.info("server stream req receive");
        if (clientRequest.getMsg().equals(CMD_TRIGGER_CONCURRENT_SERVER_SEND)) {
            // Concurrent server send: use multiple threads to send responses simultaneously
            int totalMessages = clientRequest.getCount();
            int threadCount = 4;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(totalMessages);
            for (int i = 0; i < totalMessages; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    sofaStreamObserver.onNext(new ServerResponse(CMD_TRIGGER_CONCURRENT_SERVER_SEND, index));
                    doneLatch.countDown();
                });
            }
            startLatch.countDown();
            try {
                doneLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor.shutdown();
            sofaStreamObserver.onCompleted();
        } else {
            sofaStreamObserver.onNext(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount()));
            sofaStreamObserver.onNext(new ExtendServerResponse(clientRequest.getMsg(), clientRequest.getCount() + 1,
                "extendString"));
            sofaStreamObserver.onNext(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount() + 2));
            sofaStreamObserver.onNext(new ExtendServerResponse(clientRequest.getMsg(), clientRequest.getCount() + 3,
                "extendString"));
            sofaStreamObserver.onNext(new ServerResponse(clientRequest.getMsg(), clientRequest.getCount() + 4));
            if (clientRequest.getMsg().equals(CMD_TRIGGER_STREAM_ERROR)) {
                sofaStreamObserver.onError(new RuntimeException(ERROR_MSG));
                sofaStreamObserver.onCompleted();
            } else {
                sofaStreamObserver.onCompleted();
            }
        }
    }

    @Override
    public ServerResponse sayHello(ClientRequest clientRequest) {
        return new ServerResponse(clientRequest.getMsg(), clientRequest.getCount());
    }

    @Override
    public void parentSayHelloServerStream(ClientRequest clientRequest,
                                           SofaStreamObserver<ServerResponse> sofaStreamObserver) {
        sayHelloServerStream(clientRequest, sofaStreamObserver);
    }

}
