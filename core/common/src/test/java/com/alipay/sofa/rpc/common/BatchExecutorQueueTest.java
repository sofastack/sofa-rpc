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
package com.alipay.sofa.rpc.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author chengming
 * @version BatchExecutorQueueTest.java, v 0.1 2024年03月01日 10:55 AM chengming
 */
public class BatchExecutorQueueTest {

    private BatchExecutorQueue<Object> batchExecutorQueue;

    @Mock
    private Executor                   mockExecutor;

    @Captor
    private ArgumentCaptor<Runnable>   runnableCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        batchExecutorQueue = spy(new BatchExecutorQueue<>(2));
    }

    @Test
    public void testEnqueueAndRun() {
        Object message1 = new Object();
        Object message2 = new Object();
        Object message3 = new Object();

        // 测试 enqueue 方法以及是否通过 executor 调度了 run 方法
        batchExecutorQueue.enqueue(message1, mockExecutor);
        batchExecutorQueue.enqueue(message2, mockExecutor);
        batchExecutorQueue.enqueue(message3, mockExecutor);

        verify(mockExecutor, atLeastOnce()).execute(runnableCaptor.capture());

        Runnable scheduledRunnable = runnableCaptor.getValue();
        Assert.assertNotNull(scheduledRunnable);
        scheduledRunnable.run();

        // 验证队列是否为空
        Assert.assertTrue(batchExecutorQueue.getQueue().isEmpty());
    }

    @Test
    public void testScheduleFlush() {
        AtomicBoolean scheduled = batchExecutorQueue.getScheduled();
        Assert.assertFalse(scheduled.get());

        batchExecutorQueue.scheduleFlush(mockExecutor);
        Assert.assertTrue(scheduled.get());

        // 验证是否有任务提交到 executor
        verify(mockExecutor).execute(any(Runnable.class));
    }

}
