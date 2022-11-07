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

import com.alipay.sofa.rpc.common.MockMode;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.TestUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.MethodConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.listener.ConfigListener;
import com.alipay.sofa.rpc.std.sample.SampleService;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alipay.sofa.rpc.common.utils.TestUtils.randomString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author zhaowang
 * @version : AbstractInterfaceConfigTest.java, v 0.1 2022年01月25日 4:46 下午 zhaowang
 */
public class AbstractInterfaceConfigTest {

    @Test
    public void testDefaultValue() {
        TestConfig defaultConfig = new TestConfig();
        assertNotNull(defaultConfig.getApplication());
        assertEquals(null, defaultConfig.getInterfaceId());
        assertEquals("", defaultConfig.getUniqueId());
        assertEquals(null, defaultConfig.getFilterRef());
        assertEquals(null, defaultConfig.getFilter());
        assertEquals(null, defaultConfig.getRegistry());
        assertEquals(null, defaultConfig.getMethods());
        assertEquals("hessian2", defaultConfig.getSerialization());
        assertEquals(true, defaultConfig.isRegister());
        assertEquals(true, defaultConfig.isSubscribe());
        assertEquals("javassist", defaultConfig.getProxy());
        assertEquals("", defaultConfig.getGroup());
        assertEquals("", defaultConfig.getVersion());
        assertEquals(null, defaultConfig.getCacheRef());
        assertEquals(null, defaultConfig.getMockRef());
        assertEquals(null, defaultConfig.getParameters());
        assertEquals(false, defaultConfig.isCache());
        assertEquals(null, defaultConfig.getMockMode());
        assertEquals(false, defaultConfig.isMock());
        assertEquals(false, defaultConfig.isValidation());
        assertEquals(null, defaultConfig.getCompress());
        assertEquals(null, defaultConfig.getVirtualInterfaceId());
        assertEquals(null, defaultConfig.getConfigValueCache());
        assertEquals(null, defaultConfig.getProxyClass());
        assertEquals(null, defaultConfig.getConfigListener());
    }

    @Test
    public void testSetGet() {
        TestConfig config = new TestConfig();
        config.setProxyClass(SampleService.class);
        assertEquals(SampleService.class, config.getProxyClass());

        ApplicationConfig application = new ApplicationConfig();
        String appName = randomString();
        application.setAppName(appName);
        config.setApplication(application);
        assertSame(application, config.getApplication());
        assertSame(appName, config.getAppName());

        config.setApplication(null);
        assertNotNull(config.getApplication());
        assertNotSame(application, config.getApplication());

        String interfaceId = TestUtils.randomString();
        String virtualInterfaceId = TestUtils.randomString();
        assertNull(config.getVirtualInterfaceId());
        assertNull(config.getInterfaceId());
        config.setInterfaceId(interfaceId);
        assertEquals(interfaceId, config.getInterfaceId());
        config.setVirtualInterfaceId(virtualInterfaceId);
        assertEquals(virtualInterfaceId, config.getInterfaceId());

        String uniqueId = TestUtils.randomString();
        config.setUniqueId(uniqueId);
        assertEquals(uniqueId, config.getUniqueId());
        try {
            config.setUniqueId("/");
        } catch (SofaRpcRuntimeException e) {
            //ignore
            assertTrue(e.getMessage().contains("The value of config"));
        }
        assertEquals(uniqueId, config.getUniqueId());

        List<String> filterRefList = new ArrayList<>();
        config.setFilterRef(filterRefList);
        assertSame(filterRefList, config.getFilterRef());

        List<String> filterList = new ArrayList<>();
        config.setFilter(filterList);
        assertSame(filterList, config.getFilter());

        List<RegistryConfig> registryConfigs = new ArrayList<>();
        config.setRegistry(registryConfigs);
        assertSame(registryConfigs, config.getRegistry());

        Map<String, MethodConfig> methods = new HashMap<>();
        config.setMethods(methods);
        assertSame(methods, config.getMethods());

        String serialization = TestUtils.randomString();
        config.setSerialization(serialization);
        Assert.assertEquals(serialization, config.getSerialization());

        assertTrue(config.isRegister());
        config.setRegister(false);
        assertFalse(config.isRegister());

        assertTrue(config.isSubscribe());
        config.setSubscribe(false);
        assertFalse(config.isSubscribe());

        String proxy = randomString();
        config.setProxy(proxy);
        assertEquals(proxy, config.getProxy());

        String group = randomString();
        config.setGroup(group);
        assertEquals(group, config.getGroup());

        String version = randomString();
        config.setVersion(version);
        assertEquals(version, config.getVersion());

        Object mockRef = new Object();
        config.setMockRef(mockRef);
        assertSame(mockRef, config.getMockRef());

        Map<String, String> parameters = new HashMap<>();
        String key = randomString();
        String value = randomString();
        parameters.put(key, value);
        assertNull(config.getParameters());
        config.setParameters(parameters);
        assertNotNull(config.getParameters());
        assertNotSame(parameters, config.getParameters());
        assertEquals(value, config.getParameters().get(key));

        assertFalse(config.isMock());
        config.setMock(true);
        assertTrue(config.isMock());

        ConfigListener configListener = new ConfigListener() {
            @Override
            public void configChanged(Map newValue) {

            }

            @Override
            public void attrUpdated(Map newValue) {

            }
        };
        config.setConfigListener(configListener);
        assertSame(configListener, config.getConfigListener());
    }

