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
package com.alipay.sofa.rpc.metrics.lookout;

import com.alipay.lookout.api.Id;
import com.alipay.lookout.api.Lookout;
import com.alipay.lookout.api.Measurement;
import com.alipay.lookout.api.Metric;
import com.alipay.lookout.api.NoopRegistry;
import com.alipay.lookout.api.Registry;
import com.alipay.lookout.api.Tag;
import com.alipay.lookout.core.DefaultRegistry;
import com.alipay.sofa.rpc.api.future.SofaResponseFuture;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.MethodConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.module.ModuleFactory;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
public class RpcLookoutTest extends ActivelyDestroyTest {

    static Field                           corePoolSize;
    static Field                           maxPoolSize;
    static Field                           queueSize;

    private ServerConfig                   serverConfig;

    private ProviderConfig<LookoutService> providerConfig;

    private ConsumerConfig<LookoutService> consumerConfig;

    private LookoutService                 lookoutService;

    private CountSofaResponseCallback      onReturn;

    @BeforeClass
    public static void beforeClass() {

        RpcRunningState.setUnitTestMode(false);

        try {
            Class clazz = RpcLookout.class;
            Class[] innerClazzs = clazz.getDeclaredClasses();
            for (Class cls : innerClazzs) {
                if (cls.getName().contains("ThreadPoolConfig")) {
                    corePoolSize = cls.getDeclaredField("corePoolSize");
                    corePoolSize.setAccessible(true);

                    maxPoolSize = cls.getDeclaredField("maxPoolSize");
                    maxPoolSize.setAccessible(true);

                    queueSize = cls.getDeclaredField("queueSize");
                    queueSize.setAccessible(true);
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        Registry registry = new DefaultRegistry();
        final Registry currentRegistry = Lookout.registry();
        if (currentRegistry == NoopRegistry.INSTANCE) {
            Lookout.setRegistry(registry);
        } else {
            //clear all metrics now
            Iterator<Metric> itar = currentRegistry.iterator();
            while (itar.hasNext()) {
                Metric metric = itar.next();
                Id id = metric.id();
                currentRegistry.removeMetric(id);

            }
        }
    }

    @AfterClass
    public static void adAfterClass() {
        RpcRunningState.setUnitTestMode(true);
        ActivelyDestroyTest.adAfterClass();
    }

    private Metric fetchWithName(String name) {
        Registry registry = Lookout.registry();
        System.out.println("current registry is " + registry + ",name=" + name);
        for (Metric metric : registry) {
            System.out.println("metrics name is " + metric.id().name());
            if (metric.id().name().equalsIgnoreCase(name)) {
                return metric;
            }
        }
        return null;
    }

    /**
     * invoke
     */
    @Before
    public void before() {

        serverConfig = new ServerConfig()
            .setPort(12201)
            .setCoreThreads(30)
            .setMaxThreads(500)
            .setQueues(600)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT);

        providerConfig = new ProviderConfig<LookoutService>()
            .setInterfaceId(LookoutService.class.getName())
            .setRef(new LookoutServiceImpl())
            .setServer(serverConfig)
            .setBootstrap("bolt")
            .setRegister(false)
            .setApplication(new ApplicationConfig().setAppName("TestLookOutServer"));
        providerConfig.export();

        MethodConfig methodConfigFuture = new MethodConfig()
            .setName("sayFuture")
            .setInvokeType("future");
        onReturn = new CountSofaResponseCallback();
        MethodConfig methodConfigCallback = new MethodConfig()
            .setName("sayCallback")
            .setInvokeType("callback")
            .setOnReturn(onReturn);
        MethodConfig methodConfigOneway = new MethodConfig()
            .setName("sayOneway")
            .setInvokeType("oneway");
        List<MethodConfig> methodConfigs = new ArrayList<MethodConfig>();
        methodConfigs.add(methodConfigFuture);
        methodConfigs.add(methodConfigCallback);
        methodConfigs.add(methodConfigOneway);

        consumerConfig = new ConsumerConfig<LookoutService>()
            .setInterfaceId(LookoutService.class.getName())
            .setProtocol("bolt")
            .setBootstrap("bolt")
            .setMethods(methodConfigs)
            .setTimeout(3000)
            .setRegister(false)
            .setDirectUrl("bolt://127.0.0.1:12201?appName=TestLookOutServer")
            .setApplication(new ApplicationConfig().setAppName("TestLookOutClient"));
        lookoutService = consumerConfig.refer();

    }

    @After
    public void after() {

        final Registry currentRegistry = Lookout.registry();
        //clear all metrics now
        Iterator<Metric> itar = currentRegistry.iterator();
        while (itar.hasNext()) {
            Metric metric = itar.next();
            Id id = metric.id();
            currentRegistry.removeMetric(id);
        }

        if (serverConfig != null) {
            serverConfig.destroy();
        }
        if (providerConfig != null) {
            providerConfig.unExport();
        }
        if (consumerConfig != null) {
            consumerConfig.unRefer();
        }
    }

    /**
     * test thread pool config
     *
     * @throws Exception Exception
     */
    @Test
    public void testThreadPoolConfig() throws Exception {

        Metric metric = fetchWithName("rpc.bolt.threadpool.config");

        Collection<Measurement> measurements = metric.measure().measurements();
        assertTrue(measurements.size() == 1);
        for (Measurement measurement : measurements) {

            // 判断ThreadPool启动配置
            Object obj = measurement.value();

            assertEquals(30, corePoolSize.get(obj));
            assertEquals(500, maxPoolSize.get(obj));
            assertEquals(600, queueSize.get(obj));
        }
    }

    /**
     * test thread pool active count
     *
     * @throws Exception Exception
     */
    @Test
    public void testThreadPoolActiveCount() throws Exception {

        Metric metric = fetchWithName("rpc.bolt.threadpool.active.count");

        Collection<Measurement> measurements = metric.measure().measurements();
        assertTrue(measurements.size() == 1);
        for (Measurement measurement : measurements) {
            assertEquals(0, ((Number) measurement.value()).intValue());
        }
    }

    /**
     * test thread pool idle count
     */
    @Test
    public void testThreadPoolIdleCount() {

        //sync invoke some time
        for (int i = 0; i < 3; i++) {
            try {
                lookoutService.saySync("lookout_sync");
            } catch (Exception e) {
                LOGGER.error("sync error", e);
            }
        }

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Metric metric = fetchWithName("rpc.bolt.threadpool.idle.count");

        Collection<Measurement> measurements = metric.measure().measurements();
        assertTrue(measurements.size() == 1);
        for (Measurement measurement : measurements) {
            assertEquals(3, ((Number) measurement.value()).intValue());
        }
    }

    /**
     * test thread pool queue size
     */
    @Test
    public void testThreadPoolQueueSize() {

        Metric metric = fetchWithName("rpc.bolt.threadpool.queue.size");

        Collection<Measurement> measurements = metric.measure().measurements();
        assertTrue(measurements.size() == 1);
        for (Measurement measurement : measurements) {
            assertEquals(0, ((Number) measurement.value()).intValue());
        }
    }

    /**
     * test provider service stats
     */
    @Test
    public void testFutureServiceStats() {

        //future
        for (int i = 0; i < 4; i++) {
            try {
                lookoutService.sayFuture("lookout_future");
                SofaResponseFuture.getResponse(3000, true);
            } catch (Exception e) {
                LOGGER.error("future error", e);
            }
        }

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Metric metric = fetchWithName("rpc.provider.service.stats");
        String methodName = "sayFuture";
        Tag testTag = findTagFromMetrics(metric, methodName);
        if (testTag == null) {
            Assert.fail("no method was found " + methodName);
        }
        assertMethod(metric, true, 4, methodName, 0, 0);

        Metric consumerMetric = fetchWithName("rpc.consumer.service.stats");

        testTag = findTagFromMetrics(consumerMetric, methodName);
        if (testTag == null) {
            Assert.fail("no method was found " + methodName);
        }
        assertMethod(consumerMetric, false, 4, methodName, 1620, 534);

    }

    /**
     * test provider service stats
     */
    @Test
    public void testCallbackServiceStats() {

        //callback
        for (int i = 0; i < 5; i++) {
            try {
                lookoutService.sayCallback("lookout_callback");
            } catch (Exception e) {
                LOGGER.error("callback error", e);
            }
        }

        for (int i = 0; i < 10; i++) {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
            }
            if (onReturn.getSize() == 5) {
                break;
            }
        }

        Metric metric = fetchWithName("rpc.provider.service.stats");

        String methodName = "sayCallback";
        Tag testTag = findTagFromMetrics(metric, methodName);
        if (testTag == null) {
            Assert.fail("no method was found " + methodName);
        }
        assertMethod(metric, true, 5, methodName, 0, 0);

        Metric consumerMetric = fetchWithName("rpc.consumer.service.stats");

        testTag = findTagFromMetrics(consumerMetric, methodName);
        if (testTag == null) {
            Assert.fail("no method was found " + methodName);
        }
        assertMethod(consumerMetric, false, 5, methodName, 2045, 720);

    }

    /**
     * test provider service stats
     */
    @Test
    public void testOnewayServiceStats() {

        //oneway
        for (int i = 0; i < 6; i++) {
            try {
                lookoutService.sayOneway("lookout_oneway");
            } catch (Exception e) {
                LOGGER.error("oneway error", e);
            }
        }

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Metric metric = fetchWithName("rpc.provider.service.stats");

        String methodName = "sayOneway";
        Tag testTag = findTagFromMetrics(metric, methodName);
        if (testTag == null) {
            Assert.fail("no method was found " + methodName);
        }
        assertMethod(metric, true, 6, methodName, 0, 0);

        Metric consumerMetric = fetchWithName("rpc.consumer.service.stats");

        testTag = findTagFromMetrics(consumerMetric, methodName);
        if (testTag == null) {
            Assert.fail("no method was found " + methodName);
        }
        assertMethod(consumerMetric, false, 6, methodName, 2430, 0);

    }

    /**
     * test provider service stats
     */
    @Test
    public void testSyncServiceStats() {
        System.out.println("start where is the log");

        //sync
        for (int i = 0; i < 3; i++) {
            try {
                lookoutService.saySync("lookout_sync");
                System.out.println("lookout_sync invoke success");
            } catch (Exception e) {
                LOGGER.error("sync error", e);
            }
        }

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            LOGGER.error("wait InterruptedException", e);
        }

        Metric metric = fetchWithName("rpc.provider.service.stats");
        System.out.println("where is the log" + metric);
        Assert.assertNotEquals("metrics is null", null, metric);
        String methodName = "saySync";
        Tag testTag = findTagFromMetrics(metric, methodName);
        if (testTag == null) {
            System.out.println("no method was found ");
            Assert.fail("no method was found " + methodName);
        }

        System.out.println("xxxxxxyyyyyy");
        assertMethod(metric, true, 3, methodName, 0, 0);

        Metric consumerMetric = fetchWithName("rpc.consumer.service.stats");

        testTag = findTagFromMetrics(consumerMetric, methodName);
        if (testTag == null) {
            Assert.fail("no method was found " + methodName);
        }
        assertMethod(consumerMetric, false, 3, methodName, 1203, 352);

    }

