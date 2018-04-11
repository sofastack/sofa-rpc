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
package com.alipay.sofa.rpc.common.utils;

import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ThreadPoolUtilsTest {
    @Test
    public void newFixedThreadPool() throws Exception {
        ThreadPoolExecutor executor = ThreadPoolUtils.newFixedThreadPool(10);
        Assert.assertEquals(executor.getCorePoolSize(), 10);
        Assert.assertEquals(executor.getMaximumPoolSize(), 10);
    }

    @Test
    public void newFixedThreadPool1() throws Exception {
        BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        ThreadPoolExecutor executor = ThreadPoolUtils.newFixedThreadPool(10, queue);
        Assert.assertEquals(executor.getCorePoolSize(), 10);
        Assert.assertEquals(executor.getMaximumPoolSize(), 10);
        Assert.assertEquals(executor.getQueue(), queue);
    }

    @Test
    public void newFixedThreadPool2() throws Exception {
        BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        ThreadFactory factory = new NamedThreadFactory("xxx");
        ThreadPoolExecutor executor = ThreadPoolUtils.newFixedThreadPool(10, queue, factory);
        Assert.assertEquals(executor.getCorePoolSize(), 10);
        Assert.assertEquals(executor.getMaximumPoolSize(), 10);
        Assert.assertEquals(executor.getQueue(), queue);
        Assert.assertEquals(executor.getThreadFactory(), factory);
    }

    @Test
    public void newFixedThreadPool3() throws Exception {
        BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        RejectedExecutionHandler handler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

            }
        };
        ThreadFactory factory = new NamedThreadFactory("xxx");
        ThreadPoolExecutor executor = ThreadPoolUtils.newFixedThreadPool(10, queue, factory, handler);
        Assert.assertEquals(executor.getCorePoolSize(), 10);
        Assert.assertEquals(executor.getMaximumPoolSize(), 10);
        Assert.assertEquals(executor.getQueue(), queue);
        Assert.assertEquals(executor.getThreadFactory(), factory);
        Assert.assertEquals(executor.getRejectedExecutionHandler(), handler);
    }

    @Test
    public void newCachedThreadPool() throws Exception {
        ThreadFactory factory = new NamedThreadFactory("xxx");
        ThreadPoolExecutor executor = ThreadPoolUtils.newCachedThreadPool(10, 20);
        Assert.assertEquals(executor.getCorePoolSize(), 10);
        Assert.assertEquals(executor.getMaximumPoolSize(), 20);
    }

    @Test
    public void newCachedThreadPool1() throws Exception {
        BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        ThreadFactory factory = new NamedThreadFactory("xxx");
        ThreadPoolExecutor executor = ThreadPoolUtils.newCachedThreadPool(10, 20, queue);
        Assert.assertEquals(executor.getCorePoolSize(), 10);
        Assert.assertEquals(executor.getMaximumPoolSize(), 20);
        Assert.assertEquals(executor.getQueue(), queue);
    }

    @Test
    public void newCachedThreadPool2() throws Exception {
        BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        ThreadFactory factory = new NamedThreadFactory("xxx");
        ThreadPoolExecutor executor = ThreadPoolUtils.newCachedThreadPool(10, 20, queue, factory);
        Assert.assertEquals(executor.getCorePoolSize(), 10);
        Assert.assertEquals(executor.getMaximumPoolSize(), 20);
        Assert.assertEquals(executor.getQueue(), queue);
        Assert.assertEquals(executor.getThreadFactory(), factory);
    }

    @Test
    public void newCachedThreadPool3() throws Exception {
        BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        RejectedExecutionHandler handler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

            }
        };
        ThreadFactory factory = new NamedThreadFactory("xxx");
        ThreadPoolExecutor executor = ThreadPoolUtils.newCachedThreadPool(10, 20, queue, factory, handler);
        Assert.assertEquals(executor.getCorePoolSize(), 10);
        Assert.assertEquals(executor.getMaximumPoolSize(), 20);
        Assert.assertEquals(executor.getQueue(), queue);
        Assert.assertEquals(executor.getThreadFactory(), factory);
        Assert.assertEquals(executor.getRejectedExecutionHandler(), handler);
    }

    @Test
    public void newCachedThreadPool4() throws Exception {
        BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();
        RejectedExecutionHandler handler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

            }
        };
        ThreadFactory factory = new NamedThreadFactory("xxx");
        ThreadPoolExecutor executor = ThreadPoolUtils.newCachedThreadPool(10, 20, 45678, queue, factory, handler);
        Assert.assertEquals(executor.getCorePoolSize(), 10);
        Assert.assertEquals(executor.getMaximumPoolSize(), 20);
        Assert.assertEquals(executor.getQueue(), queue);
        Assert.assertEquals(executor.getKeepAliveTime(TimeUnit.MILLISECONDS), 45678);
        Assert.assertEquals(executor.getThreadFactory(), factory);
        Assert.assertEquals(executor.getRejectedExecutionHandler(), handler);
    }

    @Test
    public void buildQueue() throws Exception {
        BlockingQueue<Runnable> queue = ThreadPoolUtils.buildQueue(0);
        Assert.assertEquals(queue.getClass(), SynchronousQueue.class);
        queue = ThreadPoolUtils.buildQueue(-1);
        Assert.assertEquals(queue.getClass(), LinkedBlockingQueue.class);
        queue = ThreadPoolUtils.buildQueue(10);
        Assert.assertEquals(queue.getClass(), LinkedBlockingQueue.class);
    }

    @Test
    public void buildQueue1() throws Exception {
        BlockingQueue<Runnable> queue = ThreadPoolUtils.buildQueue(0, true);
        Assert.assertEquals(queue.getClass(), SynchronousQueue.class);
        queue = ThreadPoolUtils.buildQueue(-1, true);
        Assert.assertEquals(queue.getClass(), PriorityBlockingQueue.class);
        queue = ThreadPoolUtils.buildQueue(100, true);
        Assert.assertEquals(queue.getClass(), PriorityBlockingQueue.class);
        queue = ThreadPoolUtils.buildQueue(-1, false);
        Assert.assertEquals(queue.getClass(), LinkedBlockingQueue.class);
        queue = ThreadPoolUtils.buildQueue(100, false);
        Assert.assertEquals(queue.getClass(), LinkedBlockingQueue.class);
    }

}