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

import com.alipay.sofa.rpc.config.ServerConfig;

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
