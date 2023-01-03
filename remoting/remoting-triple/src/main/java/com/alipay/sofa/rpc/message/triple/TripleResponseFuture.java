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
package com.alipay.sofa.rpc.message.triple;

import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.message.AbstractResponseFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author gujin
 * Created on 2022/9/27 5:00 下午
 */
public class TripleResponseFuture<V> extends AbstractResponseFuture<V> {

    /**
     * sofa请求
     */
    protected final SofaRequest request;

    /**
     * 构造函数
     */
    public TripleResponseFuture(SofaRequest request, int timeout) {
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
    public TripleResponseFuture addListeners(List<SofaResponseCallback> list) {
        throw new UnsupportedOperationException("Not supported, Please use callback function");
    }

    @Override
    public TripleResponseFuture addListener(SofaResponseCallback sofaResponseCallback) {
        throw new UnsupportedOperationException("Not supported, Please use callback function");
    }

    @Override
    public void notifyListeners() {
    }
}
