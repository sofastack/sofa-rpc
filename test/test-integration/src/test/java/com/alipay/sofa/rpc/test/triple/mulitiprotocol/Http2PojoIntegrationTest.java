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
package com.alipay.sofa.rpc.test.triple.mulitiprotocol;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.cache.RpcCacheManager;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.triple.stream.ClientRequest;
import com.alipay.sofa.rpc.test.triple.stream.HelloService;
import com.alipay.sofa.rpc.test.triple.stream.HelloServiceImpl;
import com.alipay.sofa.rpc.test.triple.stream.ServerResponse;
import com.alipay.sofa.rpc.transport.SofaStreamObserver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for pure HTTP/2 implementation with POJO services.
 * Tests HTTP/2 (gRPC) access to Triple services using custom Netty server
 * with POJO-based services (not protobuf).
 *
 * <p>This test verifies the pure HTTP/2 implementation that uses native Netty HTTP/2
 * while maintaining gRPC protocol compatibility for POJO services.
 *
 * <p>Note: This test class should be run in isolation due to shared global state in the RPC framework.
 * Run individually: mvn test -pl test/test-integration -Dtest=Http2PojoIntegrationTest
 */
public class Http2PojoIntegrationTest {

    private static final Logger                 LOGGER = LoggerFactory
                                                           .getLogger(Http2PojoIntegrationTest.class);

    private static final int                    PORT   = 50076;

    private static ProviderConfig<HelloService> providerConfig;
    private static ServerConfig                 serverConfig;
    private static ConsumerConfig<HelloService> consumerConfig;
    private static HelloService                 helloService;

    @BeforeClass
    public static void setUp() throws InterruptedException {
        RpcRunningState.setUnitTestMode(true);

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("http2-pojo-test");

        serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(PORT);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("triple.http1.enabled", "true");
        serverConfig.setParameters(parameters);

        providerConfig = new ProviderConfig<HelloService>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig);

        providerConfig.export();

        consumerConfig = new ConsumerConfig<HelloService>()
            .setApplication(applicationConfig)
            .setInterfaceId(HelloService.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("tri://127.0.0.1:" + PORT);

        helloService = consumerConfig.refer();

        Thread.sleep(10000);

        LOGGER.info("HTTP/2 POJO test server started on port: {}", PORT);
    }

