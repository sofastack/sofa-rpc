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
package com.alipay.sofa.rpc.module;

import com.alipay.lookout.api.Lookout;
import com.alipay.lookout.api.NoopRegistry;
import com.alipay.lookout.api.Registry;
import com.alipay.lookout.core.DefaultRegistry;
import com.alipay.lookout.core.InfoWrapper;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.metrics.lookout.RpcLookoutId;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:zhiyuan.lzy@antfin.com">zhiyuan.lzy</a>
 */
public class ConsumerSubTest {

    @Test
    public void testSubLookout() {

        Registry registry = new DefaultRegistry();

        if (Lookout.registry() == NoopRegistry.INSTANCE) {
            Lookout.setRegistry(registry);
        }

        LookoutModule lookoutModule = new LookoutModule();
        Assert.assertEquals(true, lookoutModule.needLoad());

        lookoutModule.install();
        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setInterfaceId("a");
        EventBus.post(new ConsumerSubEvent(consumerConfig));

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        RpcLookoutId rpcLookoutId = new RpcLookoutId();
        InfoWrapper result = Lookout.registry().get(rpcLookoutId.fetchConsumerSubId());
        final Object value = result.value();
        Assert.assertTrue(value instanceof ConsumerConfig);

        consumerConfig = (ConsumerConfig) value;

        Assert.assertEquals("a", consumerConfig.getInterfaceId());
    }

}