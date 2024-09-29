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
import com.alipay.sofa.rpc.dynamic.apollo.ApolloDynamicConfigManager;
import com.alipay.sofa.rpc.test.HelloService;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Narziss
 * @version ApolloDynamicConfigTest.java, v 0.1 2024年09月28日 10:46 Narziss
 */
public class ApolloDynamicConfigTest {

    @Test
    public void testApolloDynamicConfig() throws Exception {
        System.setProperty(DynamicConfigKeys.DYNAMIC_URL.getKey(), "apollo://127.0.0.1:8080");
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
        DynamicConfigManager dynamicConfigManager = DynamicConfigManagerFactory.getDynamicManagerWithUrl
            (clientApplication.getAppName(), System.getProperty(DynamicConfigKeys.DYNAMIC_URL.getKey()));
        Field field = ApolloDynamicConfigManager.class.getDeclaredField("watchListenerMap");
        field.setAccessible(true);
        Map<String, ApolloDynamicConfigManager.ApolloListener> watchListenerMap = (Map<String, ApolloDynamicConfigManager.ApolloListener>) field
            .get(dynamicConfigManager);
        ApolloDynamicConfigManager.ApolloListener apolloConfigListener = watchListenerMap.get(consumerConfig
            .getInterfaceId());

        // 测试配置更新
        String configValue = "timeout=5000\n.sayHello.timeout=6000";
        ConfigChange configChange = new ConfigChange("application", consumerConfig.getInterfaceId(), null, configValue, PropertyChangeType.ADDED);
        Map<String, ConfigChange> changes= new HashMap<>();
        changes.put(configChange.getPropertyName(), configChange);
        ConfigChangeEvent event = new ConfigChangeEvent("application",changes);
        apolloConfigListener.onChange(event);
        Assert.assertEquals(6000, consumerConfig.getMethodTimeout("sayHello"));
    }
}
