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
package com.alipay.sofa.rpc.transport;

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.listener.ChannelListener;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ClientTransportConfigTest {

    @Test
    public void testAll() {

        ClientTransportConfig config = new ClientTransportConfig();
        config.setProviderInfo(ProviderHelper.toProviderInfo("127.0.0.1"));
        config.setContainer("xxx");
        config.setChannelListeners(Collections.<ChannelListener> singletonList(new ChannelListener() {
            @Override
            public void onConnected(AbstractChannel channel) {

            }

            @Override
            public void onDisconnected(AbstractChannel channel) {

            }
        }));
        config.setConnectionNum(22);
        config.setConnectTimeout(3333);
        config.setConsumerConfig(new ConsumerConfig());
        config.setDisconnectTimeout(4444);
        config.setInvokeTimeout(5555);
        config.setPayload(666666);
        config.setUseEpoll(true);

        Assert.assertNotNull(config.getConsumerConfig());
        Assert.assertNotNull(config.getProviderInfo());
        Assert.assertNotNull(config.getChannelListeners());
        Assert.assertEquals("xxx", config.getContainer());
        Assert.assertEquals(22, config.getConnectionNum());
        Assert.assertEquals(3333, config.getConnectTimeout());
        Assert.assertEquals(4444, config.getDisconnectTimeout());
        Assert.assertEquals(5555, config.getInvokeTimeout());
        Assert.assertEquals(666666, config.getPayload());
        Assert.assertTrue(config.isUseEpoll());
    }
}