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

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.message.ResponseFuture;
import rx.Observable;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Future;

public class SofaHystrixAsyncCommand extends SofaHystrixCommand {

    private SofaHystrixObservableCommand command;

    protected SofaHystrixAsyncCommand(FilterInvoker invoker, SofaRequest request) {
        super(invoker, request);
        this.command = new SofaHystrixObservableCommand(invoker, request);
    }

    @Override
    protected SofaResponse run() throws Exception {
        SofaResponse response = super.run();
        ResponseFuture responseFuture = RpcInternalContext.getContext().getFuture();
        command.setResponseFuture(responseFuture);
        responseFuture = command.toResponseFuture();
        RpcInternalContext.getContext().setFuture(responseFuture);
        return response;
    }

    protected SofaResponse getFallback(SofaResponse response, Throwable t) {
        FallbackFactory fallbackFactory = FallbackFactoryLoader.load(invoker.getConfig());
        if (fallbackFactory == null) {
            return super.getFallback();
        }
        final Object fallback = fallbackFactory.create(response, t);
        try {
            // 希望 fallback 异常能在第一次调用时抛出，所以没使用 Observable#fromCallable
            Object fallbackResult = request.getMethod().invoke(fallback, request.getMethodArgs());
            Future delegate = Observable.just(fallbackResult).toBlocking().toFuture();
            rpcInternalContext.setFuture(new HystrixResponseFuture(delegate));
            return new SofaResponse();
        } catch (IllegalAccessException e) {
            throw new SofaRpcRuntimeException("Hystrix fallback method invoke failed.", e);
        } catch (InvocationTargetException e) {
            throw new SofaRpcRuntimeException("Hystrix fallback method invoke failed.",
                e.getTargetException());
        }
    }

}
