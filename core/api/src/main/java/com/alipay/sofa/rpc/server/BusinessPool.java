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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.common.utils.ThreadPoolUtils;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.transport.ServerTransportConfig;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Business pool 
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class BusinessPool {

    public static synchronized ThreadPoolExecutor initPool(ServerTransportConfig transportConfig) {
        // 计算线程池大小
        int minPoolSize; // TODO 最小值和存活时间是否可配？
        int aliveTime;
        int maxPoolSize = transportConfig.getBizMaxThreads();
        String poolType = transportConfig.getBizPoolType();
        if (RpcConstants.THREADPOOL_TYPE_FIXED.equals(poolType)) {
            minPoolSize = maxPoolSize;
            aliveTime = 0;
        } else if (RpcConstants.THREADPOOL_TYPE_CACHED.equals(poolType)) {
            minPoolSize = 20;
            maxPoolSize = Math.max(minPoolSize, maxPoolSize);
            aliveTime = 60000;
        } else {
            throw ExceptionUtils.buildRuntime("server.threadPoolType", poolType);
        }

        // 初始化队列
        String queueType = transportConfig.getBizPoolQueueType();
        int queueSize = transportConfig.getBizPoolQueues();
        boolean isPriority = RpcConstants.QUEUE_TYPE_PRIORITY.equals(queueType);
        BlockingQueue<Runnable> configQueue = ThreadPoolUtils.buildQueue(queueSize, isPriority);

        return new ThreadPoolExecutor(minPoolSize, maxPoolSize, aliveTime, TimeUnit.MILLISECONDS, configQueue);
    }

    public static ThreadPoolExecutor initPool(ServerConfig serverConfig) {
        int minPoolSize = serverConfig.getCoreThreads();
        int maxPoolSize = serverConfig.getMaxThreads();
        int queueSize = serverConfig.getQueues();
        int aliveTime = serverConfig.getAliveTime();

        BlockingQueue<Runnable> poolQueue = queueSize > 0 ? new LinkedBlockingQueue<Runnable>(
            queueSize) : new SynchronousQueue<Runnable>();

        return new ThreadPoolExecutor(minPoolSize, maxPoolSize, aliveTime, TimeUnit.MILLISECONDS, poolQueue);
    }

}
