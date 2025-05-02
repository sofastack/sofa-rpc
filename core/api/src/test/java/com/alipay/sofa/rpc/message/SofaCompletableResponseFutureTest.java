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
package com.alipay.sofa.rpc.message;

import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SofaCompletableResponseFutureTest {

    @Mock
    private ResponseFuture<String>                delegateFuture;

    @Mock
    private RequestBase                           request;

    private SofaCompletableResponseFuture<String> future;
    private RpcInvokeContext                      context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RpcInvokeContext.getContext();
        context.setFuture(delegateFuture);
        future = new SofaCompletableResponseFuture<>(delegateFuture);
    }

    @Test
    public void testDelegateFutureAccess() {
        // 验证是否能正确获取委托的Future
        assertSame(delegateFuture, future.getDelegate());
    }

    @Test
    public void testResponseCallbackRegistration() {
        // 验证回调是否正确注册
        assertNotNull(context.getResponseCallback());

        context.getResponseCallback().onAppResponse("test", "testMethod", request);
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    public void testMultipleCallbacks() {
        List<SofaResponseCallback> callbacks = new ArrayList<>();
        SofaResponseCallback callback1 = mock(SofaResponseCallback.class);
        SofaResponseCallback callback2 = mock(SofaResponseCallback.class);
        callbacks.add(callback1);
        callbacks.add(callback2);

        when(delegateFuture.addListeners(callbacks)).thenReturn(delegateFuture);

        future.addListeners(callbacks);

        // 验证回调转发
        verify(delegateFuture).addListeners(callbacks);

        context.getResponseCallback().onAppResponse("success", "testMethod", request);

        // 验证CompletableFuture完成
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    public void testErrorHandling() {
        // 应用异常
        RuntimeException appException = new RuntimeException("App error");
        context.getResponseCallback().onAppException(appException, "testMethod", request);
        assertTrue(future.isCompletedExceptionally());

        // SOFA异常
        SofaRpcException sofaException = new SofaRpcException("Sofa error");
        context.getResponseCallback().onSofaException(sofaException, "testMethod", request);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    public void testGetWithDelegate() throws Exception {
        when(delegateFuture.get()).thenReturn("delegated response");

        assertEquals("delegated response", future.get());
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());

        assertEquals("delegated response", future.get());
    }

    @Test
    public void testGetWithException() throws Exception {
        RuntimeException exception = new RuntimeException("Test error");
        when(delegateFuture.get()).thenThrow(exception);

        try {
            future.get();
            fail("Expected SofaRpcException");
        } catch (SofaRpcException e) {
            assertEquals(RpcErrorType.CLIENT_UNDECLARED_ERROR, e.getErrorType());
            assertSame(exception, e.getCause());
        }

        assertTrue(future.isDone());
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    public void testGetWithCallback() throws Exception {
        when(delegateFuture.get()).thenReturn("delegated response");

        context.getResponseCallback().onAppResponse("callback response", "testMethod", request);

        assertEquals("callback response", future.get());
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    public void testContextCleanup() {
        RpcInvokeContext.removeContext();
        assertNull(RpcInvokeContext.getContext().getResponseCallback());
    }

    @Test
    public void testChainOperations() throws Exception {
        // 测试链式操作
        CompletableFuture<String> transformed = future
            .thenApply(s -> s + " TRANSFORMED")
            .thenApply(String::toUpperCase)
            .thenApply(s -> s + " AND CHAINED");

        context.getResponseCallback().onAppResponse("original", "testMethod", request);

        assertEquals("ORIGINAL TRANSFORMED AND CHAINED", transformed.get());
    }

    @Test
    public void testChainWithExceptionHandling() throws Exception {
        // 测试链式操作中的异常处理
        CompletableFuture<String> recovered = future
            .thenCompose(s -> {
                CompletableFuture<String> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new RuntimeException("Error in transformation"));
                return failedFuture;
            })
            .exceptionally(ex -> "recovered from: " + ex.getMessage());

        context.getResponseCallback().onAppResponse("original", "testMethod", request);

        assertEquals("recovered from: java.lang.RuntimeException: Error in transformation", recovered.get());
    }

    @Test
    public void testChainWithMultipleStages() throws Exception {
        CompletableFuture<String> multiStage = future
            .thenApply(s -> s + " stage1")
            .thenApply(s -> s + " stage2")
            .thenApply(s -> s + " stage3");

        context.getResponseCallback().onAppResponse("start", "testMethod", request);

        assertEquals("start stage1 stage2 stage3", multiStage.get());
    }

    @Test
    public void testChainWithAsyncOperations() throws Exception {
        // 测试异步链式操作
        CompletableFuture<String> asyncChain = future
            .thenApplyAsync(s -> s + " async1")
            .thenApplyAsync(s -> s + " async2");

        context.getResponseCallback().onAppResponse("base", "testMethod", request);

        assertEquals("base async1 async2", asyncChain.get());
    }

    @Test
    public void testCancel() {
        ResponseFuture<String> delegate = mock(ResponseFuture.class);
        when(delegate.cancel(true)).thenReturn(true);
        when(delegate.isCancelled()).thenReturn(true);

        SofaCompletableResponseFuture<String> future = new SofaCompletableResponseFuture<>(delegate);

        assertTrue(future.cancel(true));
        verify(delegate).cancel(true);

        assertTrue(future.isCancelled());
        assertTrue(future.isDone());

        assertThrows(SofaRpcException.class, () -> future.get());
    }

    @Test
    public void testCancelFailed() {
        ResponseFuture<String> delegate = mock(ResponseFuture.class);
        when(delegate.cancel(true)).thenReturn(false);

        SofaCompletableResponseFuture<String> future = new SofaCompletableResponseFuture<>(delegate);

        assertFalse(future.cancel(true));
        verify(delegate).cancel(true);

        assertFalse(future.isCancelled());
    }

    @Test
    public void testIsCancelled() {
        ResponseFuture<String> delegate = mock(ResponseFuture.class);
        when(delegate.isCancelled()).thenReturn(true);

        SofaCompletableResponseFuture<String> future = new SofaCompletableResponseFuture<>(delegate);

        assertTrue(future.isCancelled());
        verify(delegate).isCancelled();
    }

    @Test
    public void testCancelAfterComplete() {
        ResponseFuture<String> delegate = mock(ResponseFuture.class);
        SofaCompletableResponseFuture<String> future = new SofaCompletableResponseFuture<>(delegate);

        future.complete("result");

        assertFalse(future.cancel(true));
        assertFalse(future.isCancelled());
    }

    @Test
    public void testCancelWithException() {
        ResponseFuture<String> delegate = mock(ResponseFuture.class);
        when(delegate.cancel(true)).thenThrow(new RuntimeException("cancel failed"));

        SofaCompletableResponseFuture<String> future = new SofaCompletableResponseFuture<>(delegate);

        assertThrows(RuntimeException.class, () -> future.cancel(true));
        assertFalse(future.isCancelled());
    }
}