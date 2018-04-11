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
package com.alipay.sofa.rpc.event;

/**
 * Subscriber of event.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @see EventBus
 */
public abstract class Subscriber {
    /**
     * 接到事件是否同步执行
     */
    protected boolean sync = true;

    /**
     * 事件订阅者
     */
    protected Subscriber() {
    }

    /**
     * 事件订阅者
     *
     * @param sync 是否同步
     */
    protected Subscriber(boolean sync) {
        this.sync = sync;
    }

    /**
     * 是否同步
     *
     * @return 是否同步
     */
    public boolean isSync() {
        return sync;
    }

    /**
     * 事件处理，请处理异常
     *
     * @param event 事件
     */
    public abstract void onEvent(Event event);

}
