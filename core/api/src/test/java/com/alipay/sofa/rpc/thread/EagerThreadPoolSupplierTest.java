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

import org.junit.Test;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class EagerThreadPoolSupplierTest {

    @Test
    public void newThreadPool() {
        int minPoolSize = 1;
        int maxPoolSize = 2;
        int queueSize = 3;
        int aliveTime = 4;

        EagerThreadPoolSupplier supplier = new EagerThreadPoolSupplier();
        ThreadPoolExecutor threadPoolExecutor = supplier.newThreadPool(minPoolSize, maxPoolSize, queueSize, aliveTime);
        assertEquals(minPoolSize, threadPoolExecutor.getCorePoolSize());
        assertEquals(maxPoolSize, threadPoolExecutor.getMaximumPoolSize());
        assertEquals(queueSize, threadPoolExecutor.getQueue().remainingCapacity());
        assertEquals(aliveTime, threadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS));
        assertEquals(EagerThreadPoolExecutor.class, threadPoolExecutor.getClass());
    }
}