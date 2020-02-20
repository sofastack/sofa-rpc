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
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixEventType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@link HystrixCommand} for async requests
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class SofaAsyncHystrixCommand extends HystrixCommand implements SofaHystrixInvokable {

    private static final Logger               LOGGER               = LoggerFactory
                                                                       .getLogger(SofaAsyncHystrixCommand.class);

    private static final long                 DEFAULT_LOCK_TIMEOUT = 1000;

    private final FilterInvoker               invoker;

    private final SofaRequest                 request;

    private final RpcInternalContext          rpcInternalContext;

    private final RpcInvokeContext            rpcInvokeContext;

    private final CountDownLatch              lock                 = new CountDownLatch(1);

    private final List<SofaAsyncHystrixEvent> events               = new ArrayList<SofaAsyncHystrixEvent>();

    private SofaResponse                      sofaResponse;

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
            boolean finished = lock.await(getLockTimeout(), TimeUnit.MILLISECONDS);
            if (!finished && !this.isExecutionComplete()) {
                throw new SofaTimeOutException(
                    "Asynchronous execution timed out, please check Hystrix configuration. Events: " +
                        getExecutionEventsString());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        RpcInternalContext.getContext().setFuture(delegate);
        if (this.sofaResponse == null) {
            this.sofaResponse = buildEmptyResponse(request);
        }
        return this.sofaResponse;
    }

    @Override
    protected Object run() throws Exception {
        events.add(SofaAsyncHystrixEvent.EMIT);
        RpcInternalContext.setContext(rpcInternalContext);
        RpcInvokeContext.setContext(rpcInvokeContext);

        this.sofaResponse = invoker.invoke(request);
        ResponseFuture responseFuture = RpcInternalContext.getContext().getFuture();
        lock.countDown();
        events.add(SofaAsyncHystrixEvent.INVOKE_UNLOCKED);
        try {
            return responseFuture.get();
        } finally {
            events.add(SofaAsyncHystrixEvent.INVOKE_SUCCESS);
        }
    }

    @Override
    protected Object getFallback() {
        events.add(SofaAsyncHystrixEvent.FALLBACK_EMIT);
        if (lock.getCount() > 0) {
            // > 0 说明 run 方法没有执行，或是执行时立刻失败了
            this.sofaResponse = buildEmptyResponse(request);
            lock.countDown();
            events.add(SofaAsyncHystrixEvent.FALLBACK_UNLOCKED);
        }
        FallbackFactory fallbackFactory = SofaHystrixConfig.loadFallbackFactory((ConsumerConfig) invoker.getConfig());
        if (fallbackFactory == null) {
            return super.getFallback();
        }
        Object fallback = fallbackFactory.create(new FallbackContext(invoker, request, this.sofaResponse, this
            .getExecutionException()));
        if (fallback == null) {
            return super.getFallback();
        }
        try {
            return request.getMethod().invoke(fallback, request.getMethodArgs());
        } catch (IllegalAccessException e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_HYSTRIX_FALLBACK_FAIL), e);
        } catch (InvocationTargetException e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_HYSTRIX_FALLBACK_FAIL),
                e.getTargetException());
        } finally {
            events.add(SofaAsyncHystrixEvent.FALLBACK_SUCCESS);
        }
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

    private long getLockTimeout() {
        if (this.getProperties().executionTimeoutEnabled().get()) {
            return this.getProperties().executionTimeoutInMilliseconds().get();
        }
        return DEFAULT_LOCK_TIMEOUT;
    }

    private String getExecutionEventsString() {
        List<HystrixEventType> executionEvents = getExecutionEvents();
        if (executionEvents == null) {
            executionEvents = Collections.emptyList();
        }
        StringBuilder message = new StringBuilder("[");
        for (HystrixEventType executionEvent : executionEvents) {
            message.append(HystrixEventType.class.getSimpleName()).append("#").append(executionEvent.name())
                .append(",");
        }
        for (SofaAsyncHystrixEvent event : events) {
            message.append(SofaAsyncHystrixEvent.class.getSimpleName()).append("#").append(event.name()).append(",");
        }
        if (message.length() > 1) {
            message.deleteCharAt(message.length() - 1);
        }
        return message.append("]").toString();
    }
}
