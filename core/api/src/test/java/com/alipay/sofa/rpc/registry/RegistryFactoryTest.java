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
package com.alipay.sofa.rpc.registry;

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RegistryFactoryTest {

    @Test
    public void getRegistry() {
        try {
            RegistryConfig registryConfig = new RegistryConfig().setProtocol("test111");
            Registry registry = RegistryFactory.getRegistry(registryConfig);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }
        {
            RegistryConfig registryConfig = new RegistryConfig().setProtocol("test");
            Registry registry = RegistryFactory.getRegistry(registryConfig);
            Assert.assertTrue(registry instanceof TestRegistry);

            registry.destroy(new Destroyable.DestroyHook() {
                @Override
                public void preDestroy() {

                }

                @Override
                public void postDestroy() {

                }
            });
        }

        for (int i = 0; i < 3; i++) {
            RegistryConfig registryConfig = new RegistryConfig().setProtocol("test").setTimeout(1000 + i);
            Registry registry = RegistryFactory.getRegistry(registryConfig);
            Assert.assertTrue(registry instanceof TestRegistry);
        }

        Assert.assertTrue(RegistryFactory.getRegistries().size() == 4);

        RegistryFactory.destroyAll();
    }

    @Test
    public void destroyAll() {
    }
}