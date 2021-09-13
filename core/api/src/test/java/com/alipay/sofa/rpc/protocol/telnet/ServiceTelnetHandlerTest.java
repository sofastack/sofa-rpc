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
package com.alipay.sofa.rpc.protocol.telnet;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.protocol.*;
import com.alipay.sofa.rpc.protocol.service.EchoService;
import com.alipay.sofa.rpc.protocol.service.EchoServiceImpl;
import com.alipay.sofa.rpc.protocol.service.HelloService;
import com.alipay.sofa.rpc.protocol.service.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceTelnetHandlerTest {
    @Before
    public void before() {
        register();

    }

    @Test
    public void getCommand() throws Exception {
        Assert.assertEquals(new ServiceTelnetHandler().getCommand(), "service");

    }

    @Test
    public void telnet() throws Exception {

        Assert.assertNotNull(new ServiceTelnetHandler().telnet("service"));
        Assert.assertNotNull(new ServiceTelnetHandler().telnet("service com.alipay.sofa.rpc.protocol.HelloService"));
        Assert.assertNotNull(new ServiceTelnetHandler().telnet("service a"));
        Assert.assertNotNull(new ServiceTelnetHandler().telnet("service com.alipay.sofa.rpc.protocol.EchoService"));
    }

    @Test
    public void getDescription() throws Exception {
        Assert.assertNotNull(new ServiceTelnetHandler().getDescription());
    }

    public void register() {
        ProviderConfigRepository providerConfigRepository = ProviderConfigRepository.getProviderConfigRepository();
        ApplicationConfig application = new ApplicationConfig().setAppName("test-server");

        ServerConfig serverConfig = new ServerConfig()
            .setPort(22000)
            .setDaemon(false);

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(application)
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);

        ProviderConfig<EchoService> providerConfig2 = new ProviderConfig<EchoService>()
            .setInterfaceId(EchoService.class.getName())
            .setApplication(application)
            .setRef(new EchoServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);

        providerConfigRepository.addProviderConfig(providerConfig);
        providerConfigRepository.addProviderConfig(providerConfig2);

        ApplicationConfig application2 = new ApplicationConfig().setAppName("test-client");

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setApplication(application2)
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22000")
            .setRegister(false)
            .setTimeout(3000);
        HelloService helloService = consumerConfig.refer();

        ConsumerConfigRepository consumerConfigRepository = ConsumerConfigRepository.getConsumerConfigRepository();
        consumerConfigRepository.addConsumerConfig(consumerConfig);
    }
}
