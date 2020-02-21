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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.proxy.ProxyFactory;
import com.alipay.sofa.rpc.registry.base.BaseZkTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * 弹性长连接，可按百分比配置以及按个数配置
 *
 * @author <a href=mailto:liangyuanpengem@163.com>LiangYuanPeng</a>
 */
public class ElasticConnectConnectionHolderTest extends BaseZkTest {

    private static ServerConfig   serverConfig1;
    private static ServerConfig   serverConfig2;
    private static ServerConfig   serverConfig3;
    private static ServerConfig   serverConfig4;
    private static RegistryConfig registryConfig;

    @BeforeClass
    public static void startServer() throws Exception {

        registryConfig = new RegistryConfig()
            .setProtocol(RpcConstants.REGISTRY_PROTOCOL_ZK)
            .setAddress("127.0.0.1:2181");

        serverConfig1 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22223);
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig1)
            .setRepeatedExportLimit(-1)
            .setRegistry(registryConfig);
        providerConfig.export();

        serverConfig2 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22224);
        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig2)
            .setRepeatedExportLimit(-1)
            .setRegistry(registryConfig);
        providerConfig2.export();

        serverConfig3 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22225);
        ProviderConfig<HelloService> providerConfig3 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig3)
            .setRepeatedExportLimit(-1)
            .setRegistry(registryConfig);
        providerConfig3.export();

        serverConfig4 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22226);
        ProviderConfig<HelloService> providerConfig4 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig4)
            .setRepeatedExportLimit(-1)
            .setRegistry(registryConfig);
        providerConfig4.export();
    }

    @AfterClass
    public static void stopServer() {
        serverConfig1.destroy();
        serverConfig2.destroy();
        serverConfig3.destroy();
        serverConfig4.destroy();
    }

    @Test
    public void testConnectElastic() throws Exception {

        //please change the connect.elastic.size or connect.elastic.precent in the rpc-config.json
        //use connect.elastic.precent first unless connect.elastic.precent = 0

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setConnectionHolder("elastic")
            .setRegistry(registryConfig);
        HelloService helloService = consumerConfig.refer();

        ClientProxyInvoker invoker = (ClientProxyInvoker) ProxyFactory.getInvoker(helloService,
            consumerConfig.getProxy());
        Cluster cluster = invoker.getCluster();
        Assert.assertTrue(cluster.getConnectionHolder() instanceof ElasticConnectionHolder);
        ElasticConnectionHolder holder = (ElasticConnectionHolder) cluster.getConnectionHolder();
        Assert.assertTrue(!holder.isAvailableEmpty());
        TimeUnit.SECONDS.sleep(3);

        Assert.assertEquals(holder.getAvailableConnections().size(), 4);

    }
}