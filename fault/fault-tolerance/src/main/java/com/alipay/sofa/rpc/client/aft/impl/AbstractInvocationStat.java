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
package com.alipay.sofa.rpc.client.aft.impl;

import com.alipay.sofa.rpc.client.aft.InvocationStat;
import com.alipay.sofa.rpc.client.aft.InvocationStatDimension;
import com.alipay.sofa.rpc.common.utils.CalculateUtils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The type Abstract dimension stat.
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public abstract class AbstractInvocationStat implements InvocationStat {
    /**
     * 统计维度
     */
    protected final InvocationStatDimension dimension;
    /**
     * 调用次数
     */
    protected final AtomicLong              invokeCount    = new AtomicLong(0L);
    /**
     * 异常次数
     */
    protected final AtomicLong              exceptionCount = new AtomicLong(0L);

    /**
     * when useless in one window, this value increment 1. <br />
     * If this value is greater than threshold, this stat will be deleted.
     */
    private final transient AtomicInteger   uselessCycle   = new AtomicInteger(0);

    /**
     * Instantiates a new Abstract dimension stat.
     *
     * @param dimension the dimension
     */
    public AbstractInvocationStat(InvocationStatDimension dimension) {
        this.dimension = dimension;
    }

    @Override
    public InvocationStatDimension getDimension() {
        return dimension;
    }

    /**
     * Gets useless cycle.
     *
     * @return the useless cycle
     */
    @Override
    public AtomicInteger getUselessCycle() {
        return uselessCycle;
    }

    @Override
    public long invoke() {
        return invokeCount.incrementAndGet();
    }

    @Override
    public long getInvokeCount() {
        return invokeCount.get();
    }

    @Override
    public double getExceptionRate() {
        long invokeCount = getInvokeCount();
        return invokeCount == 0 ? -1 : CalculateUtils.divide(getExceptionCount(), invokeCount);
    }

    @Override
    public long getExceptionCount() {
        return exceptionCount.get();
    }

    /**
     * Sets invoke count.
     *
     * @param count the count
     */
    protected void setInvokeCount(long count) {
        invokeCount.set(count);
    }

    /**
     * Sets server exception.
     *
     * @param count the count
     */
    public void setExceptionCount(long count) {
        exceptionCount.set(count);
    }

    @Override
    public InvocationStat snapshot() {
        ServiceExceptionInvocationStat invocationStat = new ServiceExceptionInvocationStat(dimension);
        invocationStat.setInvokeCount(getInvokeCount());
        invocationStat.setExceptionCount(getExceptionCount());
        return invocationStat;
    }

    @Override
    public void update(InvocationStat snapshot) {
        invokeCount.addAndGet(-snapshot.getInvokeCount());
        exceptionCount.addAndGet(-snapshot.getExceptionCount());
    }
}