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
package com.alipay.sofa.rpc.registry.polaris;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import static com.tencent.polaris.api.exception.ErrorCode.SERVER_USER_ERROR;

public class PolarisRegistryTest {

    private static final String    APPNAME      = "polaris-test";
    private static final String    INTERFACE_ID = "com.alipay.sofa.rpc.registry.polaris.TestService";
    private static final String    NAMESPACE    = APPNAME;
    private static final String    SERVICE      = "com.alipay.sofa.rpc.registry.polaris.TestService:1.0:polaris-test-1";
    private static final String    SERVICE_1    = "com.alipay.sofa.rpc.registry.polaris.TestService:1.0:polaris-test-2";

    private static NamingServer    polaris;
    private static RegistryConfig  registryConfig;
    private static PolarisRegistry registry;

    @BeforeClass
    static public void setup() {
        polaris = new NamingServer(8091);
        polaris.getNamingService().addService(new ServiceKey(NAMESPACE, SERVICE_1));

        try {
            polaris.start();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        registryConfig = new RegistryConfig()
            .setProtocol("polaris")
            .setAddress("127.0.0.1:8091")
            .setRegister(true);

        registry = (PolarisRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();

    }

    @AfterClass
    static public void tearDown() {
        registry.destroy();
        polaris.terminate();
    }

    @Test
    public void testRegister() {
        polaris.getNamingService().addService(new ServiceKey(NAMESPACE, SERVICE));
        //register
        ProviderConfig<?> providerConfig = providerConfig("polaris-test-1", 12200, 12201, 12202);
        registry.register(providerConfig);
        //check register
        ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPI();
        GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
        getAllInstancesRequest.setNamespace(APPNAME);
        getAllInstancesRequest.setService(SERVICE);
        InstancesResponse allInstance = consumerAPI.getAllInstance(getAllInstancesRequest);
        Assert.assertEquals(3, allInstance.getInstances().length);

        //unregister
        registry.unRegister(providerConfig);

        //check unregister ,sleep to wait remove catch
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //if no service will throw a exception
        try {
            consumerAPI.getAllInstance(getAllInstancesRequest);
        } catch (PolarisException e) {
            Assert.assertEquals(SERVER_USER_ERROR, e.getCode());
        }
    }

    @Test
    public void testSubscribe() {
        polaris.getNamingService().addService(new ServiceKey(NAMESPACE, SERVICE));

        //register
        ProviderConfig<?> providerConfig = providerConfig("polaris-test-1", 12200, 12201, 12202);
        registry.register(providerConfig);
        ConsumerConfig<?> consumerConfig = consumerConfig("polaris-test-1");
        //subscribe
        List<ProviderGroup> providerGroups = registry.subscribe(consumerConfig);
        Assert.assertEquals(1, providerGroups.size());
        Assert.assertEquals(3, providerGroups.get(0).size());

        //another consumer subscribe, no service for it
        ConsumerConfig<?> consumerConfigWithAnotherUniqueId = consumerConfig("polaris-test-2");
        providerGroups = registry.subscribe(consumerConfigWithAnotherUniqueId);
        Assert.assertEquals(1, providerGroups.size());
        Assert.assertEquals(0, providerGroups.get(0).size());

        registry.unSubscribe(consumerConfig);
        registry.unSubscribe(consumerConfigWithAnotherUniqueId);
    }

    private ProviderConfig<?> providerConfig(String uniqueId, int... ports) {
        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId(INTERFACE_ID)
                .setUniqueId(uniqueId)
                .setApplication(new ApplicationConfig().setAppName(APPNAME))
                .setProxy("javassist")
                .setRegister(true)
                .setRegistry(registryConfig)
                .setSerialization("hessian2")
                .setWeight(222)
                .setTimeout(3000);

        IntStream.of(ports)
                .mapToObj(port ->
                        new ServerConfig()
                                .setProtocol("bolt")
                                .setHost("127.0.0.1")
                                .setPort(port)
                ).forEach(provider::setServer);
        return provider;
    }

    private ConsumerConfig<?> consumerConfig(String uniqueId) {
        ConsumerConfig<?> consumer = new ConsumerConfig();
        consumer.setInterfaceId(INTERFACE_ID)
            .setUniqueId(uniqueId)
            .setApplication(new ApplicationConfig().setAppName(APPNAME))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);

        return consumer;
    }

}
