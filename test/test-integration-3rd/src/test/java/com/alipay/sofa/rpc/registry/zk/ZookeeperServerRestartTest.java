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
package com.alipay.sofa.rpc.registry.zk;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.Registry;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.registry.base.BaseZkTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import com.alipay.sofa.rpc.transport.bolt.BoltClientTransport;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ZookeeperServerRestartTest extends BaseZkTest {

    /**
     * Logger for ZookeeperServerRestartTest
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperServerRestartTest.class);

    @Test
    public void testAll() throws Exception {
        final RegistryConfig registryConfig = new RegistryConfig().setProtocol(RpcConstants.REGISTRY_PROTOCOL_ZK)
            .setAddress("127.0.0.1:2181").setConnectTimeout(100);
        final ZookeeperRegistry registry = (ZookeeperRegistry) RegistryFactory
            .getRegistry(registryConfig);
        registry.start();

        final ServerConfig serverConfig2 = new ServerConfig().setPort(22111)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);
        final ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName()).setRef(new HelloServiceImpl("22"))
            .setServer(serverConfig2).setRegistry(registryConfig)
            .setRepeatedExportLimit(-1);

        providerConfig.export();

        final ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName()).setRegistry(registryConfig)
            .setTimeout(3333).setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setReconnectPeriod(60000); // avoid reconnect in this test case
        HelloService helloService = consumerConfig.refer();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                }
                // mock network disconnect cause by server force kill (kill -9) 
                BoltClientTransport clientTransport = (BoltClientTransport) consumerConfig.getConsumerBootstrap()
                    .getCluster().getConnectionHolder().getAvailableConnections().values().iterator().next();
                clientTransport.disconnect();
                serverConfig2.getServer().stop();
            }
        });
        thread.start();

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5200);
                } catch (InterruptedException e) {
                }
                serverConfig2.getServer().start();
                Registry registry1 = RegistryFactory.getRegistry(registryConfig);
                // mock server restart and register provider
                // if we don't unRegistry,create will fail,beacuse data is not clean quickly.
                registry1.unRegister(providerConfig);
                registry1.register(providerConfig);
            }
        });
        thread2.start();

        int times = 8;
        int count = 0;
        for (int i = 0; i < times; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            try {
                LOGGER.info("result is {}", helloService.sayHello("11", 1));
                count++;
            } catch (Exception e) {
                LOGGER.warn(e.getMessage());
            }
        }

        Assert.assertTrue(count >= 4);

    }
}
