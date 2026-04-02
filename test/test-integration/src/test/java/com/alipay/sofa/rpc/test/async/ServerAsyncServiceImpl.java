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

import com.alipay.sofa.rpc.context.AsyncContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service implementation for testing server-side async capabilities.
 * Implements both sync method and CompletableFuture return type.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ServerAsyncServiceImpl implements ServerAsyncService {

    private static final Logger          LOGGER   = LoggerFactory.getLogger(ServerAsyncServiceImpl.class);

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public String sayHello(String name) {
        LOGGER.info("sayHello called with name: {}", name);
        return "Hello, " + name;
    }

    @Override
    public CompletableFuture<String> sayHelloAsync(String name) {
        LOGGER.info("sayHelloAsync called with name: {}", name);
        // Return a CompletableFuture that will complete asynchronously
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate async business processing
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOGGER.info("sayHelloAsync completed for name: {}", name);
            return "Hello async, " + name;
        }, executor);
    }

    @Override
    public CompletableFuture<String> sayHelloAsyncWithException(String name) {
        LOGGER.info("sayHelloAsyncWithException called with name: {}", name);
        // Return a CompletableFuture that will complete with exception
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate async business processing
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Async exception for " + name);
        }, executor);
    }

    /**
     * Test method using AsyncContext for manual async control
     */
    public String sayHelloWithAsyncContext(String name) {
        LOGGER.info("sayHelloWithAsyncContext called with name: {}", name);
        // Start async context
        AsyncContext asyncContext = RpcInvokeContext.startAsync();

        // Submit async task
        executor.submit(() -> {
            try {
                // Simulate async business processing
                Thread.sleep(300);
                asyncContext.write("Hello async context, " + name);
            } catch (Exception e) {
                LOGGER.error("Error in async context", e);
                asyncContext.writeError(e);
            }
        });

        // Return null to indicate async handling
        return null;
    }
}