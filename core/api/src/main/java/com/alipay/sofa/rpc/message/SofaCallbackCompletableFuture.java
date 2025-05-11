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

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.context.RpcInvokeContext;

import java.util.concurrent.CompletableFuture;

/**
 * A wrapper for CompletableFuture to provide a callback mechanism for
 * handling responses in a non-blocking way.
 *
 * @author ecstasoy
 */
public class SofaCompletableResponseFuture<T> extends CompletableFuture<T> {

    /**
     * Factory method to create an instance of SofaCompletableResponseFuture.
     *
     * @return SofaCompletableResponseFuture instance
     */
    public static <T> SofaCompletableResponseFuture<T> create() {
        SofaCompletableResponseFuture<T> future = new SofaCompletableResponseFuture<>();
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
     * Private constructor.
     */
    private SofaCompletableResponseFuture() {}

}
