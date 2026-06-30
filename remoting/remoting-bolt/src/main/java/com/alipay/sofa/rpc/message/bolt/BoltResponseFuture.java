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
package com.alipay.sofa.rpc.message.bolt;

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.message.AbstractResponseFuture;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Future of bolt.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltResponseFuture<V> extends AbstractResponseFuture<V> {

    /**
     * sofa请求
     */
    protected final SofaRequest request;

    /**
     * Listeners for async callback
     */
    protected final List<SofaResponseCallback> listeners = new CopyOnWriteArrayList<>();

    /**
     * 构造函数
     */
    public BoltResponseFuture(SofaRequest request, int timeout) {
        super(timeout);
        this.request = request;
    }

    @Override
    protected V getNow() throws ExecutionException {
        if (cause != null) {
            throw new ExecutionException(cause);
        } else {
            return (V) result;
        }
    }

    @Override
    protected void releaseIfNeed(Object result) {

    }

    @Override
    public BoltResponseFuture addListeners(List<SofaResponseCallback> list) {
        if (list != null) {
            listeners.addAll(list);
            // If already done, notify the new listeners
            if (isDone()) {
                notifyListeners();
            }
        }
        return this;
    }

    @Override
    public BoltResponseFuture addListener(SofaResponseCallback sofaResponseCallback) {
        if (sofaResponseCallback != null) {
            listeners.add(sofaResponseCallback);
            // If already done, notify the new listener immediately
            if (isDone()) {
                notifySingleListener(sofaResponseCallback);
            }
        }
        return this;
    }

    @Override
    public void notifyListeners() {
        for (SofaResponseCallback listener : listeners) {
            notifySingleListener(listener);
        }
        // Clear listeners after notifying
        listeners.clear();
    }

    /**
     * Notify a single listener based on the result
     */
    protected void notifySingleListener(SofaResponseCallback listener) {
        try {
            if (cause != null) {
                if (cause instanceof RuntimeException) {
                    listener.onAppException(cause, request.getMethodName(), request);
                } else {
                    listener.onAppException(cause, request.getMethodName(), request);
                }
            } else if (result instanceof SofaRpcException) {
                listener.onSofaException((SofaRpcException) result, request.getMethodName(), request);
            } else {
                listener.onAppResponse(result, request.getMethodName(), request);
            }
        } catch (Exception e) {
            // Log but don't propagate
        }
    }
}
