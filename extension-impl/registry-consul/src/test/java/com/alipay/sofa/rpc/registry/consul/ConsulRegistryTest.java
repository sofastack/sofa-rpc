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
package com.alipay.sofa.rpc.registry.consul;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Consul Registry Tests
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class ConsulRegistryTest {

    private static final String INTERFACE_ID        = "com.alipay.sofa.rpc.registry.consul.TestService";

    private static final String CONSUL_SERVICE_NAME = "test-service";

    private ConsulProcess       consul;

    private RegistryConfig      registryConfig;

    private ConsulRegistry      registry;

    @Before
    public void setup() {
        consul = ConsulStarterBuilder.consulStarter()
            .withConsulVersion("1.4.0")
            .build()
            .start();

        registryConfig = new RegistryConfig()
            .setProtocol("consul")
            .setAddress("127.0.0.1:" + consul.getHttpPort())
            .setRegister(true);

        registry = (ConsulRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();
    }

    @After
    public void tearDown() {
        registry.destroy();
        consul.close();
        registry = null;
    }

    @Test
    public void testRegister() {
        ProviderConfig<?> providerConfig = providerConfig("consul-test-1", 12200, 12201, 12202);
        registry.register(providerConfig);

        ConsulClient consulClient = new ConsulClient("localhost:" + consul.getHttpPort());
        HealthServicesRequest request = HealthServicesRequest.newBuilder().setPassing(true).build();
        Response<List<HealthService>> healthServices = consulClient.getHealthServices(INTERFACE_ID, request);
        Assert.assertEquals(3, healthServices.getValue().size());

        registry.unRegister(providerConfig);

        healthServices = consulClient.getHealthServices(INTERFACE_ID, request);
        Assert.assertEquals(0, healthServices.getValue().size());
    }

    @Test
    public void testRegisterWithCustomName() {
        ProviderConfig<?> providerConfig = providerConfig("consul-test-1", 12200, 12201, 12202);
        providerConfig.setParameter(ConsulConstants.CONSUL_SERVICE_NAME_KEY, CONSUL_SERVICE_NAME);
        registry.register(providerConfig);

        ConsulClient consulClient = new ConsulClient("localhost:" + consul.getHttpPort());
        HealthServicesRequest request = HealthServicesRequest.newBuilder().setPassing(true).build();
        Response<List<HealthService>> healthServices = consulClient.getHealthServices(CONSUL_SERVICE_NAME, request);
        Assert.assertEquals(3, healthServices.getValue().size());

        registry.unRegister(providerConfig);

        healthServices = consulClient.getHealthServices(CONSUL_SERVICE_NAME, request);
        Assert.assertEquals(0, healthServices.getValue().size());
    }

    @Test
    public void testSubscribe() {
        ProviderConfig<?> providerConfig = providerConfig("consul-test-1", 12200, 12201, 12202);
        registry.register(providerConfig);

        ConsumerConfig<?> consumerConfig = consumerConfig("consul-test-1");
        List<ProviderGroup> providerGroups = registry.subscribe(consumerConfig);

        Assert.assertEquals(1, providerGroups.size());
        Assert.assertEquals(3, providerGroups.get(0).size());

        ConsumerConfig<?> consumerConfigWithAnotherUniqueId = consumerConfig("consul-test-2");
        providerGroups = registry.subscribe(consumerConfigWithAnotherUniqueId);
        Assert.assertEquals(1, providerGroups.size());
        Assert.assertEquals(0, providerGroups.get(0).size());

        registry.unSubscribe(consumerConfig);
    }

    @Test
    public void testSubscribeNotify() throws InterruptedException {
        ProviderConfig<?> providerConfig = providerConfig("consul-test-1", 12200);
        registry.register(providerConfig);

        ConsumerConfig<?> consumerConfig = consumerConfig("consul-test-1");
        MockProviderInfoListener listener = new MockProviderInfoListener();
        consumerConfig.setProviderInfoListener(listener);

        List<ProviderGroup> providerGroups = registry.subscribe(consumerConfig);

        Assert.assertEquals(1, providerGroups.size());
        Assert.assertEquals(1, providerGroups.get(0).size());

        CountDownLatch latch = new CountDownLatch(1);
        listener.setCountDownLatch(latch);

        providerConfig = providerConfig("consul-test-1", 12201, 12202);
        registry.register(providerConfig);

        boolean ok = latch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(ok);

        Map<String, ProviderInfo> providers = listener.getData();
        Assert.assertEquals(3, providers.size());

        latch = new CountDownLatch(1);
        listener.setCountDownLatch(latch);

        registry.unRegister(providerConfig);

        ok = latch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(ok);

        providers = listener.getData();
        Assert.assertEquals(1, providers.size());
    }

    private ConsumerConfig<?> consumerConfig(String uniqueId) {
        ConsumerConfig<?> consumer = new ConsumerConfig();
        consumer.setInterfaceId(INTERFACE_ID)
            .setUniqueId(uniqueId)
            .setApplication(new ApplicationConfig().setAppName("consul-registry-test"))
            .setProxy("javassist")
            .setSubscribe(true)
            .setSerialization("java")
            .setInvokeType("sync")
            .setTimeout(4444);

        return consumer;
    }

    private ProviderConfig<?> providerConfig(String uniqueId, int... ports) {
        ProviderConfig<?> provider = new ProviderConfig();
        provider.setInterfaceId(INTERFACE_ID)
                .setUniqueId(uniqueId)
                .setApplication(new ApplicationConfig().setAppName("consul-registry-test"))
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
                                .setHost("localhost")
                                .setPort(port)
                ).forEach(provider::setServer);
        return provider;
    }

    private static class MockProviderInfoListener implements ProviderInfoListener {

        ConcurrentHashMap<String, ProviderInfo> ps = new ConcurrentHashMap<String, ProviderInfo>();

        private CountDownLatch                  countDownLatch;

        public void setCountDownLatch(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void addProvider(ProviderGroup providerGroup) {
            for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                ps.put(providerInfo.getHost() + ":" + providerInfo.getPort(), providerInfo);
            }
        }

        @Override
        public void removeProvider(ProviderGroup providerGroup) {
            for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                ps.remove(providerInfo.getHost() + ":" + providerInfo.getPort());
            }
        }

        @Override
        public void updateProviders(ProviderGroup providerGroup) {
            ps.clear();
            for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                ps.put(providerInfo.getHost() + ":" + providerInfo.getPort(), providerInfo);
            }
            if (countDownLatch != null) {
                countDownLatch.countDown();
                countDownLatch = null;
            }
        }

        @Override
        public void updateAllProviders(List<ProviderGroup> providerGroups) {
            ps.clear();
            for (ProviderGroup providerGroup : providerGroups) {
                for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                    ps.put(providerInfo.getHost() + ":" + providerInfo.getPort(), providerInfo);
                }
            }
        }

        public Map<String, ProviderInfo> getData() {
            return ps;
        }
    }
}
