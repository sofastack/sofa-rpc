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
package com.alipay.sofa.rpc.bootstrap.triple;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for Triple bootstrap classes
 *
 * @author SOFA-RPC Team
 */
public class TripleBootstrapTest {

    @Test
    public void testTripleConsumerBootstrapCreation() {
        // Test consumer bootstrap creation
        ConsumerConfig<Object> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setInterfaceId("com.alipay.sofa.rpc.test.TestService");

        TripleConsumerBootstrap<Object> bootstrap = new TripleConsumerBootstrap<>(consumerConfig);

        Assert.assertNotNull(bootstrap);
        Assert.assertEquals("com.alipay.sofa.rpc.test.TestService",
            bootstrap.getConsumerConfig().getInterfaceId());
    }

    @Test
    public void testTripleProviderBootstrapCreation() {
        // Test provider bootstrap creation
        ProviderConfig<Object> providerConfig = new ProviderConfig<>();
        providerConfig.setInterfaceId("com.alipay.sofa.rpc.test.TestService");

        TripleProviderBootstrap<Object> bootstrap = new TripleProviderBootstrap<>(providerConfig);

        Assert.assertNotNull(bootstrap);
        Assert.assertEquals("com.alipay.sofa.rpc.test.TestService",
            bootstrap.getProviderConfig().getInterfaceId());
    }

    @Test
    public void testTripleConsumerBootstrapExtensionAnnotation() {
        // Verify extension annotation
        Assert.assertNotNull(
            TripleConsumerBootstrap.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class));
    }

    @Test
    public void testTripleProviderBootstrapExtensionAnnotation() {
        // Verify extension annotation
        Assert.assertNotNull(
            TripleProviderBootstrap.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class));

        com.alipay.sofa.rpc.ext.Extension extension =
                TripleProviderBootstrap.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class);
        Assert.assertEquals("tri", extension.value());
    }
}