    @Test
    public void testSetMockMode_remote() {
        TestConfig config = new TestConfig();
        config.setMockMode(MockMode.REMOTE);
        assertEquals(MockMode.REMOTE, config.getMockMode());
        assertTrue(config.isMock());
    }

    @Test
    public void testSetMockMode_local() {
        TestConfig config = new TestConfig();
        config.setMockMode(MockMode.LOCAL);
        assertEquals(MockMode.LOCAL, config.getMockMode());
        assertTrue(config.isMock());
    }

    @Test
    public void testSetMockMode_other() {
        TestConfig config = new TestConfig();
        String otherMode = "123";
        config.setMockMode(otherMode);
        assertEquals(otherMode, config.getMockMode());
        assertFalse(config.isMock());
    }

    @Test
    public void testSetParameter() {
        TestConfig config = new TestConfig();
        String key = randomString();
        String value = randomString();
        assertNull(config.getParameter(key));
        config.setParameter(key, value);
        assertEquals(value, config.getParameter(key));

        config.setParameter(key, null);
        assertNull(config.getParameter(key));
    }

    @Test
    public void testConfigValueCache() {
        TestConfig config = new TestConfig();
        assertNull(config.getConfigValueCache());
        Map<String, String> configValueCache1 = config.getConfigValueCache(false);
        assertNotNull(configValueCache1);
        Map<String, String> configValueCache2 = config.getConfigValueCache(true);
        assertNotSame(configValueCache1, configValueCache2);
        Map<String, String> configValueCache3 = config.getConfigValueCache(false);
        assertSame(configValueCache2, configValueCache3);
    }

    @Test
    public void testConfigValueCacheOrder() {
        TestConfig config = new TestConfig();
        String compressKey = "compress";
        String compressInterfaceField = "interfaceField";
        String compressInterfaceParam = "interfaceParam";
        String compressMethodField = "methodField";
        String compressMethodParam = "methodParam";

        // field has higher priority
        config.setParameter(compressKey, compressInterfaceParam);
        assertEquals(compressInterfaceParam, config.getConfigValueCache(true).get(compressKey));
        config.setCompress(compressInterfaceField);
        assertEquals(compressInterfaceField, config.getConfigValueCache(true).get(compressKey));

        // field has higher priority, method config and interface config is separate
        String methodName = randomString();
        String methodKey = "." + methodName + "." + compressKey;
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName(methodName);
        methodConfig.setParameter(compressKey, compressMethodParam);
        ArrayList<MethodConfig> methods = new ArrayList<>();
        methods.add(methodConfig);
        config.setMethods(methods);
        assertEquals(compressMethodParam, config.getConfigValueCache(true).get(methodKey));
        methodConfig.setCompress(compressMethodField);
        assertEquals(compressMethodField, config.getConfigValueCache(true).get(methodKey));
    }

