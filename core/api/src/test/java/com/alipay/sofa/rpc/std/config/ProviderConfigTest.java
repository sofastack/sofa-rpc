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

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.std.sample.SampleService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author zhaowang
 * @version : ProviderConfigTest.java, v 0.1 2022年01月25日 4:02 下午 zhaowang
 */
public class ProviderConfigTest {
    @Test
    public void testDefaultFieldValue() {
        ProviderConfig<SampleService> defaultConfig = new ProviderConfig<>();
        assertEquals(null, defaultConfig.getRef());
        assertEquals(null, defaultConfig.getServer());
        assertEquals(-1, defaultConfig.getDelay());
        assertEquals(100, defaultConfig.getWeight());
        assertEquals("*", defaultConfig.getInclude());
        assertEquals("", defaultConfig.getExclude());
        assertEquals(true, defaultConfig.isDynamic());
        assertEquals(0, defaultConfig.getPriority());
        assertEquals(null, defaultConfig.getBootstrap());
        assertEquals(null, defaultConfig.getExecutor());
        assertEquals(0, defaultConfig.getTimeout());
        assertEquals(0, defaultConfig.getConcurrents());
        assertEquals(1, defaultConfig.getRepeatedExportLimit());
        assertEquals(null, defaultConfig.getMethodsLimit());
        assertEquals(null, defaultConfig.getProviderBootstrap());
    }
}