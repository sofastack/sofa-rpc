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
import com.alipay.sofa.rpc.dynamic.zk.ZookeeperDynamicConfigManager;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.config.base.BaseZkTest;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * @author Narziss
 * @version ZookeeperDynamicConfigTest.java, v 0.1 2024年09月28日 14:33 Narziss
 */
public class ZookeeperDynamicConfigTest extends BaseZkTest {

    Logger logger = LoggerFactory.getLogger(ZookeeperDynamicConfigTest.class);

    @Test
    public void testZookeeperDynamicConfig() throws Exception {
        System.setProperty(DynamicConfigKeys.DYNAMIC_REFRESH_ENABLE.getKey(), "true");
        System.setProperty(DynamicConfigKeys.CONFIG_CENTER_ADDRESS.getKey(), "zookeeper://127.0.0.1:2181");
        ApplicationConfig clientApplication = new ApplicationConfig();
        clientApplication.setAppName("demo");

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setProtocol("bolt")
            .setDirectUrl("bolt://127.0.0.1:12200")
            .setConnectTimeout(10 * 1000)
            .setApplication(clientApplication);

        consumerConfig.refer();

        DynamicConfigManager dynamicConfigManager = DynamicConfigManagerFactory.getDynamicManager
            (clientApplication.getAppName(), "zookeeper");
        Field field = ZookeeperDynamicConfigManager.class.getDeclaredField("zkClient");
        field.setAccessible(true);
        CuratorFramework zkClient = (CuratorFramework) field.get(dynamicConfigManager);

        // 新增或修改配置节点
        if (zkClient.checkExists().forPath("/config/demo/com.alipay.sofa.rpc.test.HelloService") == null) {
            zkClient.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath("/config/demo/com.alipay.sofa.rpc.test.HelloService", "timeout=5000".getBytes());
        } else {
            zkClient.setData().forPath("/config/demo/com.alipay.sofa.rpc.test.HelloService",
                "timeout=5000".getBytes());
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        // 验证配置是否更新
        Assert.assertEquals(5000, consumerConfig.getMethodTimeout("sayHello"));

        //删除配置节点
        zkClient.delete().forPath("/config/demo/com.alipay.sofa.rpc.test.HelloService");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        // 验证配置是否删除
        Assert.assertEquals(-1, consumerConfig.getMethodTimeout("sayHello"));

        System.clearProperty(DynamicConfigKeys.CONFIG_CENTER_ADDRESS.getKey());

    }
}
