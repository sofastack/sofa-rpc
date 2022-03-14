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
import com.alipay.sofa.rpc.common.SofaConfigs;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author zhaowang
 * @version : TimeWaitLogger.java, v 0.1 2020年07月31日 10:46 上午 zhaowang Exp $
 */
public class TimeWaitLogger {

    private final long    waitTime;
    private volatile long lastLogTime;
    private final boolean disabled;

    public TimeWaitLogger(long waitTimeMills) {
        this.waitTime = waitTimeMills;
        this.disabled = SofaConfigs.getBooleanValue(RpcOptions.DISABLE_LOG_TIME_WAIT_CONF, false);
    }

    public void logWithRunnable(Runnable runnable) {
        long currentTimeMillis = System.currentTimeMillis();
        if (disabled) {
            runnable.run();
        } else if (currentTimeMillis > lastLogTime + waitTime) {
            lastLogTime = currentTimeMillis;
            runnable.run();
        }

    }

    public <T> void logWithConsumer(Consumer<T> consumer, T t) {
        long currentTimeMillis = System.currentTimeMillis();
        if (disabled) {
            consumer.accept(t);
        } else if (currentTimeMillis > lastLogTime + waitTime) {
            lastLogTime = currentTimeMillis;
            consumer.accept(t);
        }
    }

    public <T, R> void logWithBiConsume(BiConsumer<T, R> biConsumer, T r, R executor) {
        long currentTimeMillis = System.currentTimeMillis();
        if (disabled) {
            biConsumer.accept(r, executor);
        } else if (currentTimeMillis > lastLogTime + waitTime) {
            lastLogTime = currentTimeMillis;
            biConsumer.accept(r, executor);
        }
    }
}