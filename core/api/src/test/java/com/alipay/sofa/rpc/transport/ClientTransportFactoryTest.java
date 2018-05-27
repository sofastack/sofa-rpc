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

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ClientTransportFactoryTest {

    @Test
    public void doNotReuseTest() {
        ClientTransportConfig config = new ClientTransportConfig();
        config.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12222))
            .setContainer("test");

        TestClientTransport clientTransport = (TestClientTransport) ClientTransportFactory.getClientTransport(config);

        ClientTransportConfig config2 = new ClientTransportConfig();
        config2.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12222))
            .setContainer("test");
        TestClientTransport clientTransport2 = (TestClientTransport) ClientTransportFactory.getClientTransport(config2);

        Assert.assertTrue(clientTransport != clientTransport2);

        ClientTransportConfig config3 = new ClientTransportConfig();
        config3.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12223))
            .setContainer("test");
        TestClientTransport clientTransport3 = (TestClientTransport) ClientTransportFactory.getClientTransport(config3);
        Assert.assertFalse(clientTransport == clientTransport3);

        ClientTransportFactory.releaseTransport(null, 500);

        clientTransport.setRequest(4);
        ClientTransportFactory.releaseTransport(clientTransport, 500);
        Assert.assertEquals(2, ClientTransportFactory.getClientTransportHolder().size());

        clientTransport2.setRequest(0);
        ClientTransportFactory.releaseTransport(clientTransport2, 500);
        Assert.assertEquals(1, ClientTransportFactory.getClientTransportHolder().size());

        ClientTransportFactory.closeAll();

        Assert.assertEquals(0, ClientTransportFactory.getClientTransportHolder().size());
    }

    @Test
    public void testReverseClientTransport() {
        ClientTransportConfig config = new ClientTransportConfig();
        config.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12222))
            .setContainer("test");
        TestClientTransport clientTransport = (TestClientTransport) ClientTransportFactory.getClientTransport(config);

        TestChannel serverChannel = new TestChannel(clientTransport.localAddress(), clientTransport.remoteAddress());

        TestClientTransport clientTransport2 = (TestClientTransport) ClientTransportFactory.
            getReverseClientTransport("test", serverChannel);

        Assert.assertEquals(serverChannel, clientTransport2.getChannel());

        String key = NetUtils.channelToString(serverChannel.remoteAddress(), serverChannel.localAddress());
        TestClientTransport clientTransport3 = (TestClientTransport)
                ClientTransportFactory.getReverseClientTransport(key);
        Assert.assertEquals(clientTransport2, clientTransport3);

        ClientTransportFactory.removeReverseClientTransport(key);
        TestClientTransport clientTransport4 = (TestClientTransport)
                ClientTransportFactory.getReverseClientTransport(key);
        Assert.assertNull(clientTransport4);

        ClientTransportFactory.closeAll();

        Assert.assertEquals(0, ClientTransportFactory.getClientTransportHolder().size());
    }

}