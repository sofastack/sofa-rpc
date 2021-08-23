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

import com.alipay.lookout.api.Id;
import com.alipay.lookout.api.Lookout;
import com.alipay.lookout.api.Measurement;
import com.alipay.lookout.api.Metric;
import com.alipay.lookout.api.NoopRegistry;
import com.alipay.lookout.api.Registry;
import com.alipay.lookout.api.Tag;
import com.alipay.lookout.core.DefaultRegistry;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.JAXRSProviderManager;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 */
public class RestLookoutTest extends ActivelyDestroyTest {

    private ServerConfig                serverConfig;

    private ProviderConfig<RestService> providerConfig;

    private ConsumerConfig<RestService> consumerConfig;

    private RestService                 helloService;

    private Metric fetchWithNameAndMethod(String name, String methodName) {
        Registry registry = Lookout.registry();
        for (Metric metric : registry) {
            LOGGER.info("metrics name is " + metric.id() + ",name=" + name + ",methodName=" + methodName);
            if (metric.id().name().equalsIgnoreCase(name)) {

                if (StringUtils.isEmpty(methodName)) {
                    return metric;
                }
                if (matchTagFromMetrics(metric, methodName)) {
                    return metric;
                }
            }
        }
        return null;
    }

    /**
     * 通过methodName获取
     *
     * @param metric
     * @param methodName
     * @return
     */
    private boolean matchTagFromMetrics(Metric metric, String methodName) {
        for (Tag tag : metric.id().tags()) {
            if (tag.key().equalsIgnoreCase("method")) {
                String value = tag.value();
                if (StringUtils.equals(methodName, value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @BeforeClass
    public static void beforeCurrentClass() {

        RpcRunningState.setUnitTestMode(false);

        JAXRSProviderManager.registerInternalProviderClass(LookoutRequestFilter.class);

        RpcRuntimeContext.putIfAbsent(RpcRuntimeContext.KEY_APPNAME, "TestLookOutServer");

        Registry registry = new DefaultRegistry();
        final Registry currentRegistry = Lookout.registry();
        if (currentRegistry == NoopRegistry.INSTANCE) {
            Lookout.setRegistry(registry);
        }
    }

    @AfterClass
    public static void afterCurrentClass() {

        JAXRSProviderManager.removeInternalProviderClass(LookoutRequestFilter.class);

        RpcRunningState.setUnitTestMode(true);
        ActivelyDestroyTest.adAfterClass();
    }

    /**
     * invoke
     */
    @Before
    public void beforeMethod() {

        // 只有1个线程 执行
        serverConfig = new ServerConfig()
            .setStopTimeout(1000)
            .setPort(8802)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_REST)
            .setContextPath("/xyz");
        //.setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ApplicationConfig serverApplication = new ApplicationConfig();
        serverApplication.setAppName("TestLookOutServer");
        providerConfig = new ProviderConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setRef(new RestServiceImpl())
            .setServer(serverConfig)
            .setBootstrap("rest")
            //.setParameter(RpcConstants.CONFIG_HIDDEN_KEY_WARNING, "false")
            .setRegister(false)
            .setApplication(serverApplication);
        providerConfig.export();

        ApplicationConfig clientApplication = new ApplicationConfig();
        clientApplication.setAppName("TestLookOutClient");
        consumerConfig = new ConsumerConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setDirectUrl(
                "rest://127.0.0.1:8802/xyz?uniqueId=&version=1"
                    + ".0&timeout=0&delay=-1&id=rpc-cfg-0&dynamic=true&weight=100&accepts=100000"
                    + "&startTime=1523240755024&appName="
                    +
                    serverApplication.getAppName() + "&pid=22385&language=java&rpcVer=50300")
            .setProtocol("rest")
            .setBootstrap("rest")
            .setTimeout(30000)
            .setConnectionNum(5)
            .setRegister(false)
            .setApplication(clientApplication);
        helloService = consumerConfig.refer();

    }

    @After
    public void afterMethod() {

        if (providerConfig != null) {
            providerConfig.unExport();
        }
        if (consumerConfig != null) {
            consumerConfig.unRefer();
        }
        if (serverConfig != null) {
            serverConfig.destroy();
        }

    }

    /**
     * test provider service stats
     */
    @Test
    public void testServiceStats() {

        Assert.assertEquals(helloService.query(11), "hello world !null");

        //wait metrics info
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String methodName = "query";

        Metric metric = fetchWithNameAndMethod("rpc.provider.service.stats", methodName);
        if (metric == null) {
            Assert.fail("no provider metric was found null");
        }

        assertMethod(metric, true, 1, methodName, 0, 0);

        //metrics for consumer
        Metric consumerMetric = fetchWithNameAndMethod("rpc.consumer.service.stats", methodName);
        if (consumerMetric == null) {
            Assert.fail("no consumer metric was found null");
        }

        assertMethod(consumerMetric, false, 1, methodName, 1203, 352);

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
        boolean tagAssert = false;
        for (Tag tag : metric.id().tags()) {

            String key = tag.key();
            String value = tag.value();
            LOGGER.info(this.getClass().getName() + ",key=" + key + ",value=" + value);
            if (key.equals("service")) {
                assertEquals("service not equal", RestService.class.getCanonicalName() + ":1.0", value);
                tagAssert = true;
            }
            if (key.equals("protocol")) {
                assertEquals("protocol not equal", "rest", value);
                tagAssert = true;
            }
            if (key.equals("method")) {
                assertEquals("method not equal", method, value);
                tagAssert = true;
            }
            if (isProvider) {
                if (key.equals("app")) {
                    assertEquals("app not equal in provider", "TestLookOutServer", value);
                    tagAssert = true;
                }
                if (key.equals("caller_app")) {
                    assertEquals("caller_app not equal in provider", "TestLookOutClient", value);
                    tagAssert = true;
                }
            } else {
                if (key.equals("app")) {
                    assertEquals("app not equal in consumer", "TestLookOutClient", value);
                    tagAssert = true;
                }
                if (key.equals("target_app")) {
                    assertEquals("target_app not equal in consumer", "TestLookOutServer", value);
                    tagAssert = true;
                }
                if (key.equals("invoke_type")) {
                    assertEquals("invoke_type not equal in consumer", "sync", value);

                }
            }
        }
        if (!tagAssert) {
            Assert.fail("no tag assert");
        }

        // invoke info
        Collection<Measurement> measurements = metric.measure().measurements();
        if (isProvider) {
            assertEquals("measurements is not equals in provider", 3, measurements.size());
        } else {
            assertEquals("measurements is not equals in consumer", 4, measurements.size());
        }

        boolean invokeInfoAssert = false;
        for (Measurement measurement : measurements) {
            String name = measurement.name();
            int value = ((Long) measurement.value()).intValue();

            LOGGER.info(this.getClass().getName() + ",name=" + name + ",value=" + value);

            if (name.equals("total_count")) {
                assertEquals("total_count is not equal", totalCount, value);
                invokeInfoAssert = true;
            }
            if (name.equals("total_time.totalTime")) {
                assertTrue("totalTime is not equal", value < 3000);
                invokeInfoAssert = true;
            }
            if (name.equals("total_time.count")) {
                assertEquals("count is not equal", totalCount, value);
                invokeInfoAssert = true;
            }
            if (name.equals("fail_count")) {
                assertEquals("fail_count is not equal", 1, value);
                invokeInfoAssert = true;
            }
            if (name.equals("fail_time.totalTime")) {
                assertTrue("fail_time.totalTime is not equal", value > 3000);
                invokeInfoAssert = true;
            }
            if (name.equals("fail_time.count")) {
                assertEquals("fail_time.count is not equal", 1, value);
                invokeInfoAssert = true;
            }
            if (!isProvider) {
                if (name.equals("request_size.count")) {
                    LOGGER.info("request_size.count,value={},requestSize={},totalCount={}", value, requestSize,
                        totalCount);
                    assertTrue("request_size.count is smaller than 0", requestSize > 0);
                    invokeInfoAssert = true;
                }
                if (name.equals("response_size.count")) {
                    LOGGER.info("response_size.count,value={},responseSize={},totalCount={}", value, responseSize,
                        totalCount);
                    assertTrue("response_size.count is smaller than 0", requestSize > 0);
                    invokeInfoAssert = true;
                }
            }
        }
        if (!invokeInfoAssert) {
            Assert.fail("no invoke info assert");
        }
    }
}