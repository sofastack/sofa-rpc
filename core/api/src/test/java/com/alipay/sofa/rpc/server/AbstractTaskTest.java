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

import java.util.PriorityQueue;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class AbstractTaskTest {

    @Test
    public void compareTo() {
        TestTask testTask1 = new TestTask();
        testTask1.setPriority(123);
        Assert.assertEquals(123, testTask1.getPriority());

        TestTask testTask2 = new TestTask();
        testTask2.setPriority(234);

        Assert.assertTrue(testTask1.compareTo(testTask2) > 0);

        PriorityQueue<AbstractTask> queue = new PriorityQueue<AbstractTask>();
        queue.offer(testTask1);
        queue.offer(testTask2);
        Assert.assertEquals(queue.poll(), testTask2);
        Assert.assertEquals(queue.poll(), testTask1);
    }
}