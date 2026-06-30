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
package com.alipay.sofa.rpc.context;

import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Test for AsyncContext and DefaultAsyncContext
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class AsyncContextTest {

    @Before
    public void setUp() {
        RpcInvokeContext.removeContext();
        RpcInternalContext.removeAllContext();
    }

    @After
    public void tearDown() {
        RpcInvokeContext.removeContext();
        RpcInternalContext.removeAllContext();
    }

    /**
     * Test AsyncContext interface exists and has required methods
     */
    @Test
    public void testAsyncContextInterface() {
        // Verify AsyncContext interface exists
        Assert.assertNotNull(AsyncContext.class);

        // Verify interface has required methods
        try {
            AsyncContext.class.getMethod("write", Object.class);
            AsyncContext.class.getMethod("writeError", Throwable.class);
            AsyncContext.class.getMethod("isSent");
        } catch (NoSuchMethodException e) {
            Assert.fail("AsyncContext interface is missing required methods");
        }
    }

    /**
     * Test ServerAsyncResponseSender interface exists and has required methods
     */
    @Test
    public void testServerAsyncResponseSenderInterface() {
        // Verify ServerAsyncResponseSender interface exists
        Assert.assertNotNull(ServerAsyncResponseSender.class);

        // Verify interface has required methods
        try {
            ServerAsyncResponseSender.class.getMethod("sendResponse", SofaResponse.class);
            ServerAsyncResponseSender.class.getMethod("isSent");
        } catch (NoSuchMethodException e) {
            Assert.fail("ServerAsyncResponseSender interface is missing required methods");
        }
    }

    /**
     * Test DefaultAsyncContext write method
     */
    @Test
    public void testDefaultAsyncContextWrite() {
        // Create a mock ServerAsyncResponseSender
        AtomicReference<SofaResponse> capturedResponse = new AtomicReference<>();
        ServerAsyncResponseSender mockSender = new ServerAsyncResponseSender() {
            @Override
            public void sendResponse(SofaResponse response) {
                capturedResponse.set(response);
            }

            @Override
            public boolean isSent() {
                return false;
            }
        };

        // Create DefaultAsyncContext
        SofaResponse sofaResponse = new SofaResponse();
        DefaultAsyncContext asyncContext = new DefaultAsyncContext(mockSender, sofaResponse);

        // Test write method
        asyncContext.write("test response");

        // Verify response was sent
        Assert.assertNotNull(capturedResponse.get());
        Assert.assertEquals("test response", capturedResponse.get().getAppResponse());
    }

    /**
     * Test DefaultAsyncContext writeError method
     */
    @Test
    public void testDefaultAsyncContextWriteError() {
        // Create a mock ServerAsyncResponseSender
        AtomicReference<SofaResponse> capturedResponse = new AtomicReference<>();
        ServerAsyncResponseSender mockSender = new ServerAsyncResponseSender() {
            @Override
            public void sendResponse(SofaResponse response) {
                capturedResponse.set(response);
            }

            @Override
            public boolean isSent() {
                return false;
            }
        };

        // Create DefaultAsyncContext
        SofaResponse sofaResponse = new SofaResponse();
        DefaultAsyncContext asyncContext = new DefaultAsyncContext(mockSender, sofaResponse);

        // Test writeError method
        RuntimeException testException = new RuntimeException("test error");
        asyncContext.writeError(testException);

        // Verify error was sent
        Assert.assertNotNull(capturedResponse.get());
        Assert.assertEquals(testException, capturedResponse.get().getAppResponse());
    }

    /**
     * Test DefaultAsyncContext prevents double write
     */
    @Test(expected = IllegalStateException.class)
    public void testDefaultAsyncContextPreventDoubleWrite() {
        // Create a mock ServerAsyncResponseSender
        ServerAsyncResponseSender mockSender = new ServerAsyncResponseSender() {
            @Override
            public void sendResponse(SofaResponse response) {
            }

            @Override
            public boolean isSent() {
                return false;
            }
        };

        // Create DefaultAsyncContext
        SofaResponse sofaResponse = new SofaResponse();
        DefaultAsyncContext asyncContext = new DefaultAsyncContext(mockSender, sofaResponse);

        // First write should succeed
        asyncContext.write("first response");

        // Second write should throw exception
        asyncContext.write("second response");
    }

    /**
     * Test DefaultAsyncContext isSent method
     */
    @Test
    public void testDefaultAsyncContextIsSent() {
        // Create a mock ServerAsyncResponseSender
        ServerAsyncResponseSender mockSender = new ServerAsyncResponseSender() {
            @Override
            public void sendResponse(SofaResponse response) {
            }

            @Override
            public boolean isSent() {
                return false;
            }
        };

        // Create DefaultAsyncContext
        SofaResponse sofaResponse = new SofaResponse();
        DefaultAsyncContext asyncContext = new DefaultAsyncContext(mockSender, sofaResponse);

        // Initially should not be sent
        Assert.assertFalse(asyncContext.isSent());

        // After write, should be sent
        asyncContext.write("test response");
        Assert.assertTrue(asyncContext.isSent());
    }

    /**
     * Test RpcInvokeContext isAsyncStarted method
     */
    @Test
    public void testRpcInvokeContextIsAsyncStarted() {
        // Initially should return false
        Assert.assertFalse(RpcInvokeContext.isAsyncStarted());

        // Set async started flag through reflection
        RpcInvokeContext context = RpcInvokeContext.getContext();
        try {
            java.lang.reflect.Field asyncStartedField = RpcInvokeContext.class.getDeclaredField("asyncStarted");
            asyncStartedField.setAccessible(true);
            asyncStartedField.set(context, true);
        } catch (Exception e) {
            Assert.fail("Failed to set asyncStarted field: " + e.getMessage());
        }

        // Now should return true
        Assert.assertTrue(RpcInvokeContext.isAsyncStarted());
    }
}