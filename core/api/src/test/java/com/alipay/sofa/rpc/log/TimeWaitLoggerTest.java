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
package com.alipay.sofa.rpc.log;

import com.alipay.sofa.rpc.common.RpcOptions;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhaowang
 * @version : TimeWaitLoggerTest.java, v 0.1 2020年07月31日 10:51 上午 zhaowang Exp $
 */
public class TimeWaitLoggerTest {

    @Test
    public void testTimeWait() throws InterruptedException {
        TimeWaitLogger timeWaitLogger = new TimeWaitLogger(1000);
        AtomicLong atomicLong = new AtomicLong();
        new Thread(()->{
            while (true){
                timeWaitLogger.logWithRunnable(atomicLong::incrementAndGet);
            }
        }).start();
        Thread.sleep(1500);
        Assert.assertEquals(2L,atomicLong.get());
    }

    @Test
    public void testDisable() throws InterruptedException {
        System.setProperty(RpcOptions.DISABLE_LOG_TIME_WAIT_CONF,"true");
        try{
            TimeWaitLogger timeWaitLogger = new TimeWaitLogger(1000);
            AtomicLong atomicLong = new AtomicLong();
            new Thread(()->{
                while (true){
                    timeWaitLogger.logWithRunnable(atomicLong::incrementAndGet);
                }
            }).start();
            Thread.sleep(1500);
            Assert.assertTrue(atomicLong.get()>1000);
        }finally {
            System.setProperty(RpcOptions.DISABLE_LOG_TIME_WAIT_CONF,"");
        }

        TimeWaitLogger timeWaitLogger = new TimeWaitLogger(1000);
        AtomicLong atomicLong = new AtomicLong();
        new Thread(()->{
            while (true){
                timeWaitLogger.logWithRunnable(atomicLong::incrementAndGet);
            }
        }).start();
        Thread.sleep(1500);
        Assert.assertEquals(2L,atomicLong.get());

    }
}