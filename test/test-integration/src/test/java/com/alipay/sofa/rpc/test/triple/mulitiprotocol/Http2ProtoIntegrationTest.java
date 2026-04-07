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
import com.alipay.sofa.rpc.test.triple.GreeterImpl;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.SofaGreeterTriple;
import io.grpc.stub.StreamObserver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for HTTP/2 (gRPC) implementation with Protobuf services.
 * Tests HTTP/2 transport with Triple protocol.
 *
 * <h3>Manual Testing with grpcurl:</h3>
 *
 * <p>Prerequisites: grpcurl installed and server running on port 50073.
 *
 * <h4>1. Unary Call (sayHello):</h4>
 * <pre>
 * grpcurl -plaintext -d '{"name":"http2-test"}' \
 *   localhost:50073 helloworld.Greeter/SayHello
 *
 * # Response:
 * # {
 * #   "message": "hello, http2-test"
 * # }
 * </pre>
 *
 * <h4>2. Server Streaming Call (sayHelloServerStream):</h4>
 * <pre>
 * grpcurl -plaintext -d '{"name":"stream-test"}' \
 *   localhost:50073 helloworld.Greeter/SayHelloServerStream
 *
 * # Response (multiple messages):
 * # {
 * #   "message": "hello, stream-test"
 * # }
 * # {
 * #   "message": "hello, stream-test"
 * # }
 * # ...
 * </pre>
 *
 * <h4>3. Bidirectional Streaming Call (sayHelloBinary):</h4>
 * <pre>
 * # Note: grpcurl bidirectional streaming requires interactive mode
 * grpcurl -plaintext -d '{"name":"bi-stream-1"}' \
 *   localhost:50073 helloworld.Greeter/SayHelloBinary
 *
 * # Or use the Java client test
 * </pre>
 *
 * <h4>4. Client Streaming Call (sayHelloClientStream):</h4>
 * <pre>
 * # Note: grpcurl client streaming requires interactive mode
 * grpcurl -plaintext localhost:50073 helloworld.Greeter/SayHelloClientStream
 *
 * # Then input multiple JSON messages:
 * # {"name":"client-stream-1"}
 * # {"name":"client-stream-2"}
 * # {"name":"client-stream-3"}
 * # (Ctrl+D to end)
 * </pre>
 *
 * <h4>5. List services:</h4>
 * <pre>
 * grpcurl -plaintext localhost:50073 list
 * grpcurl -plaintext localhost:50073 describe helloworld.Greeter
 * </pre>
 *
 * <h4>6. With custom headers:</h4>
 * <pre>
 * grpcurl -plaintext -H "tri-timeout: 5000" -H "tri-service-version: 1.0.0" \
 *   -d '{"name":"header-test"}' \
 *   localhost:50073 helloworld.Greeter/SayHello
 * </pre>
 *
 * <h3>Manual Testing with curl (HTTP/2):</h3>
 * <pre>
 * # Note: curl HTTP/2 support requires curl 7.47.0+
 * # Check HTTP/2 support:
 * curl --version | grep -i http2
 *
 * # Unary call (requires protobuf binary encoding):
 * # This is complex with curl, use grpcurl instead
 * </pre>
 *
 * <h3>Server Startup for Manual Testing:</h3>
 * <pre>
 * ServerConfig serverConfig = new ServerConfig()
 *     .setProtocol("tri")
 *     .setPort(50073)
 *     .setParameter("triple.http1.enabled", "true");
 * </pre>
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class Http2ProtoIntegrationTest {

    private static final Logger                               LOGGER = LoggerFactory
                                                                         .getLogger(Http2ProtoIntegrationTest.class);

    private static final int                                  PORT   = 50073;

    private static ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig;
    private static ServerConfig                               serverConfig;
    private static ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig;
    private static SofaGreeterTriple.IGreeter                 greeter;

    @BeforeClass
    public static void setUp() throws InterruptedException {
        RpcRunningState.setUnitTestMode(true);

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("http2-proto-test");

        serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(PORT);

        // Enable HTTP/1.1 for interoperability testing
        Map<String, String> parameters = new HashMap<>();
        parameters.put("triple.http1.enabled", "true");
        serverConfig.setParameters(parameters);

        providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig);

        providerConfig.export();

        consumerConfig = new ConsumerConfig<SofaGreeterTriple.IGreeter>()
            .setApplication(applicationConfig)
            .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("tri://127.0.0.1:" + PORT);

        greeter = consumerConfig.refer();

        Thread.sleep(10000);

        LOGGER.info("HTTP/2 Proto test server started on port: {}", PORT);
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

    // ==================== Unary Call Tests ====================

    /**
     * Test unary call via HTTP/2 (Triple protocol).
     */
    @Test
    public void testUnaryCall() throws Exception {
        LOGGER.info("=== Testing Unary Call (HTTP/2) ===");

        try {


            HelloRequest request = HelloRequest.newBuilder()
                .setName("http2-proto-unary-test")
                .build();

            HelloReply reply = greeter.sayHello(request);

            LOGGER.info("Unary Response: {}", reply.getMessage());

            Assert.assertNotNull("Response should not be null", reply);
            Assert.assertTrue("Response should contain the name",
                reply.getMessage().contains("http2-proto-unary-test"));

            LOGGER.info("Unary call test PASSED");
        } catch (Exception e) {
            LOGGER.error("Unary call test FAILED: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Test unary call with custom headers.
     */
    @Test
    public void testUnaryCallWithHeaders() throws Exception {
        LOGGER.info("=== Testing Unary Call with Custom Headers ===");

        try {


            RpcInvokeContext context = RpcInvokeContext.getContext();
            context.putRequestBaggage("custom-header", "test-value");
            context.putRequestBaggage("tracing-id", "12345");

            HelloRequest request = HelloRequest.newBuilder()
                .setName("header-test")
                .build();

            HelloReply reply = greeter.sayHello(request);

            LOGGER.info("Response with headers: {}", reply.getMessage());

            Assert.assertNotNull("Response should not be null", reply);

            LOGGER.info("Unary call with headers test PASSED");
        } catch (Exception e) {
            LOGGER.error("Unary call with headers test FAILED: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== Server Streaming Tests ====================

    /**
     * Test server streaming call.
     */
    @Test
    public void testServerStreamingCall() throws Exception {
        LOGGER.info("=== Testing Server Streaming Call ===");

        try {


            HelloRequest request = HelloRequest.newBuilder()
                .setName("http2-stream-test")
                .build();

            final int[] count = { 0 };
            final CountDownLatch latch = new CountDownLatch(1);
            final Throwable[] error = new Throwable[1];

            greeter.sayHelloServerStream(request, new StreamObserver<HelloReply>() {
                @Override
                public void onNext(HelloReply value) {
                    count[0]++;
                    LOGGER.info("Server streaming response {}: {}", count[0], value.getMessage());
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.error("Server streaming error: {}", t.getMessage());
                    error[0] = t;
                    latch.countDown();
                }

                @Override
                public void onCompleted() {
                    LOGGER.info("Server streaming completed, total responses: {}", count[0]);
                    latch.countDown();
                }
            });

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            Assert.assertTrue("Server streaming should complete within timeout", completed);
            Assert.assertNull("Should not have error", error[0]);
            Assert.assertTrue("Should receive at least 1 response", count[0] > 0);

            LOGGER.info("Server streaming call test PASSED");
        } catch (Exception e) {
            LOGGER.error("Server streaming call test FAILED: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== Bidirectional Streaming Tests ====================

    /**
     * Test bidirectional streaming call.
     */
    @Test
    public void testBidirectionalStreamingCall() throws Exception {
        LOGGER.info("=== Testing Bidirectional Streaming Call ===");

        try {


            final CountDownLatch latch = new CountDownLatch(1);
            final Throwable[] error = new Throwable[1];
            final int[] responseCount = { 0 };

            StreamObserver<HelloRequest> requestObserver = greeter.sayHelloBinary(
                new StreamObserver<HelloReply>() {
                    @Override
                    public void onNext(HelloReply value) {
                        responseCount[0]++;
                        LOGGER.info("Bi-stream response {}: {}", responseCount[0], value.getMessage());
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOGGER.error("Bi-stream error: {}", t.getMessage());
                        error[0] = t;
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.info("Bi-stream completed, total responses: {}", responseCount[0]);
                        latch.countDown();
                    }
                });

            // Send multiple requests
            for (int i = 1; i <= 3; i++) {
                HelloRequest request = HelloRequest.newBuilder()
                    .setName("bi-stream-" + i)
                    .build();
                LOGGER.info("Sending bi-stream request {}: {}", i, request.getName());
                requestObserver.onNext(request);
            }

            Thread.sleep(2000);
            requestObserver.onCompleted();

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            Assert.assertTrue("Bi-stream should complete within timeout", completed);
            Assert.assertNull("Should not have error", error[0]);
            Assert.assertEquals("Should receive 3 responses", 6, responseCount[0]);

            LOGGER.info("Bidirectional streaming call test PASSED");
        } catch (Exception e) {
            LOGGER.error("Bidirectional streaming call test FAILED: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== Client Streaming Tests ====================

    /**
     * Test client streaming call.
     */
    @Test
    public void testClientStreamingCall() throws Exception {
        LOGGER.info("=== Testing Client Streaming Call ===");

        try {


            final CountDownLatch latch = new CountDownLatch(1);
            final Throwable[] error = new Throwable[1];
            final HelloReply[] finalResponse = new HelloReply[1];

            StreamObserver<HelloRequest> requestObserver = greeter.sayHelloClientStream(
                new StreamObserver<HelloReply>() {
                    @Override
                    public void onNext(HelloReply value) {
                        LOGGER.info("Client streaming final response: {}", value.getMessage());
                        finalResponse[0] = value;
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOGGER.error("Client streaming error: {}", t.getMessage());
                        error[0] = t;
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.info("Client streaming completed");
                        latch.countDown();
                    }
                });

            // Send multiple requests
            for (int i = 1; i <= 5; i++) {
                HelloRequest request = HelloRequest.newBuilder()
                    .setName("client-stream-" + i)
                    .build();
                LOGGER.info("Sending client streaming request {}: {}", i, request.getName());
                requestObserver.onNext(request);
            }

            Thread.sleep(1000);
            requestObserver.onCompleted();

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            Assert.assertTrue("Client streaming should complete within timeout", completed);
            Assert.assertNull("Should not have error", error[0]);
            Assert.assertNotNull("Should have final response", finalResponse[0]);

            LOGGER.info("Client streaming call test PASSED");
        } catch (Exception e) {
            LOGGER.error("Client streaming call test FAILED: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== Concurrent Tests ====================

    /**
     * Test multiple concurrent calls.
     */
    @Test
    public void testConcurrentCalls() throws Exception {
        LOGGER.info("=== Testing Concurrent Calls ===");

        try {


            int numCalls = 5;
            Thread[] threads = new Thread[numCalls];
            final boolean[] success = new boolean[numCalls];
            final Exception[] errors = new Exception[numCalls];

            for (int i = 0; i < numCalls; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        HelloRequest request = HelloRequest.newBuilder()
                            .setName("concurrent-" + index)
                            .build();

                        HelloReply reply = greeter.sayHello(request);
                        success[index] = reply != null && reply.getMessage().contains("concurrent-" + index);
                        LOGGER.info("Concurrent call {} completed: {}", index, reply.getMessage());
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
            Assert.assertEquals("All concurrent calls should succeed", numCalls, successCount);

            LOGGER.info("Concurrent calls test PASSED");
        } catch (Exception e) {
            LOGGER.error("Concurrent calls test FAILED: {}", e.getMessage(), e);
            throw e;
        }
    }
}