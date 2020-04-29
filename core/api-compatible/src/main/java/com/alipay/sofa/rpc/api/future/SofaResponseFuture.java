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
package com.alipay.sofa.rpc.api.future;

import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
import com.alipay.sofa.rpc.log.LogCodes;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class can get the response through Future mechanism
 *
 * @author <a href=mailto:hongwei.yhw@antfin.com>hongwei.yhw</a>
 */
public class SofaResponseFuture {

    /**
     * Returns <tt>true</tt> if this task completed.
     */
    public static boolean isDone() throws SofaRpcException {
        return getFuture().isDone();
    }

    /**
     * get response
     * <p>
     * If remoting get exception, framework will wrapped it to SofaRpcException
     *
     * @param timeout get timeout
     * @param clear   true: framework will clear the ThreadLocal when return
     * @return The response 
     * @throws SofaRpcException When throw SofaRpcException
     * @throws InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     */
    public static Object getResponse(long timeout, boolean clear) throws SofaRpcException, InterruptedException {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        Future future = context.getFuture();
        if (null == future) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                LogCodes.getLog(LogCodes.ERROR_RESPONSE_FUTURE_NULL, Thread.currentThread()));
        }
        try {
            if (clear) {
                context.setFuture(null);
            }
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            // Future设置为超时
            if (!future.isDone()) {
                throw new SofaTimeOutException("Future is not done when timeout.", ex);
            } else {
                throw new SofaTimeOutException(ex.getMessage(), ex);
            }
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SofaRpcException) {
                throw (SofaRpcException) cause;
            } else {
                throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, cause.getMessage(), cause);
            }
        }
    }

    /**
     * @return 原生 Java Future 对象
     * @throws SofaRpcException 当前线程上下文没有值的时候
     */
    public static Future getFuture() throws SofaRpcException {
        return getFuture(false);
    }

    /**
     * @param clear 是否清除线程上下文
     * @return 原生 Java Future 对象
     * @throws SofaRpcException 当前线程上下文没有值的时候
     */
    public static Future getFuture(boolean clear) throws SofaRpcException {
        RpcInvokeContext context = RpcInvokeContext.getContext();
        Future future = context.getFuture();
        if (future == null) {
            throw new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR,
                LogCodes.getLog(LogCodes.ERROR_RESPONSE_FUTURE_NULL,
                    Thread.currentThread()));
        }
        if (clear) {
            context.setFuture(null);
        }
        return future;
    }
}
