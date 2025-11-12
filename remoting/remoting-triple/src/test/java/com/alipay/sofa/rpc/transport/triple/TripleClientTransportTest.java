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
package com.alipay.sofa.rpc.transport.triple;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.server.triple.HelloService;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import com.alipay.sofa.rpc.transport.ClientTransportFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author junyuan
 * @version TripleClientTransportTest.java, v 0.1 2024-08-01 17:12 junyuan Exp $
 */
public class TripleClientTransportTest {

    @Test
    public void testInit() {

        Assert.assertEquals(TripleClientTransport.KEEP_ALIVE_INTERVAL, 0);

    }

    @Test
    public void test() {
        //模拟两个 reference 去消费同一份推送数据
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.setHost("127.0.0.1");
        providerInfo.setPort(55555);

        ConsumerConfig<?> consumerConfig1 = new ConsumerConfig<>();
        consumerConfig1.setProtocol("tri");
        consumerConfig1.setInterfaceId(HelloService.class.getName());
        ClientTransportConfig config1 = providerToClientConfig(consumerConfig1, providerInfo);
        TripleClientTransport clientTransport1 = (TripleClientTransport) ClientTransportFactory.getClientTransport(config1);

        ConsumerConfig<?> consumerConfig2 = new ConsumerConfig<>();
        consumerConfig2.setProtocol("tri");
        consumerConfig2.setInterfaceId(HelloService.class.getName());
        ClientTransportConfig config2 = providerToClientConfig(consumerConfig2, providerInfo);
        TripleClientTransport clientTransport2 = (TripleClientTransport) ClientTransportFactory.getClientTransport(config2);

        Assert.assertNotNull(TripleClientTransport.channelMap.get("127.0.0.1:55555"));
        Assert.assertTrue(clientTransport1.isAvailable());
        Assert.assertTrue(clientTransport2.isAvailable());
        Assert.assertEquals(clientTransport1.channel, clientTransport2.channel);

        clientTransport1.destroy();
        Assert.assertNull(clientTransport1.channel);
        Assert.assertFalse(clientTransport1.isAvailable());
        Assert.assertTrue(clientTransport2.isAvailable());
        Assert.assertNotNull(TripleClientTransport.channelMap.get("127.0.0.1:55555"));

        clientTransport1.connect();
        Assert.assertTrue(clientTransport1.isAvailable());
        Assert.assertEquals(clientTransport1.channel, clientTransport2.channel);

        clientTransport2.destroy();
        Assert.assertNull(clientTransport2.channel);
        Assert.assertTrue(clientTransport1.isAvailable());
        Assert.assertFalse(clientTransport2.isAvailable());
        Assert.assertNotNull(TripleClientTransport.channelMap.get("127.0.0.1:55555"));

        clientTransport1.destroy();
        Assert.assertNull(clientTransport1.channel);
        Assert.assertFalse(clientTransport1.isAvailable());
        Assert.assertFalse(clientTransport2.isAvailable());
        Assert.assertNull(TripleClientTransport.channelMap.get("127.0.0.1:55555"));
    }

    private ClientTransportConfig providerToClientConfig(ConsumerConfig<?> consumerConfig, ProviderInfo providerInfo) {
        return new ClientTransportConfig()
            .setConsumerConfig(consumerConfig)
            .setProviderInfo(providerInfo)
            .setContainer(consumerConfig.getProtocol())
            .setConnectTimeout(consumerConfig.getConnectTimeout())
            .setInvokeTimeout(consumerConfig.getTimeout())
            .setDisconnectTimeout(consumerConfig.getDisconnectTimeout())
            .setConnectionNum(consumerConfig.getConnectionNum())
            .setChannelListeners(consumerConfig.getOnConnect());
    }
}