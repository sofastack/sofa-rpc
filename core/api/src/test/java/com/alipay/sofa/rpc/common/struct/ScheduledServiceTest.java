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
package com.alipay.sofa.rpc.common.struct;

import com.alipay.sofa.rpc.common.threadpool.SofaExecutorFactory;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ScheduledServiceTest {

    private static final AtomicInteger count = new AtomicInteger();

    @Test
    public void testAll() throws InterruptedException {
        ScheduledService scheduledService = new ScheduledService("ttt", 12345,
            new Runnable() {
                @Override
                public void run() {
                    count.incrementAndGet();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        scheduledService.start();
        Assert.assertFalse(scheduledService.isStarted());

        scheduledService = new ScheduledService("ttt", ScheduledService.MODE_FIXEDRATE,
            new Runnable() {
                @Override
                public void run() {
                    count.incrementAndGet();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        scheduledService.start();
        Assert.assertTrue(scheduledService.isStarted());
        scheduledService.start();
        Assert.assertTrue(scheduledService.isStarted());
        Thread.sleep(300);
        Assert.assertTrue(count.get() > 0);

        ScheduledService.reset();
        Assert.assertTrue(scheduledService.isStarted());
        Assert.assertFalse(ScheduledService.isResetting());

        scheduledService.shutdown();
        Assert.assertFalse(scheduledService.isStarted());
        scheduledService.stop();
        Assert.assertFalse(scheduledService.isStarted());
    }

    @Test
    public void testReuse() {
        String testPrefix = "prefix-for-schedule-test";

        ScheduledService scheduledService1 = new ScheduledService(testPrefix, ScheduledService.MODE_FIXEDRATE, () -> {}, 0, 100, TimeUnit.MILLISECONDS).start();
        ScheduledService scheduledService2 = new ScheduledService(testPrefix, ScheduledService.MODE_FIXEDRATE, () -> {}, 0, 100, TimeUnit.MILLISECONDS).start();

        SofaExecutorFactory factory = ExtensionLoaderFactory.getExtensionLoader(SofaExecutorFactory.class).getExtension("reuse-scheduled");
        ScheduledThreadPoolExecutor pool = (ScheduledThreadPoolExecutor) factory.createExecutor(testPrefix, null);

        try{
            Thread.sleep(100);
        } catch (Exception e) {

        }

        BlockingQueue<Runnable> queue = pool.getQueue();
        Assert.assertEquals(2, queue.size());

        scheduledService1.shutdown();
        scheduledService2.shutdown();
        Assert.assertFalse(pool.isShutdown());
    }
}