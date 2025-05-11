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
        context.setResponseCallback(new SofaResponseCallback<T>() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                // Safe cast assumes the response type will match the expected type T
                @SuppressWarnings("unchecked")
                T typedResponse = (T) appResponse;
                future.complete(typedResponse);
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

}
