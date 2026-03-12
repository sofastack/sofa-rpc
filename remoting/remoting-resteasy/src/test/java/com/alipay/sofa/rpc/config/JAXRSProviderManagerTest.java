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
package com.alipay.sofa.rpc.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Unit tests for JAXRSProviderManager
 *
 * @author SOFA-RPC Team
 */
public class JAXRSProviderManagerTest {

    @Test
    public void testRegisterInternalProviderClass() {
        Set<Class> initialClasses = JAXRSProviderManager.getInternalProviderClasses();
        int initialSize = initialClasses.size();

        JAXRSProviderManager.registerInternalProviderClass(String.class);
        Assert.assertEquals(initialSize + 1, JAXRSProviderManager.getInternalProviderClasses().size());

        JAXRSProviderManager.removeInternalProviderClass(String.class);
        Assert.assertEquals(initialSize, JAXRSProviderManager.getInternalProviderClasses().size());
    }

    @Test
    public void testRegisterCustomProviderInstance() {
        Set<Object> initialProviders = JAXRSProviderManager.getCustomProviderInstances();
        int initialSize = initialProviders.size();

        Object provider = new Object();
        JAXRSProviderManager.registerCustomProviderInstance(provider);
        Assert.assertEquals(initialSize + 1, JAXRSProviderManager.getCustomProviderInstances().size());

        JAXRSProviderManager.removeCustomProviderInstance(provider);
        Assert.assertEquals(initialSize, JAXRSProviderManager.getCustomProviderInstances().size());
    }

    @Test
    public void testGetInternalProviderClasses() {
        Set<Class> classes = JAXRSProviderManager.getInternalProviderClasses();
        Assert.assertNotNull(classes);
        Assert.assertTrue(classes instanceof java.util.concurrent.CopyOnWriteArraySet ||
            classes.getClass().getName().contains("SynchronizedSet") ||
            classes.getClass().getName().contains("Collections"));
    }

    @Test
    public void testGetCustomProviderInstances() {
        Set<Object> providers = JAXRSProviderManager.getCustomProviderInstances();
        Assert.assertNotNull(providers);
        Assert.assertTrue(providers instanceof java.util.concurrent.CopyOnWriteArraySet ||
            providers.getClass().getName().contains("SynchronizedSet") ||
            providers.getClass().getName().contains("Collections"));
    }

    @Test
    public void testIsCglibProxyClass() {
        Assert.assertFalse(JAXRSProviderManager.isCglibProxyClass(String.class));
        Assert.assertFalse(JAXRSProviderManager.isCglibProxyClass(Object.class));
    }

    @Test
    public void testIsCglibProxyClassName() {
        Assert.assertFalse(JAXRSProviderManager.isCglibProxyClassName("java.lang.String"));
        Assert.assertFalse(JAXRSProviderManager.isCglibProxyClassName(null));
        Assert.assertTrue(JAXRSProviderManager.isCglibProxyClassName("com.example.MyClass$$EnhancerByCGLIB"));
    }

    @Test
    public void testGetTargetClass() {
        Object obj = new Object();
        Class<?> targetClass = JAXRSProviderManager.getTargetClass(obj);
        Assert.assertEquals(Object.class, targetClass);
    }
}
