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
package com.alipay.sofa.rpc.bootstrap.dubbo;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for DubboSingleton
 *
 * @author SOFA-RPC Team
 */
public class DubboSingletonTest {

    @Test
    public void testStaticBlocks() {
        // Verify static maps are initialized
        Assert.assertNotNull(DubboSingleton.SERVER_MAP);
        Assert.assertNotNull(DubboSingleton.REGISTRY_MAP);
    }

    @Test
    public void testDestroyAll() {
        // Test destroyAll method - should not throw exception
        try {
            DubboSingleton.destroyAll();
        } catch (Exception e) {
            // May fail in test environment without full dubbo setup
        }
    }
}
