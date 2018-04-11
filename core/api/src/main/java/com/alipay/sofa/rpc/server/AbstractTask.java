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

import com.alipay.sofa.rpc.common.annotation.Unstable;
import com.alipay.sofa.rpc.common.utils.ThreadPoolUtils;

/**
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Unstable
public abstract class AbstractTask implements Runnable, Comparable<AbstractTask> {

    /**
     * 优先级，越大越高
     */
    protected int priority = ThreadPoolUtils.THREAD_PRIORITY_NORMAL;

    @Override
    public abstract void run();

    @Override
    public int compareTo(AbstractTask o) {
        // 值越大越高
        return o.getPriority() - this.getPriority();
    }

    public AbstractTask setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public int getPriority() {
        return priority;
    }
}
