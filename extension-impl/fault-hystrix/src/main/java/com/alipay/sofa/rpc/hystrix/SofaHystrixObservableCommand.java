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

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;
import rx.functions.Func1;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * {@link HystrixObservableCommand} for sync requests
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class SofaHystrixObservableCommand extends HystrixObservableCommand implements SofaHystrixInvokable {

    private final static Logger LOGGER = LoggerFactory.getLogger(SofaHystrixCommand.class);

    private FilterInvoker       invoker;

    private SofaRequest         request;

    private SofaResponse        sofaResponse;

    private ResponseFuture      responseFuture;

    public SofaHystrixObservableCommand(FilterInvoker invoker, SofaRequest request) {
        super(SofaHystrixConfig.loadSetterFactory((ConsumerConfig) invoker.getConfig()).createObservableSetter(invoker,
            request));
        this.invoker = invoker;
        this.request = request;
    }

    @Override
    protected Observable construct() {
        this.sofaResponse = invoker.invoke(request);
        this.responseFuture = RpcInternalContext.getContext().getFuture();
        return Observable.from(this.responseFuture);
    }

    @Override
    protected Observable resumeWithFallback() {
        return Observable.fromCallable(new Callable<FallbackFactory>() {
            @Override
            public FallbackFactory call() throws Exception {
                return SofaHystrixConfig.loadFallbackFactory((ConsumerConfig) invoker.getConfig());
            }
        }).flatMap(new Func1<FallbackFactory, Observable<?>>() {
            @Override
            public Observable<?> call(final FallbackFactory fallbackFactory) {
                if (fallbackFactory != null) {
                    return Observable.fromCallable(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            return fallbackFactory.create(null, getExecutionException());
                        }
                    }).map(new Func1<Object, Object>() {
                        @Override
                        public Object call(Object fallback) {
                            try {
                                return request.getMethod().invoke(fallback, request.getMethodArgs());
                            } catch (IllegalAccessException e) {
                                throw new SofaRpcRuntimeException("Hystrix fallback method failed to execute.", e);
                            } catch (InvocationTargetException e) {
                                throw new SofaRpcRuntimeException("Hystrix fallback method failed to execute.",
                                    e.getTargetException());
                            }
                        }
                    });
                } else {
                    return SofaHystrixObservableCommand.super.resumeWithFallback();
                }
            }
        });
    }

    @Override
    public SofaResponse invoke() {
        if (isCircuitBreakerOpen() && LOGGER.isWarnEnabled(invoker.getConfig().getAppName())) {
            LOGGER.warnWithApp(invoker.getConfig().getAppName(), "Circuit Breaker is opened, method: {}#{}",
                invoker.getConfig().getInterfaceId(), request.getMethodName());
        }
        Future delegate = this.toObservable().toBlocking().toFuture();
        RpcInternalContext.getContext().setFuture(new HystrixResponseFuture(delegate, this.responseFuture));
        if (this.sofaResponse == null && this.responseFuture == null) {
            // 没有执行 construct，熔断或是线程池拒绝
            return new SofaResponse();
        }
        return this.sofaResponse;
    }
}