    @Test
    public void testQueryAttribute() {
        String compressKey = "compress";
        String methodName = randomString();
        String methodKey = "." + methodName + "." + compressKey;
        String interfaceCompress = randomString();
        String methodCompress = randomString();

        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName(methodName);
        methodConfig.setCompress(methodCompress);
        List<MethodConfig> methods = new ArrayList<>();
        methods.add(methodConfig);

        TestConfig config = new TestConfig();
        config.setCompress(interfaceCompress);
        config.setMethods(methods);
        assertEquals(interfaceCompress, config.queryAttribute(compressKey));
        assertEquals(methodCompress, config.queryAttribute(methodKey));

        // test fail
        try {
            config.queryAttribute(".str");
            fail();
        } catch (SofaRpcRuntimeException e) {
            // ignore
        }

        String invalidKey = randomString();
        try {
            config.queryAttribute(invalidKey);
            fail();
        } catch (SofaRpcRuntimeException e) {
            // ignore
        }

    }

    @Test
    public void testUpdateAttribute() {
        String compressKey = "compress";
        String methodName = randomString();
        String methodKey = "." + methodName + "." + compressKey;
        String oldInterfaceValue = randomString();
        String newInterfaceValue = randomString();
        String oldMethodValue = randomString();
        String newMethodValue = randomString();

        // update interface attribute
        TestConfig config = new TestConfig();
        config.setCompress(oldInterfaceValue);
        assertEquals(oldInterfaceValue, config.getCompress());
        assertTrue(config.updateAttribute(compressKey, newInterfaceValue, false));
        ;
        // value not overwrited
        assertEquals(oldInterfaceValue, config.getCompress());
        assertTrue(config.updateAttribute(compressKey, newInterfaceValue, true));
        assertEquals(newInterfaceValue, config.getCompress());
        assertFalse(config.updateAttribute(compressKey, newInterfaceValue, true));

        // update method attribute
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName(methodName);
        methodConfig.setCompress(oldMethodValue);
        List<MethodConfig> methods = new ArrayList<>();
        methods.add(methodConfig);
        config.setMethods(methods);
        assertEquals(oldMethodValue, config.queryAttribute(methodKey));
        assertTrue(config.updateAttribute(methodKey, newMethodValue, false));
        assertEquals(oldMethodValue, config.queryAttribute(methodKey));
        assertTrue(config.updateAttribute(methodKey, newMethodValue, true));
        assertEquals(newMethodValue, config.queryAttribute(methodKey));
        assertFalse(config.updateAttribute(methodKey, newMethodValue, true));

        String timeoutKey = "timeout";
        String anotherMethodName = randomString();
        String methodTimeout = "." + anotherMethodName + "." + timeoutKey;
        assertTrue(config.updateAttribute(methodTimeout, "10", true));

        Map<String, MethodConfig> methodConfigMap = config.getMethods();
        assertEquals((Integer) 10, methodConfigMap.get(anotherMethodName).getTimeout());
        ;

    }

    @Test(expected = SofaRpcRuntimeException.class)
    public void testUniqueIdCheck() throws NoSuchFieldException, IllegalAccessException {
        RpcConfigs.putValue(RpcOptions.RPC_UNIQUEID_PATTERN_CHECK, true);
        TestConfig config = new TestConfig();
        config.setProxyClass(SampleService.class);
        String uniqueId = TestUtils.randomString() + "$";
        config.setUniqueId(uniqueId);
    }

    @Test
    public void testUniqueIdCheckDisabled() {
        RpcConfigs.putValue(RpcOptions.RPC_UNIQUEID_PATTERN_CHECK, false);
        TestConfig config = new TestConfig();
        config.setProxyClass(SampleService.class);
        String uniqueId = TestUtils.randomString() + "$";
        config.setUniqueId(uniqueId);
        assertEquals(uniqueId, config.getUniqueId());
    }

    static class TestConfig extends AbstractInterfaceConfig {

        @Override
        protected Class<?> getProxyClass() {
            return proxyClass;
        }

        @Override
        protected String buildKey() {
            return null;
        }

        @Override
        public boolean hasTimeout() {
            return false;
        }

        @Override
        public boolean hasConcurrents() {
            return false;
        }
    }

}