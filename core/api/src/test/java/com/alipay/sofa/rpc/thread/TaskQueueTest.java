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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author zhaowang
 * @version : TaskQueueTest.java, v 0.1 2022年08月03日 4:36 PM zhaowang
 */
public class TaskQueueTest {

    @Test
    public void testOffer1() throws Exception {
        Assert.assertThrows(RejectedExecutionException.class, () -> {
            TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
            queue.offer(mock(Runnable.class));
        });
    }

    @Test
    public void testOffer2() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
        Mockito.when(executor.getPoolSize()).thenReturn(2);
        Mockito.when(executor.getActiveCount()).thenReturn(1);
        queue.setExecutor(executor);
        Assert.assertTrue(queue.offer(mock(Runnable.class)));
    }

    @Test
    public void testOffer3() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
        Mockito.when(executor.getPoolSize()).thenReturn(2);
        Mockito.when(executor.getActiveCount()).thenReturn(2);
        Mockito.when(executor.getMaximumPoolSize()).thenReturn(4);
        queue.setExecutor(executor);
        Assert.assertFalse(queue.offer(mock(Runnable.class)));
    }

    @Test
    public void testOffer4() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
        Mockito.when(executor.getPoolSize()).thenReturn(4);
        Mockito.when(executor.getActiveCount()).thenReturn(4);
        Mockito.when(executor.getMaximumPoolSize()).thenReturn(4);
        queue.setExecutor(executor);
        Assert.assertTrue(queue.offer(mock(Runnable.class)));
    }

    @Test
    public void testRetryOffer1() throws Exception {
        Assert.assertThrows(RejectedExecutionException.class, () -> {
            TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
            EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
            Mockito.when(executor.isShutdown()).thenReturn(true);
            queue.setExecutor(executor);
            queue.retryOffer(mock(Runnable.class), 1000, TimeUnit.MILLISECONDS);
        });
    }

    @Test
    public void testRetryOffer2() throws Exception {
        TaskQueue<Runnable> queue = new TaskQueue<Runnable>(1);
        EagerThreadPoolExecutor executor = mock(EagerThreadPoolExecutor.class);
        Mockito.when(executor.isShutdown()).thenReturn(false);
        queue.setExecutor(executor);
        Assert.assertTrue(queue.retryOffer(mock(Runnable.class), 1000, TimeUnit.MILLISECONDS));
    }

}