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
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test for ProviderInvoker with CompletableFuture support
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ProviderInvokerTest {

    @Before
    public void setUp() {
        RpcInvokeContext.removeContext();
        RpcInternalContext.removeAllContext();
        RpcInternalContext.setContext(RpcInternalContext.getContext());
    }

    @After
    public void tearDown() {
        RpcInvokeContext.removeContext();
        RpcInternalContext.removeAllContext();
    }

    /**
     * Test service interface with CompletableFuture return type
     */
    public interface AsyncService {
        CompletableFuture<String> sayHelloAsync(String name);

        String sayHello(String name);
    }

    /**
     * Test service implementation with CompletableFuture
     */
    public static class AsyncServiceImpl implements AsyncService {
        @Override
        public CompletableFuture<String> sayHelloAsync(String name) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Hello, " + name;
            });
        }

        @Override
        public String sayHello(String name) {
            return "Hello, " + name;
        }
    }

    /**
     * Test ProviderInvoker with normal synchronous method
     */
    @Test
    public void testProviderInvokerSyncMethod() throws Exception {
        // Create provider config
        ProviderConfig<AsyncService> providerConfig = new ProviderConfig<>();
        providerConfig.setInterfaceId(AsyncService.class.getName());
        providerConfig.setRef(new AsyncServiceImpl());
        providerConfig.setUniqueId("test");

        // Create ProviderInvoker
        ProviderInvoker<AsyncService> providerInvoker = new ProviderInvoker<>(providerConfig);

        // Create request
        SofaRequest request = new SofaRequest();
        request.setMethodName("sayHello");
        request.setMethodArgSigs(new String[]{String.class.getName()});
        request.setMethodArgs(new Object[]{"World"});

        // Get the method
        Method method = AsyncService.class.getMethod("sayHello", String.class);
        request.setMethod(method);

        // Invoke
        SofaResponse response = providerInvoker.invoke(request);

        // Verify response
        Assert.assertNotNull(response);
        Assert.assertEquals("Hello, World", response.getAppResponse());
    }

    /**
     * Test ProviderInvoker with CompletableFuture return type
     */
    @Test
    public void testProviderInvokerCompletableFuture() throws Exception {
        // Create provider config
        ProviderConfig<AsyncService> providerConfig = new ProviderConfig<>();
        providerConfig.setInterfaceId(AsyncService.class.getName());
        providerConfig.setRef(new AsyncServiceImpl());
        providerConfig.setUniqueId("test");

        // Create ProviderInvoker
        ProviderInvoker<AsyncService> providerInvoker = new ProviderInvoker<>(providerConfig);

        // Create request
        SofaRequest request = new SofaRequest();
        request.setMethodName("sayHelloAsync");
        request.setMethodArgSigs(new String[]{String.class.getName()});
        request.setMethodArgs(new Object[]{"World"});

        // Get the method
        Method method = AsyncService.class.getMethod("sayHelloAsync", String.class);
        request.setMethod(method);

        // Invoke - should return null as response will be sent asynchronously
        SofaResponse response = providerInvoker.invoke(request);

        // For CompletableFuture, the response should be null (sent asynchronously)
        // The actual response will be sent through the AsyncContext callback
        Assert.assertNull(response);
    }

    /**
     * Test ProviderInvoker handles exception from CompletableFuture
     */
    public static class AsyncServiceImplWithException implements AsyncService {
        @Override
        public CompletableFuture<String> sayHelloAsync(String name) {
            return CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("Test exception");
            });
        }

        @Override
        public String sayHello(String name) {
            return "Hello, " + name;
        }
    }

    /**
     * Test ProviderInvoker with CompletableFuture that throws exception
     */
    @Test
    public void testProviderInvokerCompletableFutureWithException() throws Exception {
        // Create provider config
        ProviderConfig<AsyncService> providerConfig = new ProviderConfig<>();
        providerConfig.setInterfaceId(AsyncService.class.getName());
        providerConfig.setRef(new AsyncServiceImplWithException());
        providerConfig.setUniqueId("test");

        // Create ProviderInvoker
        ProviderInvoker<AsyncService> providerInvoker = new ProviderInvoker<>(providerConfig);

        // Create request
        SofaRequest request = new SofaRequest();
        request.setMethodName("sayHelloAsync");
        request.setMethodArgSigs(new String[]{String.class.getName()});
        request.setMethodArgs(new Object[]{"World"});

        // Get the method
        Method method = AsyncService.class.getMethod("sayHelloAsync", String.class);
        request.setMethod(method);

        // Invoke - should return null as response will be sent asynchronously
        SofaResponse response = providerInvoker.invoke(request);

        // For CompletableFuture, the response should be null (sent asynchronously)
        Assert.assertNull(response);
    }

    /**
     * Test ProviderInvoker handles method not found
     */
    @Test(expected = SofaRpcException.class)
    public void testProviderInvokerMethodNotFound() throws Exception {
        // Create provider config
        ProviderConfig<AsyncService> providerConfig = new ProviderConfig<>();
        providerConfig.setInterfaceId(AsyncService.class.getName());
        providerConfig.setRef(new AsyncServiceImpl());
        providerConfig.setUniqueId("test");

        // Create ProviderInvoker
        ProviderInvoker<AsyncService> providerInvoker = new ProviderInvoker<>(providerConfig);

        // Create request with non-existent method
        SofaRequest request = new SofaRequest();
        request.setMethodName("nonExistentMethod");
        request.setMethodArgSigs(new String[]{});
        request.setMethodArgs(new Object[]{});

        // Don't set method - this should trigger the error

        // Invoke - should throw exception
        providerInvoker.invoke(request);
    }
}