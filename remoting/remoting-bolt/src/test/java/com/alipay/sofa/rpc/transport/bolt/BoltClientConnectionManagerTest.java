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
package com.alipay.sofa.rpc.transport.bolt;

import com.alipay.remoting.Connection;
import com.alipay.remoting.Url;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for BoltClientConnectionManager (abstract class)
 * Tests the constructor and hook functionality
 *
 * @author <a href="mailto:xxx@antfin.com">XXX</a>
 */
public class BoltClientConnectionManagerTest {

    @Test
    public void testConstructorWithHook() {
        // Test constructor with hook enabled
        TestBoltClientConnectionManager manager = new TestBoltClientConnectionManager(true);
        Assert.assertNotNull(manager);
    }

    @Test
    public void testConstructorWithoutHook() {
        // Test constructor with hook disabled
        TestBoltClientConnectionManager manager = new TestBoltClientConnectionManager(false);
        Assert.assertNotNull(manager);
    }

    @Test
    public void testAbstractMethods() {
        TestBoltClientConnectionManager manager = new TestBoltClientConnectionManager(false);

        // Test that abstract methods can be called
        manager.checkLeak();

        Connection connection = manager.getConnection(null, null, null);
        Assert.assertNull(connection);

        manager.closeConnection(null, null, null);

        boolean result = manager.isConnectionFine(null, null, null);
        Assert.assertFalse(result);
    }

    /**
     * Test implementation of abstract BoltClientConnectionManager
     */
    static class TestBoltClientConnectionManager extends BoltClientConnectionManager {

        public TestBoltClientConnectionManager(boolean addHook) {
            super(addHook);
        }

        @Override
        protected void checkLeak() {
            // No-op implementation for testing
        }

        @Override
        public Connection getConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url) {
            return null;
        }

        @Override
        public void closeConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url) {
            // No-op
        }

        @Override
        public boolean isConnectionFine(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url) {
            return false;
        }
    }

    @Test
    public void testMultipleConstructors() {
        // Create multiple managers to ensure no static state issues
        TestBoltClientConnectionManager manager1 = new TestBoltClientConnectionManager(true);
        TestBoltClientConnectionManager manager2 = new TestBoltClientConnectionManager(false);
        TestBoltClientConnectionManager manager3 = new TestBoltClientConnectionManager(true);

        Assert.assertNotNull(manager1);
        Assert.assertNotNull(manager2);
        Assert.assertNotNull(manager3);
    }
}