    @AfterClass
    public static void tearDown() {
        if (consumerConfig != null) {
            consumerConfig.unRefer();
        }
        if (providerConfig != null) {
            providerConfig.unExport();
        }
        if (serverConfig != null) {
            serverConfig.destroy();
        }
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
        RpcCacheManager.clearAll();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Test HTTP/2 unary call with POJO service.
     */
    @Test
    public void testHttp2UnaryCall() throws Exception {
        LOGGER.info("=== Testing HTTP/2 Unary Call with POJO ===");

        try {
            ClientRequest request = new ClientRequest("http2-pojo-unary", 100);
            ServerResponse response = helloService.sayHello(request);

            LOGGER.info("HTTP/2 Unary Response: msg={}, count={}", response.getMsg(), response.getCount());

            Assert.assertNotNull("Response should not be null", response);
            Assert.assertEquals("Response msg should match", "http2-pojo-unary", response.getMsg());
            Assert.assertEquals("Response count should match", 100, response.getCount());

            LOGGER.info("HTTP/2 Unary call test PASSED");
        } catch (Exception e) {
            LOGGER.error("HTTP/2 Unary call test FAILED with exception: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Test HTTP/2 server streaming call with POJO service.
     */
    @Test
    public void testHttp2ServerStreamingCall() throws Exception {
        LOGGER.info("=== Testing HTTP/2 Server Streaming Call with POJO ===");

        try {


            ClientRequest request = new ClientRequest("http2-pojo-stream", 1);

            final List<ServerResponse> responses = new ArrayList<>();
            final CountDownLatch latch = new CountDownLatch(1);
            final Throwable[] error = new Throwable[1];

            helloService.sayHelloServerStream(request, new SofaStreamObserver<ServerResponse>() {
                @Override
                public void onNext(ServerResponse value) {
                    LOGGER.info("Server streaming response: msg={}, count={}", value.getMsg(), value.getCount());
                    responses.add(value);
                }

                @Override
                public void onError(Throwable throwable) {
                    LOGGER.error("Server streaming error: {}", throwable.getMessage());
                    error[0] = throwable;
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("Server streaming completed, total responses: {}", responses.size());
                    latch.countDown();
                }
            });

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            Assert.assertTrue("Server streaming should complete within timeout", completed);

            Assert.assertNull("Should not have error", error[0]);
            Assert.assertEquals("Should receive 5 responses", 5, responses.size());

            for (int i = 0; i < 5; i++) {
                ServerResponse resp = responses.get(i);
                Assert.assertEquals("Response msg should match", "http2-pojo-stream", resp.getMsg());
                Assert.assertEquals("Response count should be " + (i + 1), i + 1, resp.getCount());
            }

            LOGGER.info("HTTP/2 Server streaming call test PASSED");
        } catch (Exception e) {
            LOGGER.error("HTTP/2 Server streaming call test FAILED with exception: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Test HTTP/2 bidirectional streaming call with POJO service.
     */
    @Test
    public void testHttp2BiStreamingCall() throws Exception {
        LOGGER.info("=== Testing HTTP/2 Bidirectional Streaming Call with POJO ===");

        try {


            final List<ServerResponse> responses = new ArrayList<>();
            final CountDownLatch latch = new CountDownLatch(1);
            final Throwable[] error = new Throwable[1];

            SofaStreamObserver<ClientRequest> requestObserver = helloService.sayHelloBiStream(
                new SofaStreamObserver<ServerResponse>() {
                    @Override
                    public void onNext(ServerResponse value) {
                        LOGGER.info("Bi-stream response: msg={}, count={}", value.getMsg(), value.getCount());
                        responses.add(value);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOGGER.error("Bi-stream error: {}", throwable.getMessage());
                        error[0] = throwable;
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.info("Bi-stream completed, total responses: {}", responses.size());
                        latch.countDown();
                    }
                });

            for (int i = 1; i <= 3; i++) {
                ClientRequest request = new ClientRequest("bi-stream-" + i, i * 10);
                LOGGER.info("Sending bi-stream request: msg={}, count={}", request.getMsg(), request.getCount());
                requestObserver.onNext(request);
            }

            requestObserver.onCompleted();

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            Assert.assertTrue("Bi-stream should complete within timeout", completed);

            Assert.assertNull("Should not have error", error[0]);
            Assert.assertEquals("Should receive 3 responses", 3, responses.size());

            for (int i = 0; i < 3; i++) {
                ServerResponse resp = responses.get(i);
                Assert.assertEquals("Response msg should match", "bi-stream-" + (i + 1), resp.getMsg());
                Assert.assertEquals("Response count should match", (i + 1) * 10, resp.getCount());
            }

            LOGGER.info("HTTP/2 Bidirectional streaming call test PASSED");
        } catch (Exception e) {
            LOGGER.error("HTTP/2 Bidirectional streaming call test FAILED with exception: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Test HTTP/2 call with custom headers.
     */
    @Test
    public void testHttp2CallWithHeaders() throws Exception {
        LOGGER.info("=== Testing HTTP/2 Call with Custom Headers ===");

        try {


            RpcInvokeContext context = RpcInvokeContext.getContext();
            context.putRequestBaggage("custom-header", "test-value");
            context.putRequestBaggage("tracing-id", "12345");

            ClientRequest request = new ClientRequest("header-test", 200);
            ServerResponse response = helloService.sayHello(request);

            LOGGER.info("HTTP/2 Call with Headers Response: msg={}, count={}",
                response.getMsg(), response.getCount());

            Assert.assertNotNull("Response should not be null", response);
            Assert.assertEquals("Response msg should match", "header-test", response.getMsg());
            Assert.assertEquals("Response count should match", 200, response.getCount());

            LOGGER.info("HTTP/2 Call with headers test PASSED");
        } catch (Exception e) {
            LOGGER.error("HTTP/2 Call with headers test FAILED with exception: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Test multiple concurrent HTTP/2 calls.
     */
    @Test
    public void testConcurrentHttp2Calls() throws Exception {
        LOGGER.info("=== Testing Concurrent HTTP/2 Calls ===");

        try {


            int numCalls = 5;
            Thread[] threads = new Thread[numCalls];
            final boolean[] success = new boolean[numCalls];
            final Exception[] errors = new Exception[numCalls];

            for (int i = 0; i < numCalls; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        ClientRequest request = new ClientRequest("concurrent-" + index, index * 100);
                        ServerResponse response = helloService.sayHello(request);
                        success[index] = response != null &&
                            response.getMsg().equals("concurrent-" + index) &&
                            response.getCount() == index * 100;
                        LOGGER.info("Concurrent call {} completed: msg={}", index, response.getMsg());
                    } catch (Exception e) {
                        errors[index] = e;
                        LOGGER.error("Concurrent call {} failed: {}", index, e.getMessage());
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join(10000);
            }

            int successCount = 0;
            for (int i = 0; i < numCalls; i++) {
                if (success[i]) {
                    successCount++;
                } else if (errors[i] != null) {
                    LOGGER.error("Error in call {}: {}", i, errors[i].getMessage());
                }
            }

            LOGGER.info("Concurrent calls completed: {}/{}", successCount, numCalls);
            LOGGER.info("Concurrent HTTP/2 calls test completed");
        } catch (Exception e) {
            LOGGER.error("Concurrent HTTP/2 calls test FAILED with exception: {}", e.getMessage(), e);
            throw e;
        }
    }
}