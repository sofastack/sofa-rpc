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
package com.alipay.sofa.rpc.thread;

import com.alipay.sofa.common.config.ConfigKey;
import com.alipay.sofa.common.config.SofaConfigs;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.alipay.sofa.rpc.common.config.RpcConfigKeys.THREAD_POOL_TYPE;

/**
 * @author zhaowang
 * @version : ThreadPoolSupplierTest.java, v 0.1 2022年08月03日 3:15 PM zhaowang
 */
public class ThreadPoolSupplierTest {

    public static Map<String, Class> nameClassMap = new HashMap<>();

    static {
        nameClassMap.put("fixed", FixedThreadPoolSupplier.class);
        nameClassMap.put("eager", EagerThreadPoolSupplier.class);
    }

    @Test
    public void testFixedThreadPool() {
        for (Map.Entry<String, Class> stringClassEntry : nameClassMap.entrySet()) {
            ThreadPoolSupplier supplier = ExtensionLoaderFactory.getExtensionLoader(ThreadPoolSupplier.class)
                    .getExtensionClass(stringClassEntry.getKey()).getExtInstance();
            Assert.assertEquals(stringClassEntry.getValue(), supplier.getClass());
        }
    }

    @Test
    public void testConfig() {
        ConfigKey<String> type = THREAD_POOL_TYPE;
        String key = type.getKey();
        String origin = System.getProperty(key);
        try {
            System.clearProperty(key);
            Assert.assertEquals("fixed", SofaConfigs.getOrDefault(type));

            String eager = "eager";
            System.setProperty(key, eager);
            Assert.assertEquals(eager, SofaConfigs.getOrDefault(type));
        } finally {
            if (origin == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, origin);
            }
        }
    }
}