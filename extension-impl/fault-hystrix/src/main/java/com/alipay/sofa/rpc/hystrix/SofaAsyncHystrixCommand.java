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

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.netflix.hystrix.HystrixCommand;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

/**
 * {@link HystrixCommand} for async requests
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class SofaAsyncHystrixCommand extends HystrixCommand implements SofaHystrixInvokable {

    private final static Logger   LOGGER = LoggerFactory.getLogger(SofaHystrixCommand.class);

    private FilterInvoker         invoker;

    private SofaRequest           request;

    private RpcInternalContext    rpcInternalContext;
    private RpcInvokeContext      rpcInvokeContext;

    private SofaResponse sofaResponse;

    private CountDownLatch lock = new CountDownLatch(1);

    public SofaAsyncHystrixCommand(FilterInvoker invoker, SofaRequest request) {
        super(SofaHystrixConfig.loadSetterFactory((ConsumerConfig) invoker.getConfig()).createSetter(invoker,
            request));
        this.rpcInternalContext = RpcInternalContext.peekContext();
        this.rpcInvokeContext = RpcInvokeContext.peekContext();
        this.invoker = invoker;
        this.request = request;
    }

    @Override
    public SofaResponse invoke() {
        if (isCircuitBreakerOpen() && LOGGER.isWarnEnabled(invoker.getConfig().getAppName())) {
            LOGGER.warnWithApp(invoker.getConfig().getAppName(), "Circuit Breaker is opened, method: {}#{}",
                invoker.getConfig().getInterfaceId(), request.getMethodName());
        }
        HystrixResponseFuture delegate = new HystrixResponseFuture(this.queue());
        try {
            lock.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        RpcInternalContext.getContext().setFuture(delegate);
        return this.sofaResponse;
    }

    @Override
    protected Object run() throws Exception {
        RpcInternalContext.setContext(rpcInternalContext);
        RpcInvokeContext.setContext(rpcInvokeContext);

        this.sofaResponse = invoker.invoke(request);
        ResponseFuture responseFuture = RpcInternalContext.getContext().getFuture();
        lock.countDown();
        return responseFuture.get();
    }

    // Copy from AbstractCluster#buildEmptyResponse
    private SofaResponse buildEmptyResponse(SofaRequest request) {
        SofaResponse response = new SofaResponse();
        Method method = request.getMethod();
        if (method != null) {
            response.setAppResponse(ClassUtils.getDefaultPrimitiveValue(method.getReturnType()));
        }
        return response;
    }

    @Override
    protected Object getFallback() {
        if (lock.getCount() > 0) {
            // > 0 说明 run 方法没有执行，或是执行时立刻失败了
            this.sofaResponse = buildEmptyResponse(request);
            lock.countDown();
        }
        FallbackFactory fallbackFactory = SofaHystrixConfig.loadFallbackFactory((ConsumerConfig) invoker.getConfig());
        if (fallbackFactory == null) {
            return super.getFallback();
        }
        Object fallback = fallbackFactory.create(null, this.getExecutionException());
        if (fallback == null) {
            return super.getFallback();
        }
        try {
            return request.getMethod().invoke(fallback, request.getMethodArgs());
        } catch (IllegalAccessException e) {
            throw new SofaRpcRuntimeException("Hystrix fallback method failed to execute.", e);
        } catch (InvocationTargetException e) {
            throw new SofaRpcRuntimeException("Hystrix fallback method failed to execute.",
                    e.getTargetException());
        }
    }
}
