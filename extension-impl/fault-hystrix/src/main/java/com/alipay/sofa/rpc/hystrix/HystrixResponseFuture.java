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
package com.alipay.sofa.rpc.hystrix;

import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.message.ResponseFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * the {@link Future}(from {@link rx.Observable}) wrapper that can be used as a {@link ResponseFuture}
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class HystrixResponseFuture implements ResponseFuture {

    private Future         delegate;

    private ResponseFuture responseFuture;

    public HystrixResponseFuture(Future delegate, ResponseFuture responseFuture) {
        this.delegate = delegate;
        this.responseFuture = responseFuture;
    }

    @Override
    public ResponseFuture addListener(SofaResponseCallback sofaResponseCallback) {
        if (responseFuture == null) {
            // TODO 因为熔断了，没有发出真实请求，无法获得真实实现类是如何处理的
            throw new UnsupportedOperationException("Not supported, Please use callback function");
        }
        return responseFuture.addListener(sofaResponseCallback);
    }

    @Override
    public ResponseFuture addListeners(List list) {
        if (responseFuture == null) {
            throw new UnsupportedOperationException("Not supported, Please use callback function");
        }
        return responseFuture.addListeners(list);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }
}
