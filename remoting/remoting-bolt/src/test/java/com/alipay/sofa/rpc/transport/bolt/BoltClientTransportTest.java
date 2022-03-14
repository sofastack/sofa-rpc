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
package com.alipay.sofa.rpc.transport.bolt;

import com.alipay.remoting.exception.ConnectionClosedException;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.exception.InvokeSendFailedException;
import com.alipay.remoting.rpc.exception.InvokeServerBusyException;
import com.alipay.remoting.rpc.exception.InvokeServerException;
import com.alipay.remoting.rpc.exception.InvokeTimeoutException;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import com.alipay.sofa.rpc.transport.ClientTransportFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltClientTransportTest extends ActivelyDestroyTest {

    @Test
    public void doReuseTest() {

        ServerConfig serverConfig = new ServerConfig().setPort(12222).setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        serverConfig.buildIfAbsent().start();

        ServerConfig serverConfig2 = new ServerConfig().setPort(12223).setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        serverConfig2.buildIfAbsent().start();

        ClientTransportConfig clientTransportConfig = new ClientTransportConfig();
        ProviderInfo providerInfo = ProviderHelper.toProviderInfo("127.0.0.1:12222");
        clientTransportConfig.setProviderInfo(providerInfo);

        BoltClientTransport clientTransport = new BoltClientTransport(clientTransportConfig);
        clientTransport.disconnect();
        final BoltClientConnectionManager connectionManager = BoltClientTransport.connectionManager;

        if (connectionManager instanceof ReuseBoltClientConnectionManager) {
            Assert.assertTrue(((ReuseBoltClientConnectionManager) connectionManager).urlConnectionMap.size() == 0);
            Assert.assertTrue(((ReuseBoltClientConnectionManager) connectionManager).connectionRefCounter.size() == 0);
        } else {
            Assert.fail();
        }

        ClientTransportConfig config1 = new ClientTransportConfig();
        config1.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12222))
            .setContainer("bolt");

        BoltClientTransport clientTransport1 = (BoltClientTransport) ClientTransportFactory
            .getClientTransport(config1);
        clientTransport1.connect();

        ClientTransportConfig config2 = new ClientTransportConfig();
        config2.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12222))
            .setContainer("bolt");
        BoltClientTransport clientTransport2 = (BoltClientTransport) ClientTransportFactory
            .getClientTransport(config2);
        clientTransport2.connect();
        Assert.assertFalse(clientTransport1 == clientTransport2);
        Assert.assertTrue(clientTransport1.fetchConnection() == clientTransport2.fetchConnection());

        ClientTransportConfig config3 = new ClientTransportConfig();
        config3.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12223))
            .setContainer("bolt");
        BoltClientTransport clientTransport3 = (BoltClientTransport) ClientTransportFactory
            .getClientTransport(config3);
        clientTransport3.connect();
        Assert.assertFalse(clientTransport1 == clientTransport3);
        Assert.assertFalse(clientTransport1.fetchConnection() == clientTransport3.fetchConnection());

        ClientTransportFactory.releaseTransport(null, 500);

        clientTransport1.currentRequests.set(4);
        ClientTransportFactory.releaseTransport(clientTransport1, 500);

        clientTransport2.currentRequests.set(0);
        ClientTransportFactory.releaseTransport(clientTransport2, 500);

        ClientTransportFactory.closeAll();
    }

    @Test
    public void testConvertToRpcException() {
        ClientTransportConfig config1 = new ClientTransportConfig();
        config1.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12222))
            .setContainer("bolt");
        BoltClientTransport transport = new BoltClientTransport(config1);
        Assert.assertTrue(transport
            .convertToRpcException(new SofaRpcException(RpcErrorType.CLIENT_UNDECLARED_ERROR, ""))
            instanceof SofaRpcException);
        Assert.assertTrue(transport.convertToRpcException(new InvokeTimeoutException())
            instanceof SofaTimeOutException);
        Assert.assertTrue(transport.convertToRpcException(new InvokeServerBusyException())
            .getErrorType() == RpcErrorType.SERVER_BUSY);
        Assert.assertTrue(transport.convertToRpcException(new SerializationException("xx", true))
            .getErrorType() == RpcErrorType.SERVER_SERIALIZE);
        Assert.assertTrue(transport.convertToRpcException(new SerializationException("xx", false))
            .getErrorType() == RpcErrorType.CLIENT_SERIALIZE);
        Assert.assertTrue(transport.convertToRpcException(new DeserializationException("xx", true))
            .getErrorType() == RpcErrorType.SERVER_DESERIALIZE);
        Assert.assertTrue(transport.convertToRpcException(new DeserializationException("xx", false))
            .getErrorType() == RpcErrorType.CLIENT_DESERIALIZE);
        Assert.assertTrue(transport.convertToRpcException(new ConnectionClosedException())
            .getErrorType() == RpcErrorType.CLIENT_NETWORK);
        Assert.assertTrue(transport.convertToRpcException(new InvokeSendFailedException())
            .getErrorType() == RpcErrorType.CLIENT_NETWORK);
        Assert.assertTrue(transport.convertToRpcException(new InvokeServerException())
            .getErrorType() == RpcErrorType.SERVER_UNDECLARED_ERROR);
        Assert.assertTrue(transport.convertToRpcException(new UnsupportedOperationException())
            .getErrorType() == RpcErrorType.CLIENT_UNDECLARED_ERROR);
    }

    @Test
    public void testNotSupport() {
        ClientTransportConfig config1 = new ClientTransportConfig();
        config1.setProviderInfo(new ProviderInfo().setHost("127.0.0.1").setPort(12222))
            .setContainer("bolt");
        BoltClientTransport transport = new BoltClientTransport(config1);
        try {
            transport.setChannel(null);
            Assert.fail();
        } catch (Exception e) {

        }
        try {
            transport.getChannel();
            Assert.fail();
        } catch (Exception e) {

        }
        try {
            transport.receiveRpcResponse(null);
            Assert.fail();
        } catch (Exception e) {

        }
        try {
            transport.handleRpcRequest(null);
            Assert.fail();
        } catch (Exception e) {

        }

    }
}