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

import com.alipay.sofa.rpc.common.SofaConfigs;

/**
 * @author zhaowang
 * @version : TimeWaitLogger.java, v 0.1 2020年07月31日 10:46 上午 zhaowang Exp $
 */
public class TimeWaitLogger {

    public static final String DISABLE_TIME_WAIT_CONF = "sofa.rpc.log.disableTimeWaitLog";
    private long               waitTime;
    private long               lastLogTime;
    private boolean            disabled;

    public TimeWaitLogger(long waitTimeMills) {
        this.waitTime = waitTimeMills;
        init();
    }

    public void logWithWaitTime(Runnable runnable) {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis > lastLogTime + waitTime || disabled) {
            lastLogTime = currentTimeMillis;
            runnable.run();
        }
    }

    protected void init() {
        this.disabled = SofaConfigs.getBooleanValue(DISABLE_TIME_WAIT_CONF, false);
    }

}