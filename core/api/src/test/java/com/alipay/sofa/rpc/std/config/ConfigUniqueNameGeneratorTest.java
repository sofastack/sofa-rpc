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
package com.alipay.sofa.rpc.std.config;

import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.std.AbstractMockitoTest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * @author zhaowang
 * @version : ConfigUniqueNameGeneratorTest.java, v 0.1 2022年01月28日 11:08 上午 zhaowang
 */
public class ConfigUniqueNameGeneratorTest extends AbstractMockitoTest {

    @Mock
    private AbstractInterfaceConfig config;
    @Mock
    private ProviderConfig          providerConfig;
    @Mock
    private ConsumerConfig          consumerConfig;

    @Test
    public void testGetServiceName() {
        String interfaceId = "interfaceId";
        String uniqueId = "uniqueId";
        when(config.getInterfaceId()).thenReturn(interfaceId);
        when(config.getUniqueId()).thenReturn(null);
        Assert.assertEquals(interfaceId, ConfigUniqueNameGenerator.getServiceName(config));
        when(config.getUniqueId()).thenReturn(uniqueId);
        Assert.assertEquals("interfaceId:uniqueId", ConfigUniqueNameGenerator.getServiceName(config));
    }

    @Test
    public void testGetUniqueName() {
        String interfaceId = "interfaceId";
        String uniqueId = "uniqueId";
        String version = "version";

        when(config.getInterfaceId()).thenReturn(interfaceId);
        when(config.getUniqueId()).thenReturn(null);
        when(config.getVersion()).thenReturn(null);
        Assert.assertEquals("interfaceId:1.0", ConfigUniqueNameGenerator.getUniqueName(config));

        when(config.getInterfaceId()).thenReturn(interfaceId);
        when(config.getUniqueId()).thenReturn(uniqueId);
        when(config.getVersion()).thenReturn(null);
        Assert.assertEquals("interfaceId:1.0:uniqueId", ConfigUniqueNameGenerator.getUniqueName(config));

        when(config.getInterfaceId()).thenReturn(interfaceId);
        when(config.getUniqueId()).thenReturn(null);
        when(config.getVersion()).thenReturn(version);
        Assert.assertEquals("interfaceId:version", ConfigUniqueNameGenerator.getUniqueName(config));

        when(config.getInterfaceId()).thenReturn(interfaceId);
        when(config.getUniqueId()).thenReturn(uniqueId);
        when(config.getVersion()).thenReturn(version);
        Assert.assertEquals("interfaceId:version:uniqueId", ConfigUniqueNameGenerator.getUniqueName(config));
    }

    @Test
    public void testGetInterfaceName() {
        String uniqueName = "interfaceId:1.0";
        Assert.assertEquals("interfaceId", ConfigUniqueNameGenerator.getInterfaceName(uniqueName));
        uniqueName = "interfaceId:1.0:uniqueId";
        Assert.assertEquals("interfaceId", ConfigUniqueNameGenerator.getInterfaceName(uniqueName));
        uniqueName = "interfaceId:version";
        Assert.assertEquals("interfaceId", ConfigUniqueNameGenerator.getInterfaceName(uniqueName));
        uniqueName = "interfaceId:version:uniqueId";
        Assert.assertEquals("interfaceId", ConfigUniqueNameGenerator.getInterfaceName(uniqueName));
        Assert.assertNull(ConfigUniqueNameGenerator.getInterfaceName(null));
    }

    @Test
    public void testGetUniqueNameProtocol() {
        String interfaceId = "interfaceId";

        when(providerConfig.getInterfaceId()).thenReturn(interfaceId);
        when(providerConfig.getUniqueId()).thenReturn(null);
        when(providerConfig.getVersion()).thenReturn(null);
        Assert.assertEquals("interfaceId:1.0", ConfigUniqueNameGenerator.getUniqueName(providerConfig));

        String uniqueNameProtocol = ConfigUniqueNameGenerator.getUniqueNameProtocol(providerConfig, null);
        Assert.assertEquals("interfaceId:1.0", uniqueNameProtocol);

        uniqueNameProtocol = ConfigUniqueNameGenerator.getUniqueNameProtocol(providerConfig, "protocol");
        Assert.assertEquals("interfaceId:1.0@protocol", uniqueNameProtocol);
    }

    @Test
    public void testGetUniqueNameProtocol2() {
        String interfaceId = "interfaceId";

        when(consumerConfig.getInterfaceId()).thenReturn(interfaceId);
        when(consumerConfig.getUniqueId()).thenReturn(null);
        when(consumerConfig.getVersion()).thenReturn(null);
        Assert.assertEquals("interfaceId:1.0", ConfigUniqueNameGenerator.getUniqueName(consumerConfig));
        when(consumerConfig.getProtocol()).thenReturn(null);
        // discuss if protocol is null
        Assert.assertEquals("interfaceId:1.0@null", ConfigUniqueNameGenerator.getUniqueNameProtocol(consumerConfig));
        when(consumerConfig.getProtocol()).thenReturn("protocol");
        Assert
            .assertEquals("interfaceId:1.0@protocol", ConfigUniqueNameGenerator.getUniqueNameProtocol(consumerConfig));
    }
}