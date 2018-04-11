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

import com.alipay.sofa.rpc.client.aft.InvocationStatDimension;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;

/**
 * 具体的一种调控统计结果维度.
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class ServiceExceptionInvocationStat extends AbstractInvocationStat {

    /**
     * Instantiates a new Service exception invocation stat.
     *
     * @param invocation the invocation
     */
    public ServiceExceptionInvocationStat(InvocationStatDimension invocation) {
        super(invocation);
    }

    @Override
    public long catchException(Throwable t) {
        if (t instanceof SofaRpcException) {
            SofaRpcException exception = (SofaRpcException) t;
            if (exception.getErrorType() == RpcErrorType.CLIENT_TIMEOUT
                || exception.getErrorType() == RpcErrorType.SERVER_BUSY) {
                return exceptionCount.incrementAndGet();
            }
        }
        return exceptionCount.get();
    }
}