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
package com.alipay.sofa.rpc.registry.nacos;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * @author bystander
 * @version $Id: NacosRegistryNamespaceTest.java, v 0.1 2019年02月20日 18:30 bystander Exp $
 */
public class NacosRegistryNamespaceTest {

    @Test
    public void testWithNamespace() {
        RegistryConfig registryConfig = new RegistryConfig()
            .setProtocol("nacos")
            .setSubscribe(true)
            .setAddress("127.0.0.1:8848/namespace")
            .setRegister(true);

        NacosRegistry registry = (NacosRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();

        Properties properties = registry.getNacosConfig();

        String address = properties.getProperty(PropertyKeyConst.SERVER_ADDR);

        Assert.assertEquals("127.0.0.1:8848", address);
        String namespace = properties.getProperty(PropertyKeyConst.NAMESPACE);
        Assert.assertEquals("namespace", namespace);

    }

    @Test
    public void testWithoutNamespace() {
        RegistryConfig registryConfig = new RegistryConfig()
            .setProtocol("nacos")
            .setSubscribe(true)
            .setAddress("127.0.0.1:8848/")
            .setRegister(true);

        NacosRegistry registry = (NacosRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();

        Properties properties = registry.getNacosConfig();

        String address = properties.getProperty(PropertyKeyConst.SERVER_ADDR);

        Assert.assertEquals("127.0.0.1:8848", address);
        String namespace = properties.getProperty(PropertyKeyConst.NAMESPACE);
        Assert.assertEquals("sofa-rpc", namespace);

    }

    @Test
    public void testWithoutSlashNamespace() {
        RegistryConfig registryConfig = new RegistryConfig()
            .setProtocol("nacos")
            .setSubscribe(true)
            .setAddress("127.0.0.1:8848")
            .setRegister(true);

        NacosRegistry registry = (NacosRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();

        Properties properties = registry.getNacosConfig();

        String address = properties.getProperty(PropertyKeyConst.SERVER_ADDR);

        Assert.assertEquals("127.0.0.1:8848", address);
        String namespace = properties.getProperty(PropertyKeyConst.NAMESPACE);
        //default namespace
        Assert.assertEquals("sofa-rpc", namespace);

    }
}