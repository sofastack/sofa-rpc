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
package com.alipay.sofa.rpc.client.aft;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Invocation statistics
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public interface InvocationStat {

    /**
     * The Dimension of invocation statistics
     *
     * @return InvocationStatDimension
     */
    InvocationStatDimension getDimension();

    /**
     * Gets useless cycle.
     *
     * @return the useless cycle
     */
    public AtomicInteger getUselessCycle();

    /**
     * tick one invocation
     *
     * @return current invoke count
     */
    long invoke();

    /**
     * tick one exception event
     *
     * @param t Throwable
     * @return current exception count
     */
    long catchException(Throwable t);

    /**
     * get count of invocation
     *
     * @return current invoke count
     */
    long getInvokeCount();

    /**
     * Get count of exception event
     *
     * @return current invoke count
     */
    long getExceptionCount();

    /**
     * Get rate of exception event. by exception count / invoke count
     *
     * @return exception rate of all invoke 
     */
    double getExceptionRate();

    /**
     * Do snapshot of stat.
     *
     * @return snapshot of one time window
     */
    InvocationStat snapshot();

    /**
     * Update the value of InvocationStat(eg. for next time window)
     *
     * @param snapshot snapshot of one time window
     */
    void update(InvocationStat snapshot);

}