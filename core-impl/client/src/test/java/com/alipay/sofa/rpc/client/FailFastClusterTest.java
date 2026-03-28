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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for FailFastCluster
 *
 * @author SOFA-RPC Team
 */
public class FailFastClusterTest {

    @Test
    public void testClassInitialization() {
        Assert.assertNotNull(FailFastCluster.class);
    }

    @Test
    public void testExtensionAnnotation() {
        com.alipay.sofa.rpc.ext.Extension extension =
                FailFastCluster.class.getAnnotation(com.alipay.sofa.rpc.ext.Extension.class);

        Assert.assertNotNull(extension);
        Assert.assertEquals("failfast", extension.value());
    }

    @Test
    public void testConstructor() {
        ConsumerBootstrap mockBootstrap = new MockConsumerBootstrap();
        FailFastCluster cluster = new FailFastCluster(mockBootstrap);

        Assert.assertNotNull(cluster);
    }

    private static class MockConsumerBootstrap extends ConsumerBootstrap {
        public MockConsumerBootstrap() {
            super(null);
        }

        @Override
        public Object refer() {
            return null;
        }

        @Override
        public void unRefer() {
        }

        @Override
        public Object getProxyIns() {
            return null;
        }

        @Override
        public com.alipay.sofa.rpc.client.Cluster getCluster() {
            return null;
        }

        @Override
        public java.util.List<com.alipay.sofa.rpc.client.ProviderGroup> subscribe() {
            return null;
        }

        @Override
        public boolean isSubscribed() {
            return false;
        }
    }
}
