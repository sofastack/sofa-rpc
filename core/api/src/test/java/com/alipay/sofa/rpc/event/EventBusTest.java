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

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class EventBusTest {
    @Test
    public void isEnable() throws Exception {
        Assert.assertEquals(EventBus.isEnable(), RpcConfigs.getBooleanValue(RpcOptions.EVENT_BUS_ENABLE));
    }

    @Test
    public void isEnable1() throws Exception {
        Assert.assertEquals(EventBus.isEnable(NullTestEvent.class), false);
    }

    @Test
    public void register() throws Exception {
        Subscriber subscriber = new TestSubscriber();
        try {
            Assert.assertEquals(EventBus.isEnable(TestEvent.class), false);
            EventBus.register(TestEvent.class, subscriber);
            Assert.assertEquals(EventBus.isEnable(TestEvent.class), true);
        } finally {
            EventBus.unRegister(TestEvent.class, subscriber);
        }
        Assert.assertEquals(EventBus.isEnable(TestEvent.class), false);
    }

    @Test
    public void post() throws Exception {
        TestSubscriber subscriber = new TestSubscriber();
        try {
            Assert.assertEquals(EventBus.isEnable(TestEvent.class), false);
            EventBus.register(TestEvent.class, subscriber);
            Assert.assertEquals(EventBus.isEnable(TestEvent.class), true);

            EventBus.post(new NullTestEvent());
            EventBus.post(new TestEvent("xxx"));
            Assert.assertEquals(subscriber.getCache(), "xxx");
        } finally {
            EventBus.unRegister(TestEvent.class, subscriber);
        }
        Assert.assertEquals(EventBus.isEnable(TestEvent.class), false);
    }

}