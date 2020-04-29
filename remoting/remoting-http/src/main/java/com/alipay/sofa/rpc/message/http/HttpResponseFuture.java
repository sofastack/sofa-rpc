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
package com.alipay.sofa.rpc.message.http;

import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.message.AbstractResponseFuture;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Future for http.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class HttpResponseFuture<V> extends AbstractResponseFuture<V> {

    /**
     * sofa请求
     */
    protected final SofaRequest request;

    /**
     * sofa响应
     */
    protected SofaResponse      response;

    /**
     * 构造函数
     */
    public HttpResponseFuture(SofaRequest request, int timeout) {
        super(timeout);
        this.request = request;
    }

    @Override
    protected TimeoutException clientTimeoutException() {
        throw new SofaTimeOutException(LogCodes.getLog(LogCodes.ERROR_INVOKE_TIMEOUT,
            SerializerFactory.getAliasByCode(request.getSerializeType()),
            request.getTargetServiceUniqueName(),
            request.getMethodName(), "",
            StringUtils.objectsToString(request.getMethodArgs()), timeout));
    }

    @Override
    protected V getNow() throws ExecutionException {
        if (cause != null) {
            // 异常
            throw new ExecutionException(cause);
        } else if (result instanceof SofaResponse) {
            SofaResponse response = (SofaResponse) result;
            if (response.isError()) {
                cause = new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, response.getErrorMsg());
                throw new ExecutionException(cause);
            } else {
                result = response.getAppResponse();
                if (result instanceof Throwable) {
                    throw new ExecutionException((Throwable) result);
                } else {
                    return (V) result;
                }
            }
        } else {
            return (V) result;
        }
    }

    @Override
    protected void releaseIfNeed(Object result) {
        if (result instanceof SofaResponse) {
            AbstractByteBuf byteBuffer = response.getData();
            if (byteBuffer != null) {
                byteBuffer.release();
                response.setData(null);
            }
        }
    }

    @Override
    public ResponseFuture addListeners(List<SofaResponseCallback> list) {
        throw new UnsupportedOperationException("Not supported, Please use callback function");
    }

    @Override
    public ResponseFuture addListener(SofaResponseCallback sofaResponseCallback) {
        throw new UnsupportedOperationException("Not supported, Please use callback function");
    }

    @Override
    public void notifyListeners() {

    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     */
    public SofaResponse getSofaResponse(int timeout, TimeUnit unit) throws CancellationException,
        TimeoutException, InterruptedException, ExecutionException {
        long realTimeOut = unit.toMillis(timeout);
        long remainTime = realTimeOut - (sentTime - genTime); // 剩余时间
        if (remainTime <= 0) { // 没有剩余时间不等待
            if (isDone()) { // 直接看是否已经返回
                return getNowResponse();
            }
        } else { // 等待剩余时间
            if (await(remainTime, TimeUnit.MILLISECONDS)) {
                return getNowResponse();
            }
        }
        this.setDoneTime();
        throw new TimeoutException();
    }

    protected SofaResponse getNowResponse() throws ExecutionException, CancellationException {
        if (cause != null) {
            if (cause == CANCELLATION_CAUSE) {
                throw (CancellationException) cause;
            } else {
                throw new ExecutionException(cause);
            }
        } else if (result instanceof SofaResponse) {
            return (SofaResponse) result;
        } else {
            throw new ExecutionException(new IllegalArgumentException("Not sofa response!"));
        }
    }
}
