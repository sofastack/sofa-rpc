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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class EagerThreadPoolExecutorTest {

    /**
     * It print like this:
     * thread number in current pool：1,  task number in task queue：0 executor size: 1
     * thread number in current pool：2,  task number in task queue：0 executor size: 2
     * thread number in current pool：3,  task number in task queue：0 executor size: 3
     * thread number in current pool：4,  task number in task queue：0 executor size: 4
     * thread number in current pool：5,  task number in task queue：0 executor size: 5
     * thread number in current pool：6,  task number in task queue：0 executor size: 6
     * thread number in current pool：7,  task number in task queue：0 executor size: 7
     * thread number in current pool：8,  task number in task queue：0 executor size: 8
     * thread number in current pool：9,  task number in task queue：0 executor size: 9
     * thread number in current pool：10,  task number in task queue：0 executor size: 10
     * thread number in current pool：10,  task number in task queue：4 executor size: 10
     * thread number in current pool：10,  task number in task queue：3 executor size: 10
     * thread number in current pool：10,  task number in task queue：2 executor size: 10
     * thread number in current pool：10,  task number in task queue：1 executor size: 10
     * thread number in current pool：10,  task number in task queue：0 executor size: 10
     * <p>
     * We can see , when the core threads are in busy,
     * the thread pool create thread (but thread nums always less than max) instead of put task into queue.
     */
    @Test
    public void testEagerThreadPool() throws Exception {
        int queues = 5;
        int cores = 5;
        int threads = 10;
        // alive 1 second
        long alive = 1000;

        //init queue and executor
        TaskQueue<Runnable> taskQueue = new TaskQueue<Runnable>(queues);
        final EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(cores,
                threads,
                alive,
                TimeUnit.MILLISECONDS,
                taskQueue);
        taskQueue.setExecutor(executor);

        for (int i = 0; i < 15; i++) {
            Thread.sleep(50);
            executor.execute(() -> {
                System.out.println("thread number in current pool：" + executor.getPoolSize() + ",  task number in task queue：" + executor.getQueue()
                        .size() + " executor size: " + executor.getPoolSize());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        Thread.sleep(5000);
        // core threads are all alive.
        Assert.assertEquals("more than cores threads alive!", executor.getPoolSize(), cores);
    }

    @Test
    public void testEagerThreadPool_rejectExecution() throws Exception {
        int cores = 1;
        int threads = 3;
        int queues = 2;
        long alive = 1000;

        // init queue and executor
        TaskQueue<Runnable> taskQueue = new TaskQueue<>(queues);
        final EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(cores,
                threads,
                alive, TimeUnit.MILLISECONDS,
                taskQueue);
        taskQueue.setExecutor(executor);

        Runnable runnable = () -> {
            System.out.println("thread number in current pool: " + executor.getPoolSize() + ", task number is task queue: " + executor.getQueue().size());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        for (int i = 0; i < 5; i++) {
            Thread.sleep(50);
            executor.execute(runnable);
        }
        Assert.assertThrows(RejectedExecutionException.class, () -> executor.execute(runnable));

        Thread.sleep(10000);
    }

    @Test
    public void testEagerThreadPool_extendThreadsBeforeEnqueue() throws InterruptedException {
        int cores = 2;
        int threads = 4;
        int queues = 2;
        long alive = 1000;

        TaskQueue<Runnable> taskQueue = new TaskQueue<>(queues);
        final EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(cores,
                threads,
                alive, TimeUnit.MILLISECONDS,
                taskQueue);
        taskQueue.setExecutor(executor);
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Runnable runnable = () -> {
            System.out.println("thread number in current pool: " + executor.getPoolSize() + ", task number is task queue: " + executor.getQueue().size());
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        for (int i = 0; i < 4; i++) {
            executor.execute(runnable);
        }
        Assert.assertEquals(4, executor.getPoolSize());
        Assert.assertEquals(0, taskQueue.size());
        for (int i = 0; i < 2; i++) {
            executor.execute(runnable);
        }
        Assert.assertEquals(4, executor.getPoolSize());
        Assert.assertEquals(2, taskQueue.size());
        Assert.assertThrows(RejectedExecutionException.class, () -> executor.execute(runnable));
        countDownLatch.countDown();
    }

}