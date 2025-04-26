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
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletableResponseFuture<T> extends CompletableFuture<T> implements ResponseFuture<T> {

    private final ResponseFuture<T> delegate;

    public CompletableResponseFuture(ResponseFuture<T> delegate) {
        this.delegate = delegate;

        RpcInvokeContext.getContext().setResponseCallback(new SofaResponseCallback<T>() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                CompletableResponseFuture.this.complete((T) appResponse);
            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                CompletableResponseFuture.this.completeExceptionally(throwable);
            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {
                CompletableResponseFuture.this.completeExceptionally(sofaException);
            }
        });
    }

    public ResponseFuture<T> getDelegate() {
        return delegate;
    }

    @Override
    public ResponseFuture<T> addListeners(List<SofaResponseCallback> callbacks) {
        return delegate.addListeners(callbacks);
    }

    @Override
    public ResponseFuture<T> addListener(SofaResponseCallback callback) {
        return delegate.addListener(callback);
    }

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
            this.complete(result);
            return result;
        } catch (Exception e) {
            this.completeExceptionally(e);
            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR,
                "Get response failed, cause: " + e.getMessage(), e);
        }
    }

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
            this.complete(result);
            return result;
        } catch (TimeoutException e) {
            this.completeExceptionally(e);
            throw e;
        } catch (Exception e) {
            this.completeExceptionally(e);
            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR,
                "Get response failed, cause: " + e.getMessage(), e);
        }
    }
}
