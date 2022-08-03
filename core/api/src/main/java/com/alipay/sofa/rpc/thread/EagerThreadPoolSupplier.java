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
package com.alipay.sofa.rpc.thread;

import com.alipay.sofa.rpc.ext.Extension;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaowang
 * @version : EagerThreadPoolSupplier.java, v 0.1 2022年08月03日 2:51 PM zhaowang
 */
@Extension("eager")
public class EagerThreadPoolSupplier implements ThreadPoolSupplier {

    @Override
    public ThreadPoolExecutor newThreadPool(int cores, int threads, int queueSize, int alive) {
        TaskQueue<Runnable> taskQueue = new TaskQueue<Runnable>(queueSize <= 0 ? 1 : queueSize);
        EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(cores,
            threads,
            alive,
            TimeUnit.MILLISECONDS,
            taskQueue);
        taskQueue.setExecutor(executor);
        return executor;
    }
}