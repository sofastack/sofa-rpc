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
package com.alipay.sofa.rpc.test.config;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.dynamic.DynamicConfigKeys;
import com.alipay.sofa.rpc.dynamic.DynamicConfigManager;

import com.alipay.sofa.rpc.dynamic.DynamicConfigManagerFactory;
import com.alipay.sofa.rpc.dynamic.nacos.NacosDynamicConfigManager;
import com.alipay.sofa.rpc.test.HelloService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author Narziss
 * @version NacosDynamicConfigTest.java, v 0.1 2024年09月28日 12:11 Narziss
 */
public class NacosDynamicConfigTest {

    @Test
    public void testNacosDynamicConfig() throws Exception {
        System.setProperty(DynamicConfigKeys.DYNAMIC_REFRESH_ENABLE.getKey(), "true");
        System.setProperty(DynamicConfigKeys.CONFIG_CENTER_ADDRESS.getKey(),
            "nacos://127.0.0.1:8848/sofa-rpc-config?username=nacos&password=nacos");
        ApplicationConfig clientApplication = new ApplicationConfig();
        clientApplication.setAppName("demo");

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setProtocol("bolt")
            .setDirectUrl("bolt://127.0.0.1:12200")
            .setConnectTimeout(10 * 1000)
            .setApplication(clientApplication);

        consumerConfig.refer();

        // 获取接口对应的动态配置监听器
        DynamicConfigManager dynamicConfigManager = DynamicConfigManagerFactory.getDynamicManager
            (clientApplication.getAppName(), "nacos");
        Field field = NacosDynamicConfigManager.class.getDeclaredField("watchListenerMap");
        field.setAccessible(true);
        Map<String, NacosDynamicConfigManager.NacosConfigListener> watchListenerMap = (Map<String, NacosDynamicConfigManager.NacosConfigListener>) field
            .get(dynamicConfigManager);
        NacosDynamicConfigManager.NacosConfigListener nacosConfigListener = watchListenerMap.get(consumerConfig
            .getInterfaceId());

        // 测试配置新增
        String configValue = "timeout=5000";
        nacosConfigListener.innerReceive(consumerConfig.getInterfaceId(), consumerConfig.getAppName(), configValue);
        Assert.assertEquals(5000, consumerConfig.getMethodTimeout("sayHello"));
        // 测试配置修改
        configValue = "timeout=5000" + System.lineSeparator() + ".sayHello.timeout=6000";
        nacosConfigListener.innerReceive(consumerConfig.getInterfaceId(), consumerConfig.getAppName(), configValue);
        Assert.assertEquals(6000, consumerConfig.getMethodTimeout("sayHello"));
        // 测试配置删除
        configValue = "";
        nacosConfigListener.innerReceive(consumerConfig.getInterfaceId(), consumerConfig.getAppName(), configValue);
        Assert.assertEquals(-1, consumerConfig.getMethodTimeout("sayHello"));

        System.clearProperty(DynamicConfigKeys.CONFIG_CENTER_ADDRESS.getKey());
    }
}
