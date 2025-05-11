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

import java.util.concurrent.CompletableFuture;

import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;

/**
 * A wrapper for CompletableFuture to provide a callback mechanism for
 * handling responses in a non-blocking way.
 *
 * @author ecstasoy
 */
public class SofaCallbackCompletableFuture<T> extends CompletableFuture<T> {

    /**
     * Private constructor.
     */
    private SofaCallbackCompletableFuture() {
    }

    /**
     * Indicates whether the future is attached to a callback.
     */
    private volatile boolean isAttached = false;

    /**
     * Factory method to create an instance of SofaCallbackCompletableFuture.
     *
     * @return SofaCallbackCompletableFuture instance
     */
    public static <T> SofaCallbackCompletableFuture<T> create() {
        SofaCallbackCompletableFuture<T> future = new SofaCallbackCompletableFuture<>();
        RpcInvokeContext context = RpcInvokeContext.getContext();
        if (context == null) {
            future.completeExceptionally(new IllegalStateException("RPC invoke context is not initialized"));
            return future;
        }
        // 防止竞态
        ResponseFuture<T> responseFuture = context.getFuture();
        if (responseFuture != null && responseFuture.isDone()) {
            try {
                T result = responseFuture.get();
                future.complete(result);
                return future;
            } catch (Exception e) {
                future.completeExceptionally(e);
                return future;
            }
        } else {
            // 如果原来注册了回调，直接用
            SofaResponseCallback<?> existing = context.getResponseCallback();
            context.setResponseCallback(new SofaResponseCallback<T>() {
                { future.isAttached = true; }
                @Override
                public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                    if (existing != null) {
                        existing.onAppResponse(appResponse, methodName, request);
                    }
                    @SuppressWarnings("unchecked")
                    T typedResponse = (T) appResponse;
                    future.complete(typedResponse);
                }

                @Override
                public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                    if (existing != null) {
                        existing.onAppException(throwable, methodName, request);
                    }
                    future.completeExceptionally(throwable);
                }

                @Override
                public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {
                    if (existing != null) {
                        existing.onSofaException(sofaException, methodName, request);
                    }
                    future.completeExceptionally(sofaException);
                }
            });
        }
        return future;
    }

    /**
     * Checks if the future is attached to a callback.
     *
     * @return true if attached, false otherwise
     */
    public boolean isAttached() {
        return isAttached;
    }

}
