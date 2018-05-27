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

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class SofaRejectedExecutionHandlerTest {
    @Test
    public void rejectedExecution() throws Exception {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new SynchronousQueue(),
            new SofaRejectedExecutionHandler());
        boolean error = false;
        try {
            executor.execute(new MockTask());
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);
        try {
            executor.execute(new MockTask());
        } catch (Exception e) {
            Assert.assertTrue(e instanceof RejectedExecutionException);
            error = true;
        }
        Assert.assertTrue(error);
    }

    private static class MockTask implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(2000);
            } catch (Exception ignore) {
            }
        }
    }
}