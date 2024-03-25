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
package com.alipay.sofa.rpc.registry.kubernetes;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.alipay.sofa.rpc.registry.kubernetes.KubernetesRegistryHelper.buildDataId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KubernetesRegistryTest {

    private static final String NAMESPACE = "TestNameSpace";
    private static final String POD_NAME = "TestPodName";
    private static final String APP_NAME = "TestAppName";
    private static final String SERVICE_NAME = "TestService";

    public KubernetesServer mockServer;

    private NamespacedKubernetesClient mockClient;

    private static KubernetesRegistry kubernetesRegistry;

    private static RegistryConfig registryConfig;

    private static ConsumerConfig<?> consumer;

    /**
     * Ad before class.
     */
    @BeforeClass
    public static void adBeforeClass() {
        RpcRunningState.setUnitTestMode(true);
    }

    /**
     * Ad after class.
     */
    @AfterClass
    public static void adAfterClass() {
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
    }

    @Before
    public void setup() {
        mockServer = new KubernetesServer(false, true);
        mockServer.before();
        mockClient = mockServer.getClient().inNamespace(NAMESPACE);

        registryConfig = new RegistryConfig();
        registryConfig.setProtocol("kubernetes");
        registryConfig.setAddress(mockClient.getConfiguration().getMasterUrl());
        //        registryConfig.setParameter("trustCerts", "true");
        registryConfig.setParameter("namespace", NAMESPACE);
        registryConfig.setParameter("useHttps", "false");
        registryConfig.setParameter("http2Disable", "true");

        kubernetesRegistry = new KubernetesRegistry(registryConfig);
        kubernetesRegistry.init();
        kubernetesRegistry.setCurrentHostname(POD_NAME);

        System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
        System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");

        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName(POD_NAME)
                .endMetadata()
                .withNewStatus()
                .withPodIP("192.168.1.100")
                .endStatus()
                .build();

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(SERVICE_NAME)
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .build();

        Endpoints endPoints = new EndpointsBuilder()
                .withNewMetadata()
                .withName(SERVICE_NAME)
                .endMetadata()
                .addNewSubset()
                .addNewAddress()
                .withIp("ip1")
                .withNewTargetRef()
                .withUid("uid1")
                .withName(POD_NAME)
                .endTargetRef()
                .endAddress()
                .addNewPort("Test", "Test", 12345, "TCP")
                .endSubset()
                .build();

        mockClient.pods().inNamespace(NAMESPACE).create(pod);
        mockClient.services().inNamespace(NAMESPACE).create(service);
        mockClient.endpoints().inNamespace(NAMESPACE).create(endPoints);

        Assert.assertTrue(kubernetesRegistry.start());
    }

    @After
    public void cleanup() {
        kubernetesRegistry.destroy();
        mockClient.close();
        mockServer.after();
    }

    @Test
    public void testAll() throws InterruptedException {
        ApplicationConfig applicationConfig = new ApplicationConfig()
                .setAppName(APP_NAME);

        ServerConfig serverConfig1 = new ServerConfig()
                .setProtocol("bolt")
                .setPort(12200)
                .setDaemon(false);

        ProviderConfig<TestService> providerConfig1 = new ProviderConfig<TestService>()
                .setApplication(applicationConfig)
                .setInterfaceId(TestService.class.getName())
                .setRegistry(registryConfig)
                .setRegister(true)
                //                .setUniqueId("standalone")
                .setRef(new TestServiceImpl())
                .setDelay(20)
                .setServer(serverConfig1);

        // 注册第一个providerConfig1
        kubernetesRegistry.register(providerConfig1);

        ServerConfig serverConfig2 = new ServerConfig()
                .setProtocol("h2c")
                .setPort(12202)
                .setDaemon(false);

        ProviderConfig<TestService2> providerConfig2 = new ProviderConfig<TestService2>()
                .setApplication(applicationConfig)
                .setInterfaceId(TestService2.class.getName())
                .setRegistry(registryConfig)
                .setRegister(true)
                //                .setUniqueId("standalone")
                .setRef(new TestServiceImpl2())
                .setDelay(20)
                .setServer(serverConfig2);

        // 注册第二个providerConfig2
        kubernetesRegistry.register(providerConfig2);

        List<Pod> items = mockClient.pods().inNamespace(NAMESPACE).list().getItems();

        Assert.assertEquals(1, items.size());
        Pod pod = items.get(0);
        String annotationBolt = pod.getMetadata().getAnnotations().get(buildDataId(providerConfig1, "bolt"));
        Assert.assertNotNull(annotationBolt);
        String annotationH2c = pod.getMetadata().getAnnotations().get(buildDataId(providerConfig2, "h2c"));
        Assert.assertNotNull(annotationH2c);

        // 订阅
        consumer = new ConsumerConfig();
        consumer.setInterfaceId("com.alipay.sofa.rpc.registry.kubernetes.TestService")
                .setApplication(applicationConfig)
                .setProxy("javassist")
                .setSubscribe(true)
                .setSerialization("java")
                .setInvokeType("sync")
                .setTimeout(4444);

        CountDownLatch latch = new CountDownLatch(1);
        MockProviderInfoListener providerInfoListener = new MockProviderInfoListener();
        providerInfoListener.setCountDownLatch(latch);
        consumer.setProviderInfoListener(providerInfoListener);
        List<ProviderGroup> all = kubernetesRegistry.subscribe(consumer);
        providerInfoListener.updateAllProviders(all);
        latch.await(5000, TimeUnit.MILLISECONDS);
        Map<String, ProviderGroup> ps = providerInfoListener.getData();

        Assert.assertEquals(1, kubernetesRegistry.getConsumerListeners().size());
        Assert.assertTrue(ps.size() > 0);
        Assert.assertEquals(1, ps.size());
        Assert.assertNotNull(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertTrue(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size() > 0);

        // 一次发2个端口的再次注册
        latch = new CountDownLatch(2);
        providerInfoListener.setCountDownLatch(latch);
        ServerConfig serverConfig = new ServerConfig()
                .setProtocol("bolt")
                .setHost("0.0.0.0")
                .setDaemon(false)
                .setPort(12201);
        providerConfig1.getServer().add(serverConfig);
        kubernetesRegistry.register(providerConfig1);
        latch.await(5000 * 2, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ps.size() > 0);
        Assert.assertNotNull(ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP));
        Assert.assertEquals(1, ps.get(RpcConstants.ADDRESS_DEFAULT_GROUP).size());

        // 反订阅
        kubernetesRegistry.unSubscribe(consumer);
        Assert.assertEquals(0, kubernetesRegistry.getConsumerListeners().size());

        // 反注册providerConfig1
        kubernetesRegistry.unRegister(providerConfig1);
        // 反注册providerConfig2
        kubernetesRegistry.unRegister(providerConfig2);

        List<Pod> unRegisterItems = mockClient.pods().inNamespace(NAMESPACE).list().getItems();
        Assert.assertEquals(0, unRegisterItems.get(0).getMetadata().getAnnotations().size());
    }

    private static class MockProviderInfoListener implements ProviderInfoListener {

        Map<String, ProviderGroup> providerGroupMap = new HashMap<>();

        private CountDownLatch countDownLatch;

        public void setCountDownLatch(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void addProvider(ProviderGroup providerGroup) {

        }

        @Override
        public void removeProvider(ProviderGroup providerGroup) {

        }

        @Override
        public void updateProviders(ProviderGroup providerGroup) {

            providerGroupMap.put(providerGroup.getName(), providerGroup);
            if (countDownLatch != null) {
                countDownLatch.countDown();
                countDownLatch = null;
            }
        }

        @Override
        public void updateAllProviders(List<ProviderGroup> providerGroups) {
            providerGroupMap.clear();

            if (providerGroups == null || providerGroups.size() == 0) {
            } else {
                for (ProviderGroup p : providerGroups) {
                    providerGroupMap.put(p.getName(), p);
                }

            }
        }

        public Map<String, ProviderGroup> getData() {
            return providerGroupMap;
        }
    }

    @Test
    public void testUpdatePodAnnotations() {

        // 创建一个新的 Pod
        String podName = "test-pod";
        String namespace = "test-namespace";
        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .withNamespace(namespace)
                .endMetadata()
                .build();

        // 在模拟环境中创建 Pod
        pod = mockClient.pods().inNamespace(namespace).create(pod);
        assertNotNull(pod);

        // 准备要更新的 annotations
        Map<String, String> annotations = new HashMap<>();
        annotations.put("example.com/annotation", "value");

        // 更新 Pod 的 annotations
        pod = new PodBuilder(pod)
                .editMetadata()
                .addToAnnotations(annotations)
                .endMetadata()
                .build();

        // 在模拟环境中更新 Pod
        pod = mockClient.pods().inNamespace(namespace).withName(podName).replace(pod);

        // 获取并验证 annotations 是否已更新
        assertEquals("value", pod.getMetadata().getAnnotations().get("example.com/annotation"));
    }
}