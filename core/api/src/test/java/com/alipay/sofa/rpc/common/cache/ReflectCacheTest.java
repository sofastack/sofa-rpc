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
package com.alipay.sofa.rpc.common.cache;

import com.alipay.sofa.rpc.common.utils.ClassLoaderUtils;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.common.utils.ReflectUtils;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ReflectCacheTest {

    @Test
    public void testAppClassLoader() {
        URLClassLoader cl1 = new URLClassLoader(new URL[0]);
        URLClassLoader cl2 = new URLClassLoader(new URL[0]);
        ReflectCache.registerAppClassLoader("xxx", cl1);
        ReflectCache.registerAppClassLoader("yyy", cl2);
        Assert.assertEquals(cl1, ReflectCache.getAppClassLoader("xxx"));
        Assert.assertEquals(cl2, ReflectCache.getAppClassLoader("yyy"));
        Assert.assertEquals(ClassLoaderUtils.getCurrentClassLoader(), ReflectCache.getAppClassLoader("zzz"));

        ReflectCache.APPNAME_CLASSLOADER_MAP.clear();
    }

    @Test
    public void testServiceClassLoader() {
        URLClassLoader cl1 = new URLClassLoader(new URL[0]);
        URLClassLoader cl2 = new URLClassLoader(new URL[0]);
        ReflectCache.registerServiceClassLoader("xxx", cl1);
        ReflectCache.registerServiceClassLoader("yyy", cl2);
        Assert.assertEquals(cl1, ReflectCache.getServiceClassLoader("xxx"));
        Assert.assertEquals(cl2, ReflectCache.getServiceClassLoader("yyy"));
        Assert.assertEquals(ClassLoaderUtils.getCurrentClassLoader(),
            ReflectCache.getServiceClassLoader("zzz"));

        ReflectCache.SERVICE_CLASSLOADER_MAP.clear();
    }

    @Test
    public void testClassCache() {
        ReflectCache.putClassCache(TestInterface.class.getCanonicalName(), TestInterface.class);
        Assert.assertEquals(TestInterface.class, ReflectCache.getClassCache(TestInterface.class.getCanonicalName()));
    }

    @Test
    public void testTypeStrCache() {
        ReflectCache.putTypeStrCache(TestInterface.class, TestInterface.class.getCanonicalName());
        Assert.assertEquals(TestInterface.class.getCanonicalName(), ReflectCache.getTypeStrCache(TestInterface.class));
    }

    @Test
    public void testMethodCache() {
        final String key = TestInterface.class.getCanonicalName();
        final Method method = ReflectUtils.getMethod(TestInterface.class, "invoke", SofaRequest.class);
        Assert.assertNull(ReflectCache.getMethodCache(key, "invoke"));

        ReflectCache.putMethodCache(key, method);
        Assert.assertEquals(method, ReflectCache.getMethodCache(key, "invoke"));

        ReflectCache.invalidateMethodCache(key);
        Assert.assertNull(ReflectCache.NOT_OVERLOAD_METHOD_CACHE.get(key));
    }

    @Test
    public void testMethodSigs() {
        final String key = TestInterface.class.getCanonicalName();
        final Method method = ReflectUtils.getMethod(TestInterface.class, "invoke", SofaRequest.class);
        Assert.assertNull(ReflectCache.getMethodSigsCache(key, "invoke"));

        ReflectCache.putMethodSigsCache(key, method.getName(),
            ClassTypeUtils.getTypeStrs(method.getParameterTypes(), true));
        Assert.assertArrayEquals(new String[] { SofaRequest.class.getCanonicalName() },
            ReflectCache.getMethodSigsCache(key, "invoke"));

        ReflectCache.invalidateMethodSigsCache(key);
        Assert.assertNull(ReflectCache.NOT_OVERLOAD_METHOD_CACHE.get(key));
    }

    @Test
    public void testOverloadMethodCache() {
        final String key = TestInterface2.class.getCanonicalName();
        final Method method1 = ReflectUtils.getMethod(TestInterface2.class, "invoke", SofaRequest.class);
        final Method method2 = ReflectUtils.getMethod(TestInterface2.class, "invoke", SofaRequest.class, String.class);
        Assert.assertNull(ReflectCache.getOverloadMethodCache(key, "invoke",
            new String[] { SofaRequest.class.getCanonicalName(), String.class.getCanonicalName() }));
        Assert.assertNull(ReflectCache.getOverloadMethodCache(key, "invoke",
            new String[] { SofaRequest.class.getCanonicalName() }));

        ReflectCache.putOverloadMethodCache(key, method1);
        ReflectCache.putOverloadMethodCache(key, method2);
        Assert.assertEquals(method1, ReflectCache.getOverloadMethodCache(key, "invoke",
            new String[] { SofaRequest.class.getCanonicalName() }));
        Assert.assertEquals(method2, ReflectCache.getOverloadMethodCache(key, "invoke",
            new String[] { SofaRequest.class.getCanonicalName(), String.class.getCanonicalName() }));

        ReflectCache.invalidateOverloadMethodCache(key);
        Assert.assertNull(ReflectCache.NOT_OVERLOAD_METHOD_CACHE.get(key));
    }

    @AfterClass
    public static void testClear() {
        ReflectCache.clearAll();
        Assert.assertTrue(ReflectCache.CLASS_CACHE.size() == 0);
        Assert.assertTrue(ReflectCache.TYPE_STR_CACHE.size() == 0);
        Assert.assertTrue(ReflectCache.APPNAME_CLASSLOADER_MAP.size() == 0);
        Assert.assertTrue(ReflectCache.SERVICE_CLASSLOADER_MAP.size() == 0);
        Assert.assertTrue(ReflectCache.NOT_OVERLOAD_METHOD_CACHE.size() == 0);
        Assert.assertTrue(ReflectCache.NOT_OVERLOAD_METHOD_SIGS_CACHE.size() == 0);
        Assert.assertTrue(ReflectCache.OVERLOAD_METHOD_CACHE.size() == 0);
    }

    interface TestInterface {
        SofaResponse invoke(SofaRequest request);
    }

    interface TestInterface2 {
        SofaResponse invoke(SofaRequest request);

        SofaResponse invoke(SofaRequest request, String type);
    }
}