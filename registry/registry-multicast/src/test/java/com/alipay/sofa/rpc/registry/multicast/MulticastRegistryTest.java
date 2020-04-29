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
package com.alipay.sofa.rpc.registry.multicast;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.multicast.api.HelloService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.InetAddress;

/**
 * @author zhaowang
 * @version : MulticastRegistryTest.java, v 0.1 2020年03月04日 9:33 下午 zhaowang Exp $
 */
public class MulticastRegistryTest {

    // 非守护线程
    public static final ServerConfig SERVER_CONFIG = new ServerConfig()
            .setProtocol("bolt") // 设置一个协议，默认bolt
            .setPort(12200) // 设置一个端口，默认12200
            .setDaemon(false);
    public static final ProviderConfig PROVIDER_CONFIG = new ProviderConfig<>()
            .setInterfaceId(HelloService.class.getName())
            .setServer(SERVER_CONFIG);
    public static final ConsumerConfig CONSUMER_CONFIG = new ConsumerConfig<>()
            .setInterfaceId(HelloService.class.getName())
            .setProtocol("bolt");

    @Test
    public void testInit() throws NoSuchFieldException, IllegalAccessException {
        RegistryConfig registryConfig = new RegistryConfig()
            .setProtocol("multicast")
            .setAddress("224.5.6.7:6666");
        MulticastRegistry multicastRegistry = new MulticastRegistry(registryConfig);
        multicastRegistry.init();

        Assert.assertEquals("224.5.6.7", getAddress(multicastRegistry));
        Assert.assertEquals(6666, getPort(multicastRegistry));
    }

    private int getPort(MulticastRegistry multicastRegistry) throws NoSuchFieldException, IllegalAccessException {
        Field multicastPort = MulticastRegistry.class.getDeclaredField("multicastPort");
        multicastPort.setAccessible(true);
        Object o = multicastPort.get(multicastRegistry);
        if (o instanceof Number) {
            int port = (int) o;
            return port;
        }
        throw new RuntimeException("getPort error");
    }

    private String getAddress(MulticastRegistry multicastRegistry) throws NoSuchFieldException, IllegalAccessException {
        Field multicastAddress = MulticastRegistry.class.getDeclaredField("multicastAddress");
        multicastAddress.setAccessible(true);
        Object o = multicastAddress.get(multicastRegistry);
        if (o instanceof InetAddress) {
            String hostName = ((InetAddress) o).getHostName();
            return hostName;
        }
        throw new RuntimeException("getAddress error");
    }

    @Test
    public void testSubAndUnsub() throws InterruptedException {
        RegistryConfig registryConfig = new RegistryConfig()
                .setProtocol("multicast")
                .setAddress("224.5.6.7:6667");
        MulticastRegistry server = new MulticastRegistry(registryConfig);
        server.init();
        MulticastRegistry client = new MulticastRegistry(registryConfig);
        client.init();


        server.register(PROVIDER_CONFIG);
        Thread.sleep(3000);

        ProviderGroup providerGroup = client.getAllProviderCache().get(MulticastRegistryHelper.buildListDataId(PROVIDER_CONFIG, SERVER_CONFIG.getProtocol()));
        Assert.assertFalse(providerGroup.isEmpty());

        server.unRegister(PROVIDER_CONFIG);

        Thread.sleep(3000);
        providerGroup = client.getAllProviderCache().get(MulticastRegistryHelper.buildListDataId(PROVIDER_CONFIG, SERVER_CONFIG.getProtocol()));
        Assert.assertTrue(providerGroup.isEmpty());


    }

    @Test
    public void testSub() throws InterruptedException {
        RegistryConfig registryConfig = new RegistryConfig()
                .setProtocol("multicast")
                .setAddress("224.5.6.7:6668");
        MulticastRegistry server = new MulticastRegistry(registryConfig);
        server.init();
        server.register(PROVIDER_CONFIG);
        MulticastRegistry client = new MulticastRegistry(registryConfig);
        client.init();

        ProviderGroup providerGroup = client.getAllProviderCache().get(MulticastRegistryHelper.buildListDataId(PROVIDER_CONFIG, SERVER_CONFIG.getProtocol()));
        Assert.assertTrue(providerGroup == null);
        client.subscribe(CONSUMER_CONFIG);
        Thread.sleep(3000);
        ProviderGroup providerGroup1 = client.getAllProviderCache().get(MulticastRegistryHelper.buildListDataId(PROVIDER_CONFIG, SERVER_CONFIG.getProtocol()));
        Assert.assertFalse(providerGroup1.isEmpty());
    }



}