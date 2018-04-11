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

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class UserThreadPoolTest {

    @Test
    public void testInit() {
        UserThreadPool threadPool = new UserThreadPool();
        threadPool.init();
        ThreadPoolExecutor executor = threadPool.getExecutor();
        try {
            Assert.assertEquals(executor.getCorePoolSize(), threadPool.getCorePoolSize());
            Assert.assertEquals(executor.getMaximumPoolSize(), threadPool.getMaximumPoolSize());
            Assert.assertEquals(executor.getKeepAliveTime(TimeUnit.MILLISECONDS), threadPool.getKeepAliveTime());
            Assert.assertEquals(executor.allowsCoreThreadTimeOut(), threadPool.isAllowCoreThreadTimeOut());
            Assert.assertTrue(executor.getQueue() instanceof SynchronousQueue);
        } finally {
            if (executor != null) {
                threadPool.destroy();
            }
        }
    }

    @Test
    public void testGet() {
        UserThreadPool threadPool = new UserThreadPool();
        threadPool.setCorePoolSize(10);
        threadPool.setMaximumPoolSize(100);
        threadPool.setKeepAliveTime(200);
        threadPool.setPrestartAllCoreThreads(false);
        threadPool.setAllowCoreThreadTimeOut(false);
        threadPool.setQueueSize(200);
        ThreadPoolExecutor executor = threadPool.getExecutor();
        try {
            Assert.assertEquals(executor.getCorePoolSize(), threadPool.getCorePoolSize());
            Assert.assertEquals(executor.getMaximumPoolSize(), threadPool.getMaximumPoolSize());
            Assert.assertEquals(executor.getKeepAliveTime(TimeUnit.MILLISECONDS), threadPool.getKeepAliveTime());
            Assert.assertEquals(executor.allowsCoreThreadTimeOut(), threadPool.isAllowCoreThreadTimeOut());
            Assert.assertFalse(executor.getQueue() instanceof SynchronousQueue);
        } finally {
            if (executor != null) {
                threadPool.destroy();
            }
        }
    }

    @Test
    public void testPrestartCore() {
        UserThreadPool threadPool = new UserThreadPool();
        threadPool.setCorePoolSize(5);
        threadPool.setPrestartAllCoreThreads(true);
        threadPool.init();
        ThreadPoolExecutor executor = threadPool.getExecutor();
        try {
            Assert.assertEquals(executor.getPoolSize(), 5);
        } finally {
            if (executor != null) {
                threadPool.destroy();
            }
        }
    }

}