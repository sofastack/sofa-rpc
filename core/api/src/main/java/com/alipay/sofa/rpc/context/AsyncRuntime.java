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
package com.alipay.sofa.rpc.context;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.common.utils.ThreadPoolUtils;
import com.alipay.sofa.rpc.log.TimeWaitLogger;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;

/**
 * 异步执行运行时
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class AsyncRuntime {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                LOGGER = LoggerFactory.getLogger(AsyncRuntime.class);

    /**
     * callback业务线程池（callback+async）
     */
    private static volatile ThreadPoolExecutor asyncThreadPool;

    /**
     * 得到callback用的线程池 默认开始创建
     *
     * @return callback用的线程池
     */
    public static ThreadPoolExecutor getAsyncThreadPool() {
        return getAsyncThreadPool(true);
    }

    /**
     * 得到callback用的线程池
     *
     * @param build 没有时是否构建
     * @return callback用的线程池
     */
    public static ThreadPoolExecutor getAsyncThreadPool(boolean build) {
        if (asyncThreadPool == null && build) {
            synchronized (AsyncRuntime.class) {
                if (asyncThreadPool == null && build) {
                    // 一些系统参数，可以从配置或者注册中心获取。
                    int coresize = RpcConfigs.getIntValue(RpcOptions.ASYNC_POOL_CORE);
                    int maxsize = RpcConfigs.getIntValue(RpcOptions.ASYNC_POOL_MAX);
                    int queuesize = RpcConfigs.getIntValue(RpcOptions.ASYNC_POOL_QUEUE);
                    int keepAliveTime = RpcConfigs.getIntValue(RpcOptions.ASYNC_POOL_TIME);

                    BlockingQueue<Runnable> queue = ThreadPoolUtils.buildQueue(queuesize);
                    NamedThreadFactory threadFactory = new NamedThreadFactory("RPC-CB", true);

                    RejectedExecutionHandler handler = new RejectedExecutionHandler() {
                        private final TimeWaitLogger timeWaitLogger = new TimeWaitLogger(1000);
                        private final BiConsumer<Runnable,ThreadPoolExecutor> biConsumer = (r, executor) -> LOGGER.warn("Task:{} has been reject because of threadPool exhausted! pool:{}, active:{}, queue:{}, taskcnt: {}", r,
                                executor.getPoolSize(),
                                executor.getActiveCount(),
                                executor.getQueue().size(),
                                executor.getTaskCount());

                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                                if (LOGGER.isWarnEnabled()) {
                                    timeWaitLogger.logWithBiConsume(biConsumer,r,executor);
                                }
                            throw new RejectedExecutionException(
                                LogCodes.getLog(LogCodes.ERROR_ASYNC_THREAD_POOL_REJECT));
                        }
                    };
                    asyncThreadPool = ThreadPoolUtils.newCachedThreadPool(
                        coresize, maxsize, keepAliveTime, queue, threadFactory, handler);
                }
            }
        }
        return asyncThreadPool;
    }
}
