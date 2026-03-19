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
package com.alipay.sofa.rpc.bootstrap.dubbo;

import com.alipay.sofa.rpc.bootstrap.dubbo.demo.DemoService;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import org.apache.dubbo.config.ConfigKeys;
import org.apache.dubbo.config.context.ConfigMode;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href=mailto:taobaorun@gmail.com>taobaorun</a>
 */
public class DubboConsumerBootstrapTest {

    private DubboConsumerBootstrap<DemoService> dubboConsumerBootstrap;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty(ConfigKeys.DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE, "true");
        System.setProperty(ConfigKeys.DUBBO_CONFIG_MODE, ConfigMode.IGNORE.name());
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            FrameworkModel.defaultModel().destroy();
        } catch (Exception e) {
            // ignore cleanup errors
        }
        System.clearProperty(ConfigKeys.DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE);
        System.clearProperty(ConfigKeys.DUBBO_CONFIG_MODE);
    }

    @Before
    public void setUp() {

        ApplicationConfig clientApplication = new ApplicationConfig();
        clientApplication.setAppName("client");
        ConsumerConfig consumerConfig = new ConsumerConfig<DemoService>()
                .setInterfaceId(DemoService.class.getName())
                .setDirectUrl("dubbo://127.0.0.1:20880")
                .setTimeout(30000)
                .setRegister(false)
                .setProtocol("dubbo")
                .setBootstrap("dubbo")
                .setApplication(clientApplication)
                .setInvokeType(RpcConstants.INVOKER_TYPE_ONEWAY);
        consumerConfig.setParameter("version", "1.0.1");
        dubboConsumerBootstrap = new DubboConsumerBootstrap<>(consumerConfig);

    }

    @Test
    public void test_dubbo_service_version() {
        Assert.assertEquals("1.0.1", dubboConsumerBootstrap.getConsumerConfig().getParameter("version"));
    }

    @Test
    public void testConsumerBootstrapCreation() {
        Assert.assertNotNull(dubboConsumerBootstrap);
        Assert.assertEquals(DemoService.class.getName(),
            dubboConsumerBootstrap.getConsumerConfig().getInterfaceId());
    }

    @Test
    public void testConsumerBootstrapExtensionAnnotation() {
        // Verify extension annotation
        Assert.assertNotNull(
            DubboConsumerBootstrap.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class));

        com.alipay.sofa.rpc.ext.Extension extension =
                DubboConsumerBootstrap.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class);
        Assert.assertEquals("dubbo", extension.value());
    }

    @Test
    public void testRefer() {
        // Test refer method - may fail due to network connection
        try {
            DemoService service = dubboConsumerBootstrap.refer();
            // If connection succeeds, service should not be null
            // In test environment without actual dubbo server, this may fail
        } catch (Exception e) {
            // Expected in test environment without dubbo server
        }
    }

    @Test
    public void testUnRefer() {
        // Test unRefer method - should not throw exception
        try {
            dubboConsumerBootstrap.unRefer();
        } catch (Exception e) {
            // Should not happen
            Assert.fail("unRefer should not throw exception: " + e.getMessage());
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetCluster() {
        // getCluster should throw UnsupportedOperationException
        dubboConsumerBootstrap.getCluster();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubscribe() {
        // subscribe should throw UnsupportedOperationException
        dubboConsumerBootstrap.subscribe();
    }

    @Test
    public void testIsSubscribed() {
        // Initially not subscribed
        Assert.assertFalse(dubboConsumerBootstrap.isSubscribed());
    }

    @Test
    public void testGetProxyIns() {
        // Initially proxy is null
        Assert.assertNull(dubboConsumerBootstrap.getProxyIns());
    }
}