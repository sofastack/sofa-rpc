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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.netflix.hystrix.HystrixCommand;

/**
 * The fallback context
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class FallbackContext {

    /**
     * The invoker
     */
    private FilterInvoker invoker;

    /**
     * The request
     */
    private SofaRequest   request;

    /**
     * The response, only {@link RpcConstants#INVOKER_TYPE_SYNC} and {@link SofaResponse#isError()} is true
     */
    private SofaResponse  response;

    /**
     * The exception, from {@link HystrixCommand#getExecutionException()}
     */
    private Throwable     exception;

    public FallbackContext() {
    }

    public FallbackContext(FilterInvoker invoker, SofaRequest request, SofaResponse response, Throwable exception) {
        this.invoker = invoker;
        this.request = request;
        this.response = response;
        this.exception = exception;
    }

    public FilterInvoker getInvoker() {
        return invoker;
    }

    public void setInvoker(FilterInvoker invoker) {
        this.invoker = invoker;
    }

    public SofaRequest getRequest() {
        return request;
    }

    public void setRequest(SofaRequest request) {
        this.request = request;
    }

    public SofaResponse getResponse() {
        return response;
    }

    public void setResponse(SofaResponse response) {
        this.response = response;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}