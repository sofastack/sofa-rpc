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
package com.alipay.sofa.rpc.message;

import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.SofaRequest;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Future of bolt.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltResponseFuture<V> implements ResponseFuture<V> {

    /**
     * sofa请求
     */
    protected final SofaRequest request;

    /**
     * 返回的结果。如果返回的是异常，那就是个CauseHolder对象
     *
     * @see CauseHolder
     */
    private volatile Object     result;

    /**
     * 异常包装类
     */
    private static final class CauseHolder {
        final Throwable cause;

        private CauseHolder(Throwable cause) {
            this.cause = cause;
        }
    }

    /**
     * 用户设置的超时时间
     */
    private final int     timeout;
    /**
     * Future生成时间
     */
    private final long    genTime = RpcRuntimeContext.now();
    /**
     * Future已发送时间
     */
    private volatile long sentTime;
    /**
     * Future完成的时间
     */
    private volatile long doneTime;

    /**
     * 构造函数
     */
    public BoltResponseFuture(SofaRequest request, int timeout) {
        this.request = request;
        this.timeout = timeout;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("unsupported cancel method");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return result != null;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        try {
            return (V) get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long realTimeOut = unit.toMillis(timeout);
        long remainTime = realTimeOut - (sentTime - genTime); // 剩余时间
        if (remainTime <= 0) { // 没有剩余时间不等待
            if (isDone()) { // 直接看是否已经返回
                return getNow();
            }
        } else { // 等待剩余时间
            if (await(remainTime, TimeUnit.MILLISECONDS)) {
                return getNow();
            }
        }
        this.setDoneTime();
        throw new TimeoutException();
    }

    private V getNow() throws ExecutionException {
        Object result = this.result;
        if (result instanceof CauseHolder) { // 异常
            Throwable e = ((CauseHolder) result).cause;
            throw new ExecutionException(e);
        } else {
            return (V) result;
        }
    }

    private boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        return await0(unit.toNanos(timeout), true);
    }

    private boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
        if (isDone()) {
            return true;
        }
        if (timeoutNanos <= 0) {
            return isDone();
        }
        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException(toString());
        }
        long startTime = System.nanoTime();
        long waitTime = timeoutNanos;
        boolean interrupted = false;
        try {
            synchronized (this) {
                if (isDone()) {
                    return true;
                }
                //if (waitTime <= 0) {
                //    return isDone();
                //}
                //checkDeadLock(); need this check?
                incWaiters();
                try {
                    for (;;) {
                        try {
                            wait(waitTime / 1000000, (int) (waitTime % 1000000));
                        } catch (InterruptedException e) {
                            if (interruptable) {
                                throw e;
                            } else {
                                interrupted = true;
                            }
                        }

                        if (isDone()) {
                            return true;
                        } else {
                            waitTime = timeoutNanos - (System.nanoTime() - startTime);
                            if (waitTime <= 0) {
                                return isDone();
                            }
                        }
                    }
                } finally {
                    decWaiters();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private short waiters;

    private boolean hasWaiters() {
        return waiters > 0;
    }

    private void incWaiters() {
        if (waiters == Short.MAX_VALUE) {
            throw new IllegalStateException("too many waiters: " + this);
        }
        waiters++;
    }

    private void decWaiters() {
        waiters--;
    }

    /**
     * 设置正常返回结果
     *
     * @param result 正常返回值
     */
    void setSuccess(V result) {
        if (setSuccess0(result)) {
            return;
        }
        throw new IllegalStateException("complete already: " + this);
    }

    private boolean setSuccess0(V result) {
        if (isDone()) {
            return false;
        }
        synchronized (this) {
            // Allow only once.
            if (isDone()) {
                return false;
            }
            if (this.result == null) {
                this.result = result;
            }
            this.setDoneTime();
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return true;
    }

    /**
     * 设置异常
     *
     * @param cause 异常类型
     */
    void setFailure(Throwable cause) {
        if (setFailure0(cause)) {
            return;
        }
        throw new IllegalStateException("complete already: " + this, cause);
    }

    private boolean setFailure0(Throwable cause) {
        if (isDone()) {
            return false;
        }
        synchronized (this) {
            if (isDone()) {
                return false;
            }
            result = new CauseHolder(cause);
            this.setDoneTime();
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return true;
    }

    /**
     * 设置已发送时间
     */
    public void setSentTime() {
        this.sentTime = RpcRuntimeContext.now();
    }

    /**
     * 记录结束时间
     */
    private void setDoneTime() {
        if (doneTime == 0L) {
            doneTime = RpcRuntimeContext.now();
        }
    }

    /**
     * 查看future耗时
     *
     * @return 耗时
     */
    public long getElapsedTime() {
        return doneTime - genTime;
    }

    @Override
    public ResponseFuture addListeners(List<SofaResponseCallback> list) {
        throw new UnsupportedOperationException("Not supported, Please use callback function");
    }

    @Override
    public ResponseFuture addListener(SofaResponseCallback sofaResponseCallback) {
        throw new UnsupportedOperationException("Not supported, Please use callback function");
    }

}
