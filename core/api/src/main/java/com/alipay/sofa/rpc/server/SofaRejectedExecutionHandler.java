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
package com.alipay.sofa.rpc.server;

import com.alipay.sofa.rpc.log.TimeWaitLogger;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

/**
 * RejectedExecutionHandler when thread pool is full.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SofaRejectedExecutionHandler implements RejectedExecutionHandler {

    private static final Logger  LOGGER         = LoggerFactory.getLogger(SofaRejectedExecutionHandler.class);
    private final TimeWaitLogger timeWaitLogger = new TimeWaitLogger(1000);
    private final Consumer<ThreadPoolExecutor> logConsumer = (executor) -> LOGGER.warn(LogCodes.getLog(LogCodes.ERROR_PROVIDER_TR_POOL_REJECTION,
            executor.getActiveCount(),
            executor.getPoolSize(),
            executor.getLargestPoolSize(),
            executor.getCorePoolSize(),
            executor.getMaximumPoolSize(),
            executor.getQueue().size(),
            executor.getQueue().remainingCapacity()));
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (LOGGER.isWarnEnabled()) {
            timeWaitLogger.logWithConsumer(logConsumer,executor);
        }
        throw new RejectedExecutionException();
    }
}
