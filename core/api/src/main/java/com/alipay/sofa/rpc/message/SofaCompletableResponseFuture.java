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
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.context.RpcInternalContext;

import java.util.List;
import java.util.concurrent.*;

/**
 * A wrapper for CompletableFuture that implements ResponseFuture to provide
 * enhanced functionalities for handling asynchronous responses in a more convenient way.
 *
 * @author ecstasoy
 */
public class SofaCompletableResponseFuture<T> extends CompletableFuture<T> implements ResponseFuture<T> {

    private final ResponseFuture<T> delegate;

    /**
     * Constructor that creates a new SofaCompletableResponseFuture and
     * registers a callback to handle the response.
     *
     * @return A new SofaCompletableResponseFuture instance.
     */
    public static <T> SofaCompletableResponseFuture<T> create() {
        SofaCompletableResponseFuture<T> future = new SofaCompletableResponseFuture<>(null);
        RpcInternalContext.getContext().setFuture(future);
        RpcInvokeContext.getContext().setResponseCallback(new SofaResponseCallback<T>() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                future.complete((T) appResponse);
            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {
                future.completeExceptionally(sofaException);
            }
        });
        return future;
    }

    /**
     * Creates a new SofaCompletableResponseFuture with a delegate ResponseFuture.
     * This is used when you already have a ResponseFuture and want to wrap it.
     *
     * @param delegate The delegate ResponseFuture to wrap.
     * @return A new SofaCompletableResponseFuture instance.
     */
    public static <T> SofaCompletableResponseFuture<T> create(ResponseFuture<T> delegate) {
        return new SofaCompletableResponseFuture<>(delegate);
    }

    /**
     * Private constructor to force usage of factory methods.
     */
    private SofaCompletableResponseFuture(ResponseFuture<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns the delegate ResponseFuture.
     *
     * @return The delegate ResponseFuture.
     */
    public ResponseFuture<T> getDelegate() {
        return delegate;
    }

    /**
     * Adds listeners to the delegate ResponseFuture.
     *
     * @param callbacks The list of callbacks to add.
     * @return The current instance of SofaCompletableResponseFuture.
     */
    @Override
    public ResponseFuture<T> addListeners(List<SofaResponseCallback> callbacks) {
        return delegate.addListeners(callbacks);
    }

    /**
     * Adds a listener to the delegate ResponseFuture.
     *
     * @param callback The callback to add.
     * @return The current instance of SofaCompletableResponseFuture.
     */
    @Override
    public ResponseFuture<T> addListener(SofaResponseCallback callback) {
        return delegate.addListener(callback);
    }

    /**
     * Get the result of the RPC call, blocking until it is available.
     *
     * @return The result of the RPC call.
     * @throws SofaRpcException if the call fails or is interrupted.
     **/
    @Override
    public T get() {
        try {
            // 如果CompletableFuture已经完成，直接返回结果
            if (isDone()) {
                return super.join();
            }
            // 否则调用delegate的get方法
            T result = delegate.get();
            // 完成当前CompletableFuture
            if (!isDone()) {
                this.complete(result);
            }
            return result;
        } catch (InterruptedException e) {
            if (!isDone()) {
                this.completeExceptionally(e);
            }
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                "Get response interrupted, cause: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            if (!isDone()) {
                this.completeExceptionally(e);
            }
            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR,
                "Get response failed, cause: " + e.getMessage(), e);
        } catch (Exception e) {
            if (!isDone()) {
                this.completeExceptionally(e);
            }
            // 其他异常可能是客户端错误
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                "Get response failed, cause: " + e.getMessage(), e);
        }
    }

    /**
     * Get the result of the RPC call with a timeout.
     *
     * @param timeout The timeout duration.
     * @param unit    The time unit of the timeout duration.
     * @return The result of the RPC call.
     * @throws SofaRpcException if the call fails or is interrupted.
     **/
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            // 如果CompletableFuture已经完成，直接返回结果
            if (isDone()) {
                return super.join();
            }
            // 否则调用delegate的get方法
            T result = delegate.get(timeout, unit);
            // 完成当前CompletableFuture
            if (!isDone()) {
                this.complete(result);
            }
            return result;
        } catch (TimeoutException e) {
            if (!isDone()) {
                this.completeExceptionally(e);
            }
            throw new SofaRpcException(RpcErrorType.CLIENT_TIMEOUT,
                "Get response timeout, cause: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            if (!isDone()) {
                this.completeExceptionally(e);
            }
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                "Get response interrupted, cause: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            if (!isDone()) {
                this.completeExceptionally(e);
            }
            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR,
                "Get response failed, cause: " + e.getMessage(), e);
        } catch (Exception e) {
            if (!isDone()) {
                this.completeExceptionally(e);
            }
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                "Get response failed, cause: " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to cancel the execution of this task.
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     * task should be interrupted; otherwise
     * @return {@code true} if this task was cancelled
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        try {
            boolean cancelled = delegate.cancel(mayInterruptIfRunning);
            if (cancelled) {
                super.cancel(mayInterruptIfRunning);
            }
            return cancelled;
        } catch (Exception e) {
            if (!isDone()) {
                this.completeExceptionally(e);
            }
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                "Cancel response failed, cause: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the task was cancelled.
     *
     * @return {@code true} if this task was cancelled
     */
    @Override
    public boolean isCancelled() {
        try {
            return delegate.isCancelled();
        } catch (Exception e) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                "Check cancelled state failed, cause: " + e.getMessage(), e);
        }
    }
}
