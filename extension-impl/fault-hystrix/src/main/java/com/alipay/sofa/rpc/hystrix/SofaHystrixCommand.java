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
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.netflix.hystrix.HystrixCommand;

import java.lang.reflect.InvocationTargetException;

/**
 * {@link HystrixCommand} for sync requests
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class SofaHystrixCommand extends HystrixCommand<SofaResponse> implements SofaHystrixInvokable {

    private static final Logger      LOGGER = LoggerFactory.getLogger(SofaHystrixCommand.class);

    private final RpcInternalContext rpcInternalContext;

    private final RpcInvokeContext   rpcInvokeContext;

    private final FilterInvoker      invoker;
    private final SofaRequest        request;

    public SofaHystrixCommand(FilterInvoker invoker, SofaRequest request) {
        super(SofaHystrixConfig.loadSetterFactory((ConsumerConfig) invoker.getConfig()).createSetter(invoker, request));
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
        return execute();
    }

    @Override
    protected SofaResponse run() throws Exception {
        RpcInternalContext.setContext(rpcInternalContext);
        RpcInvokeContext.setContext(rpcInvokeContext);

        SofaResponse sofaResponse = invoker.invoke(request);
        if (!sofaResponse.isError()) {
            return sofaResponse;
        }
        return getFallback(sofaResponse, null);
    }

    @Override
    protected SofaResponse getFallback() {
        return getFallback(null, getExecutionException());
    }

    protected SofaResponse getFallback(SofaResponse response, Throwable t) {
        FallbackFactory fallbackFactory = SofaHystrixConfig.loadFallbackFactory((ConsumerConfig) invoker.getConfig());
        if (fallbackFactory == null) {
            return super.getFallback();
        }
        Object fallback = fallbackFactory.create(new FallbackContext(invoker, request, response, t));
        if (fallback == null) {
            return super.getFallback();
        }
        try {
            Object fallbackResult = request.getMethod().invoke(fallback, request.getMethodArgs());
            SofaResponse actualResponse = new SofaResponse();
            actualResponse.setAppResponse(fallbackResult);
            return actualResponse;
        } catch (IllegalAccessException e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_HYSTRIX_FALLBACK_FAIL), e);
        } catch (InvocationTargetException e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_HYSTRIX_FALLBACK_FAIL),
                e.getTargetException());
        }
    }

}
