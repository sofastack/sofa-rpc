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
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ReusableClientTransportHolderTest {

    @Test
    public void getClientTransport() {
        ReusableClientTransportHolder holder = new ReusableClientTransportHolder();
        ClientTransportConfig config = new ClientTransportConfig();
        config.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12222))
            .setContainer("test");

        TestClientTransport clientTransport = (TestClientTransport) holder.getClientTransport(config);

        ClientTransportConfig config2 = new ClientTransportConfig();
        config2.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12222))
            .setContainer("test");
        TestClientTransport clientTransport2 = (TestClientTransport) holder.getClientTransport(config2);
        Assert.assertTrue(clientTransport == clientTransport2);

        ClientTransportConfig config3 = new ClientTransportConfig();
        config3.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12223))
            .setContainer("test");
        TestClientTransport clientTransport3 = (TestClientTransport) holder.getClientTransport(config3);
        Assert.assertFalse(clientTransport == clientTransport3);

        Assert.assertFalse(holder.removeClientTransport(null));

        clientTransport.setRequest(4);
        Assert.assertFalse(holder.removeClientTransport(clientTransport));
        Assert.assertEquals(2, holder.size());

        clientTransport2.setRequest(0);
        holder.removeClientTransport(clientTransport2);
        Assert.assertEquals(1, holder.size());

        holder.destroy();

        Assert.assertEquals(0, holder.size());
    }
}