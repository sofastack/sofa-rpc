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
package com.alipay.sofa.rpc.config;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ConfigUniqueNameGeneratorTest {

    @Test
    public void getServiceName() {
        ProviderConfig providerConfig = (ProviderConfig) new ProviderConfig().setInterfaceId("com.xx");
        Assert.assertEquals(ConfigUniqueNameGenerator.getServiceName(providerConfig), "com.xx");

        providerConfig = (ProviderConfig) new ProviderConfig().setInterfaceId("com.xx").setVersion("2.0");
        Assert.assertEquals(ConfigUniqueNameGenerator.getServiceName(providerConfig), "com.xx");

        providerConfig = (ProviderConfig) new ProviderConfig().setInterfaceId("com.xx").setVersion("2.0")
            .setUniqueId("ud");
        Assert.assertEquals(ConfigUniqueNameGenerator.getServiceName(providerConfig), "com.xx:ud");

        ConsumerConfig consumerConfig = (ConsumerConfig) new ConsumerConfig().setInterfaceId("com.xx");
        Assert.assertEquals(ConfigUniqueNameGenerator.getServiceName(consumerConfig), "com.xx");

        consumerConfig = (ConsumerConfig) new ConsumerConfig().setInterfaceId("com.xx").setVersion("2.0");
        Assert.assertEquals(ConfigUniqueNameGenerator.getServiceName(consumerConfig), "com.xx");

        consumerConfig = (ConsumerConfig) new ConsumerConfig().setInterfaceId("com.xx").setVersion("2.0")
            .setUniqueId("ud");
        Assert.assertEquals(ConfigUniqueNameGenerator.getServiceName(consumerConfig), "com.xx:ud");
    }

    @Test
    public void getUniqueName() throws Exception {

        ProviderConfig providerConfig = (ProviderConfig) new ProviderConfig().setInterfaceId("com.xx");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueName(providerConfig), "com.xx:1.0");

        providerConfig = (ProviderConfig) new ProviderConfig().setInterfaceId("com.xx").setVersion("2.0");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueName(providerConfig), "com.xx:2.0");

        providerConfig = (ProviderConfig) new ProviderConfig().setInterfaceId("com.xx").setVersion("2.0")
            .setUniqueId("ud");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueName(providerConfig), "com.xx:2.0:ud");

        ConsumerConfig consumerConfig = (ConsumerConfig) new ConsumerConfig().setInterfaceId("com.xx");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueName(consumerConfig), "com.xx:1.0");

        consumerConfig = (ConsumerConfig) new ConsumerConfig().setInterfaceId("com.xx").setVersion("2.0");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueName(consumerConfig), "com.xx:2.0");

        consumerConfig = (ConsumerConfig) new ConsumerConfig().setInterfaceId("com.xx").setVersion("2.0")
            .setUniqueId("ud");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueName(consumerConfig), "com.xx:2.0:ud");
    }

    @Test
    public void getUniqueNameProtocol() throws Exception {
        ProviderConfig providerConfig = (ProviderConfig) new ProviderConfig().setInterfaceId("com.xx");
        Assert.assertEquals("com.xx:1.0", ConfigUniqueNameGenerator.getUniqueNameProtocol(providerConfig, null));
        Assert.assertEquals("com.xx:1.0", ConfigUniqueNameGenerator.getUniqueNameProtocol(providerConfig, ""));
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueNameProtocol(providerConfig, "bolt"),
            "com.xx:1.0@bolt");

        providerConfig = (ProviderConfig) new ProviderConfig().setInterfaceId("com.xx").setVersion("2.0");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueNameProtocol(providerConfig, "bolt"),
            "com.xx:2.0@bolt");

        providerConfig = (ProviderConfig) new ProviderConfig().setInterfaceId("com.xx").setVersion("2.0")
            .setUniqueId("ud");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueNameProtocol(providerConfig, "bolt"),
            "com.xx:2.0:ud@bolt");
    }

    @Test
    public void getUniqueNameProtocol1() throws Exception {
        ConsumerConfig consumerConfig = (ConsumerConfig) new ConsumerConfig().setProtocol("bolt")
            .setInterfaceId("com.xx");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueNameProtocol(consumerConfig),
            "com.xx:1.0@bolt");

        consumerConfig = (ConsumerConfig) new ConsumerConfig().setProtocol("bolt")
            .setInterfaceId("com.xx").setVersion("2.0");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueNameProtocol(consumerConfig),
            "com.xx:2.0@bolt");

        consumerConfig = (ConsumerConfig) new ConsumerConfig().setProtocol("bolt")
            .setInterfaceId("com.xx").setVersion("2.0")
            .setUniqueId("ud");
        Assert.assertEquals(ConfigUniqueNameGenerator.getUniqueNameProtocol(consumerConfig),
            "com.xx:2.0:ud@bolt");
    }

    @Test
    public void getInterfaceName() {
        Assert.assertNull(ConfigUniqueNameGenerator.getInterfaceName(null));
        Assert.assertEquals("", ConfigUniqueNameGenerator.getInterfaceName(""));
        Assert.assertEquals("aaa", ConfigUniqueNameGenerator.getInterfaceName("aaa"));
        Assert.assertEquals("bbb", ConfigUniqueNameGenerator.getInterfaceName("bbb:"));
        Assert.assertEquals("ccc", ConfigUniqueNameGenerator.getInterfaceName("ccc:111"));
        Assert.assertEquals("ddd", ConfigUniqueNameGenerator.getInterfaceName("ddd:111:222"));
        Assert.assertEquals("", ConfigUniqueNameGenerator.getInterfaceName(":eee:111"));
    }
}