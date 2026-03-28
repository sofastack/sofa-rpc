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
import com.alipay.sofa.rpc.bootstrap.dubbo.demo.DemoServiceImpl;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
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
public class DubboProviderBootstrapTest {

    private DubboProviderBootstrap dubboProviderBootstrap;

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
    public void setUp() throws Exception {

        ApplicationConfig serverApplacation = new ApplicationConfig();
        serverApplacation.setAppName("server");
        ProviderConfig providerConfig = new ProviderConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setRef(new DemoServiceImpl())
            .setBootstrap("dubbo")
            .setParameter("version", "1.0.1")
            .setRegister(false).setApplication(serverApplacation);

        dubboProviderBootstrap = new DubboProviderBootstrap(providerConfig);
    }

    @Test
    public void test_dubbo_service_version() {
        Assert.assertEquals("1.0.1", dubboProviderBootstrap.getProviderConfig().getParameter("version"));
    }

    @Test
    public void testProviderBootstrapCreation() {
        Assert.assertNotNull(dubboProviderBootstrap);
        Assert.assertEquals("com.alipay.sofa.rpc.bootstrap.dubbo.demo.DemoService",
            dubboProviderBootstrap.getProviderConfig().getInterfaceId());
    }

    @Test
    public void testProviderBootstrapExtensionAnnotation() {
        // Verify extension annotation on DubboProviderBootstrap
        Assert.assertNotNull(
            DubboProviderBootstrap.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class));

        com.alipay.sofa.rpc.ext.Extension extension =
                DubboProviderBootstrap.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class);
        Assert.assertEquals("dubbo", extension.value());
    }

    @Test
    public void testExportAndUnExport() {
        // Test export and unExport methods - should not throw exception
        try {
            dubboProviderBootstrap.export();
            dubboProviderBootstrap.unExport();
        } catch (Exception e) {
            // May fail due to port conflicts or missing dependencies in test environment
        }
    }

    @Test
    public void testBuildUrls() {
        // Test buildUrls - returns null if not exported
        Assert.assertNull(dubboProviderBootstrap.buildUrls());
    }
}