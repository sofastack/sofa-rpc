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

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 简单的计时器，单位毫秒
 * <p>
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@NotThreadSafe
public class StopWatch implements Cloneable {

    /**
     * 当前计时
     */
    private long ticker;
    /**
     * 上次区间处理时间
     */
    private int  lastElapsed;

    /**
     * 记录当前时间
     *
     * @return StopWatch
     */
    public StopWatch tick() {
        long old = ticker;
        ticker = System.currentTimeMillis();
        lastElapsed = (int) (ticker - old);
        return this;
    }

    /**
     * 读取上次区间处理的时间
     *
     * @return StopWatch
     */
    public int read() {
        return lastElapsed;
    }

    /**
     * 重置时间
     *
     * @return StopWatch
     */
    public StopWatch reset() {
        ticker = System.currentTimeMillis();
        lastElapsed = 0;
        return this;
    }

    @Override
    public StopWatch clone() {
        StopWatch watch = new StopWatch();
        watch.ticker = this.ticker;
        watch.lastElapsed = lastElapsed;
        return watch;
    }
}
