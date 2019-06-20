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
package com.alipay.sofa.rpc.registry.zk;

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import com.alipay.sofa.rpc.registry.zk.base.BaseZkTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author tian
 */
public class ZookeeperRegistryDestroyTest extends BaseZkTest {

    private static RegistryConfig    registryConfig;

    private static ZookeeperRegistry registry;

    @BeforeClass
    public static void setUp() {
        registryConfig = new RegistryConfig()
            .setProtocol(RpcConstants.REGISTRY_PROTOCOL_ZK)
            .setSubscribe(true)
            .setAddress("127.0.0.1:2181")
            .setRegister(true);

        registry = (ZookeeperRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();
        Assert.assertTrue(registry.start());
    }

    @AfterClass
    public static void tearDown() {
        registry.destroy();
        registry = null;
    }

    @Test
    public void testDestroy() {
        MockDestroyHook mockHook = new MockDestroyHook();
        registry.destroy(mockHook);

        Assert.assertTrue(mockHook.isPreDestory());
        Assert.assertTrue(mockHook.isPostDestroy());
    }

    private static class MockDestroyHook implements Destroyable.DestroyHook {
        private boolean preDestory  = false;

        private boolean postDestroy = false;

        @Override
        public void preDestroy() {
            preDestory = true;
        }

        @Override
        public void postDestroy() {
            postDestroy = true;
        }

        public boolean isPreDestory() {
            return preDestory;
        }

        public boolean isPostDestroy() {
            return postDestroy;
        }
    }

}
