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

import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.message.AbstractResponseFuture;

import java.util.List;
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
        throw new UnsupportedOperationException("Not supported, Please use callback function");
    }

    @Override
    public BoltResponseFuture addListener(SofaResponseCallback sofaResponseCallback) {
        throw new UnsupportedOperationException("Not supported, Please use callback function");
    }

    @Override
    public void notifyListeners() {
    }
}
