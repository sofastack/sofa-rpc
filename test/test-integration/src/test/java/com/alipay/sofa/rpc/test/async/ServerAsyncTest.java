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
package com.alipay.sofa.rpc.test.async;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration test for server-side async capabilities.
 * Tests:
 * 1. CompletableFuture return type
 * 2. AsyncContext.startAsync() method
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerAsyncTest extends ActivelyDestroyTest {

    private static final int                                     SERVER_PORT = 22229;

    private static ServerConfig                                  serverConfig;
    private static ProviderConfig<ServerAsyncService>            providerConfig;
    private static ConsumerConfig<ServerAsyncService>            consumerConfig;
    private static ServerAsyncService                            service;

    private static ProviderConfig<ServerAsyncServiceWithContext> providerConfigWithContext;
    private static ConsumerConfig<ServerAsyncServiceWithContext> consumerConfigWithContext;
    private static ServerAsyncServiceWithContext                 serviceWithContext;

    @BeforeClass
    public static void init() {
        // Start single server for all services
        serverConfig = new ServerConfig()
            .setPort(SERVER_PORT)
            .setDaemon(false);

        // Export ServerAsyncService
        providerConfig = new ProviderConfig<ServerAsyncService>()
            .setInterfaceId(ServerAsyncService.class.getName())
            .setRef(new ServerAsyncServiceImpl())
            .setServer(serverConfig);
        providerConfig.export();

        // Export ServerAsyncServiceWithContext on same server
        providerConfigWithContext = new ProviderConfig<ServerAsyncServiceWithContext>()
            .setInterfaceId(ServerAsyncServiceWithContext.class.getName())
            .setRef(new ServerAsyncServiceWithContextImpl())
            .setServer(serverConfig);
        providerConfigWithContext.export();

        // Create consumer for ServerAsyncService
        consumerConfig = new ConsumerConfig<ServerAsyncService>()
            .setInterfaceId(ServerAsyncService.class.getName())
            .setTimeout(5000)
            .setDirectUrl("bolt://127.0.0.1:" + SERVER_PORT)
            .setProxy("jdk"); // Use JDK proxy for debugging
        service = consumerConfig.refer();

        // Create consumer for ServerAsyncServiceWithContext
        consumerConfigWithContext = new ConsumerConfig<ServerAsyncServiceWithContext>()
            .setInterfaceId(ServerAsyncServiceWithContext.class.getName())
            .setTimeout(5000)
            .setDirectUrl("bolt://127.0.0.1:" + SERVER_PORT)
            .setProxy("jdk"); // Use JDK proxy for debugging
        serviceWithContext = consumerConfigWithContext.refer();
    }

    @AfterClass
    public static void destroy() {
        if (service != null) {
            consumerConfig.unRefer();
        }
        if (providerConfig != null) {
            providerConfig.unExport();
        }
        if (serviceWithContext != null) {
            consumerConfigWithContext.unRefer();
        }
        if (providerConfigWithContext != null) {
            providerConfigWithContext.unExport();
        }
    }

    /**
     * Test synchronous method (baseline)
     */
    @Test
    public void testSyncMethod() {
        // Test sync method
        String result = service.sayHello("World");
        Assert.assertEquals("Hello, World", result);
        System.out.println("testSyncMethod passed");
    }

    /**
     * Test CompletableFuture return type
     * When service method returns CompletableFuture, the server handles it asynchronously
     * and the client receives the result through the async mechanism
     */
    @Test
    public void testCompletableFutureMethod() throws Exception {
        // Add delay to ensure previous async operations are completed
        Thread.sleep(1000);

        // Test async method returning CompletableFuture
        // Server returns null immediately, then sends response via AsyncContext
        // Client receives CompletableFuture and needs to get the result
        System.out.println("Calling sayHelloAsync...");
        CompletableFuture<String> future = service.sayHelloAsync("World");
        System.out.println("Got future: " + future);
        if (future == null) {
            throw new NullPointerException("Future is null!");
        }
        String result = future.get(5000, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Hello async, World", result);

        System.out.println("testCompletableFutureMethod passed");
    }

    /**
     * Test CompletableFuture with exception
     */
    @Test
    public void testCompletableFutureWithException() throws Exception {
        // Add delay to ensure previous async operations are completed
        Thread.sleep(800);
        // Test async method with exception - CompletableFuture.get() will throw ExecutionException
        try {
            CompletableFuture<String> future = service.sayHelloAsyncWithException("World");
            future.get(5000, TimeUnit.MILLISECONDS);
            Assert.fail("Expected exception");
        } catch (java.util.concurrent.ExecutionException e) {
            // Expected - the exception is wrapped in ExecutionException
            Assert.assertTrue(e.getCause().getMessage().contains("Async exception"));
        }

        System.out.println("testCompletableFutureWithException passed");
    }

    /**
     * Test AsyncContext.startAsync() method
     * The AsyncContext allows the service to control when to send the response asynchronously
     */
    @Test
    public void testAsyncContextMethod() throws Exception {
        // Add delay to ensure previous async operations are completed
        Thread.sleep(600);
        // Test async context method - the response is sent asynchronously via AsyncContext
        // The client should receive the response after the async processing completes
        String result = serviceWithContext.sayHelloWithAsyncContext("World");
        // The result should be the async response
        Assert.assertEquals("Hello async context, World", result);

        System.out.println("testAsyncContextMethod passed");
    }

    /**
     * Test callback invoke type with async service
     */
    @Test
    public void testCallbackWithCompletableFuture() throws Exception {
        // Create a new consumer with callback for this test
        ConsumerConfig<ServerAsyncService> callbackConsumerConfig = new ConsumerConfig<ServerAsyncService>()
            .setInterfaceId(ServerAsyncService.class.getName())
            .setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK)
            .setTimeout(5000)
            .setDirectUrl("bolt://127.0.0.1:" + SERVER_PORT);
        ServerAsyncService callbackService = callbackConsumerConfig.refer();

        try {
            final AtomicReference<String> resultRef = new AtomicReference<>();
            final CountDownLatch latch = new CountDownLatch(1);

            // Set callback
            RpcInvokeContext.getContext()
                .setResponseCallback(new SofaResponseCallback() {
                    @Override
                    public void onAppResponse(Object appResponse, String methodName,
                                              RequestBase request) {
                        resultRef.set((String) appResponse);
                        latch.countDown();
                    }

                    @Override
                    public void onAppException(Throwable throwable, String methodName,
                                               RequestBase request) {
                        latch.countDown();
                    }

                    @Override
                    public void onSofaException(SofaRpcException sofaException, String methodName,
                                                RequestBase request) {
                        latch.countDown();
                    }
                });

            // Call async method
            callbackService.sayHelloAsync("Callback");

            // Wait for callback
            boolean awaitResult = latch.await(5000, TimeUnit.MILLISECONDS);
            Assert.assertTrue("Callback timeout", awaitResult);

            Assert.assertEquals("Hello async, Callback", resultRef.get());
            System.out.println("testCallbackWithCompletableFuture passed");
        } finally {
            callbackConsumerConfig.unRefer();
        }
    }
}