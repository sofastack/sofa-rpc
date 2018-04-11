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
package com.alipay.sofa.rpc.server.rest;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.JAXRSProviderManager;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class RestProviderTest extends ActivelyDestroyTest {

    @Test
    public void testProvider() {
        JAXRSProviderManager.registerCustomProviderInstance(new CustomerInjectorFactory());
        JAXRSProviderManager.registerCustomProviderInstance(new ClientRequestTestFilter());
        JAXRSProviderManager.registerCustomProviderInstance(new ClientRequestTestFilter2());
        JAXRSProviderManager.registerCustomProviderInstance(new ContainerRequestTestFilter());
        JAXRSProviderManager.registerCustomProviderInstance(new ContainerResponseTestFilter());
        JAXRSProviderManager.registerCustomProviderInstance(new ClientResponseTestFilter());

        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(8803)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_REST);

        ProviderConfig<RestService> providerConfig = new ProviderConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setRef(new RestServiceImpl())
            .setServer(serverConfig)
            .setBootstrap("rest")
            .setRegister(false);
        providerConfig.export();

        ConsumerConfig<RestService> consumerConfig = new ConsumerConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setDirectUrl("rest://127.0.0.1:8803")
            .setProtocol("rest")
            .setBootstrap("rest")
            .setTimeout(30000)
            .setRegister(false)
            .setFilter(Arrays.asList("-*"))
            .setApplication(new ApplicationConfig().setAppName("TestClient"));
        RestService restService = consumerConfig.refer();

        Assert.assertEquals("serverok", restService.get("ok"));

        String nameA = ClientRequestTestFilter.getName();
        String nameA2 = ClientRequestTestFilter2.getName();
        String nameB = ContainerRequestTestFilter.getName();
        String nameC = ContainerResponseTestFilter.getName();
        String nameD = ClientResponseTestFilter.getName();
        Assert.assertEquals("AtestInjecttestInject", nameA);
        Assert.assertEquals("A2", nameA2);
        Assert.assertEquals("B", nameB);
        Assert.assertEquals("C", nameC);
        Assert.assertEquals("D", nameD);

    }
}