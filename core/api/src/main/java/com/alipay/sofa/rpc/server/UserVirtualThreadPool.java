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

import com.alipay.sofa.rpc.common.threadpool.SofaExecutorFactory;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author junyuan
 * @version UserVirtualThreadPool.java, v 0.1 2023年12月14日 14:17 junyuan Exp $
 */
public class UserVirtualThreadPool extends UserThreadPool {
    private static final AtomicInteger POOL_NAME_COUNTER = new AtomicInteger(0);

    /**
     * 线程名字
     *
     */
    private String                     threadPoolName;

    public UserVirtualThreadPool() {
        this.threadPoolName = DEFAUT_POOL_NAME + "-" + POOL_NAME_COUNTER.getAndIncrement();
    }

    @Override
    protected ExecutorService buildExecutorService() {
        return (ExecutorService) ExtensionLoaderFactory.getExtensionLoader(SofaExecutorFactory.class)
            .getExtension("virtual").createExecutor(threadPoolName);
    }
}