    /**
     * 通过methodName获取
     *
     * @param metric
     * @param methodName
     * @return
     */
    private Tag findTagFromMetrics(Metric metric, String methodName) {
        for (Tag tag : metric.id().tags()) {
            if (tag.key().equalsIgnoreCase("method")) {
                String value = tag.value();
                if (StringUtils.equals(methodName, value)) {
                    return tag;
                }
            }
        }
        return null;
    }

    /**
     * assert method
     *
     * @param metric       the metric
     * @param isProvider   is it the provider
     * @param totalCount   the total invoke count
     * @param method       the method name
     * @param requestSize  the request size
     * @param responseSize the response size
     */
    private void assertMethod(Metric metric, boolean isProvider, int totalCount, String method, int requestSize,
                              int responseSize) {
        // tag
        LOGGER.error("assertMethod");
        boolean tagAssert = false;
        for (Tag tag : metric.id().tags()) {
            LOGGER.error("rpclook" + tag.key() + tag.value());
            String key = tag.key();
            String value = tag.value();
            if (key.equals("service")) {
                assertEquals(LookoutService.class.getCanonicalName() + ":1.0", value);
                tagAssert = true;
            }
            if (key.equals("protocol")) {
                assertEquals("bolt", value);
                tagAssert = true;
            }
            if (key.equals("method")) {
                assertEquals(method, value);
                tagAssert = true;
            }
            if (isProvider) {
                if (key.equals("app")) {
                    assertEquals("TestLookOutServer", value);
                    tagAssert = true;
                }
                if (key.equals("caller_app")) {
                    assertEquals("TestLookOutClient", value);
                    tagAssert = true;
                }
            } else {
                if (key.equals("app")) {
                    assertEquals("TestLookOutClient", value);
                    tagAssert = true;
                }
                if (key.equals("target_app")) {
                    assertEquals("TestLookOutServer", value);
                    tagAssert = true;
                }
                if (key.equals("invoke_type")) {
                    assertEquals(method.substring(3).toLowerCase(), value);

                }
            }
        }
        if (!tagAssert) {
            Assert.fail("tag assert not executed");
        }

        // invoke info
        Collection<Measurement> measurements = metric.measure().measurements();
        if (isProvider) {
            assertEquals(6, measurements.size());
        } else {
            if (method.equals("sayOneway")) {
                assertEquals(5, measurements.size());
            } else {
                assertEquals(10, measurements.size());
            }
        }

        boolean invokeInfoAssert = false;
        for (Measurement measurement : measurements) {
            LOGGER.error("rpcfun" + measurement.name() + measurement.value().toString());

            String name = measurement.name();
            int value = ((Long) measurement.value()).intValue();

            if (name.equals("total_count")) {
                assertEquals(totalCount, value);
                invokeInfoAssert = true;
            }
            if (name.equals("total_time.totalTime")) {
                if (method.equals("sayOneway") && !isProvider) {
                    assertTrue(value < 3000);
                } else {
                    assertTrue(value > 3000);
                }
                invokeInfoAssert = true;
            }
            if (name.equals("total_time.count")) {
                assertEquals(totalCount, value);
                invokeInfoAssert = true;
            }
            if (name.equals("fail_count")) {
                assertEquals(1, value);
                invokeInfoAssert = true;
            }
            if (name.equals("fail_time.totalTime")) {
                assertTrue(value > 3000);
                invokeInfoAssert = true;
            }
            if (name.equals("fail_time.count")) {
                assertEquals(1, value);
                invokeInfoAssert = true;
            }
            if (!isProvider) {
                if (name.equals("request_size.count")) {
                    LOGGER.info("request_size.count,value={},requestSize={},totalCount={}", value, requestSize,
                        totalCount);
                    assertTrue(requestSize > 0);
                    invokeInfoAssert = true;
                }
                if (name.equals("response_size.count")) {
                    LOGGER.info("response_size.count,value={},responseSize={},totalCount={}", value, responseSize,
                        totalCount);
                    assertTrue(requestSize > 0);
                    invokeInfoAssert = true;
                }
            }
        }
        if (!invokeInfoAssert) {
            Assert.fail("invoke assert not executed");
        }
    }

    public static class CountSofaResponseCallback implements SofaResponseCallback {
        private int size = 0;

        @Override
        public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
            size++;
        }

        @Override
        public void onAppException(Throwable throwable, String methodName, RequestBase request) {
            size++;
        }

        @Override
        public void onSofaException(SofaRpcException sofaException, String methodName,
                                    RequestBase request) {
            size++;
        }

        public int getSize() {
            return size;
        }
    }
}