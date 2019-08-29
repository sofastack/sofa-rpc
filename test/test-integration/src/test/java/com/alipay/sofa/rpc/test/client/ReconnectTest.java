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
package com.alipay.sofa.rpc.test.client;

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.server.ServerFactory;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import com.alipay.sofa.rpc.test.TestUtils;
import com.alipay.sofa.rpc.transport.bolt.BoltClientTransport;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ReconnectTest extends ActivelyDestroyTest {

    @Test
    public void testReconnect() throws Exception {
        ServerConfig serverConfig1 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22221)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig1)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig.export();

        final ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22221")
            .setConnectionHolder("all")
            .setRegister(false)
            .setLazy(true)
            .setReconnectPeriod(2000)
            .setTimeout(3000);
        HelloService helloService = consumerConfig.refer();
        Assert.assertNotNull(helloService.sayHello("xxx", 11));

        // Mock server down, and RPC will throw exception(no available provider)
        providerConfig.unExport();
        ServerFactory.destroyAll();

        BoltClientTransport clientTransport = (BoltClientTransport) consumerConfig.getConsumerBootstrap().getCluster()
            .getConnectionHolder()
            .getAvailableClientTransport(ProviderHelper.toProviderInfo("bolt://127.0.0.1:22221"));

        clientTransport.disconnect();

        TestUtils.delayGet(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return consumerConfig.getConsumerBootstrap().getCluster().getConnectionHolder().isAvailableEmpty();
            }
        }, true, 100, 30);

        try {
            helloService.sayHello("xxx", 11);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(LogCodes.ERROR_TARGET_URL_INVALID));
        }

        // Mock server restart
        serverConfig1 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22221)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        providerConfig.setServer(Arrays.asList(serverConfig1)).export();
        // The consumer will reconnect to provider automatically
        TestUtils.delayGet(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return consumerConfig.getConsumerBootstrap().getCluster().getConnectionHolder().isAvailableEmpty();
            }
        }, false, 100, 30);
        // RPC return success
        Assert.assertNotNull(helloService.sayHello("xxx", 11));
    }
}