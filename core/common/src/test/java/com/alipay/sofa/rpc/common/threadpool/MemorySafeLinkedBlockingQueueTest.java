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
package com.alipay.sofa.rpc.common.threadpool;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.Assert;
import org.junit.Test;

import java.lang.instrument.Instrumentation;

public class MemorySafeLinkedBlockingQueueTest {

    @Test
    public void test() throws Exception {
        ByteBuddyAgent.install();
        final Instrumentation instrumentation = ByteBuddyAgent.getInstrumentation();
        final long objectSize = instrumentation.getObjectSize((Runnable) () -> {
        });
        int maxFreeMemory = (int) MemoryLimitCalculator.maxAvailable();
        MemorySafeLinkedBlockingQueue<Runnable> queue = new MemorySafeLinkedBlockingQueue<>(maxFreeMemory);

        // all memory is reserved for JVM, so it will fail here
        Assert.assertEquals(queue.offer(() -> {
        }), false);

        // maxFreeMemory-objectSize Byte memory is reserved for the JVM, so this will succeed
        queue.setMaxFreeMemory((int) (MemoryLimitCalculator.maxAvailable() - objectSize));
        Assert.assertEquals(queue.offer(() -> {
        }), true);
    }
}