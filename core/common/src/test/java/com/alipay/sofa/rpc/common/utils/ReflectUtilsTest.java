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
package com.alipay.sofa.rpc.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class ReflectUtilsTest {
    @Test
    public void isPrimitives() throws Exception {
        Assert.assertTrue(ReflectUtils.isPrimitives(int.class));
        Assert.assertTrue(ReflectUtils.isPrimitives(int[].class));
        Assert.assertTrue(ReflectUtils.isPrimitives(Integer.class));
        Assert.assertTrue(ReflectUtils.isPrimitives(Boolean.class));
        Assert.assertTrue(ReflectUtils.isPrimitives(Character.class));
        Assert.assertTrue(ReflectUtils.isPrimitives(String.class));
        Assert.assertTrue(ReflectUtils.isPrimitives(Date.class));
        Assert.assertTrue(ReflectUtils.isPrimitives(BigDecimal.class));
        Assert.assertFalse(ReflectUtils.isPrimitives(List.class));
    }

    @Test
    public void getCodeBase() throws Exception {
        String codeBase = ReflectUtils.getCodeBase(ReflectUtilsTest.class);
        Assert.assertNotNull(codeBase);
    }

    @Test
    public void cacheMethodArgsType() throws Exception {
    }

    @Test
    public void getMethodArgsType() throws Exception {
    }

    @Test
    public void getPropertySetterMethod() throws Exception {
    }

    @Test
    public void getPropertyGetterMethod() throws Exception {
    }

    @Test
    public void isBeanPropertyReadMethod() throws Exception {
    }

    @Test
    public void getPropertyNameFromBeanReadMethod() throws Exception {
    }

    @Test
    public void isBeanPropertyWriteMethod() throws Exception {
    }

    @Test
    public void isPublicInstanceField() throws Exception {
    }

    @Test
    public void testGetCodeBase() throws Exception {
        String dir = System.getProperty("user.dir");
        String codebase = ReflectUtils.getCodeBase(ReflectUtils.class);
        System.out.println(codebase);
        Assert.assertNotNull(codebase);

        String codebase2 = ReflectUtils.getCodeBase(ReflectUtilsTest.class);
        System.out.println(codebase2);
        Assert.assertNotNull(codebase2);
    }

    @Test
    public void testGetMethod() throws Exception {
        String className = "com.alipay.sofa.rpc.common.utils.TestBean";
        String methodName = "setAlias";
        String[] argsType1 = new String[] { "java.lang.String" };

        Method method = ReflectUtils.getMethod(className, methodName, argsType1);

        Method method3 = ClassUtils.forName(className).getMethod(methodName,
            ClassTypeUtils.getClasses(argsType1));
        Assert.assertFalse(method == method3);
        Assert.assertTrue(method.equals(method3));

        int times = 1000000;
        long start = System.nanoTime();
        for (int i = 0; i < times; i++) {
            ReflectUtils.getMethod(className, methodName, argsType1);
        }
        long end = System.nanoTime();
        System.out.println("get method " + times / 10000 + "w times elaspe " + (end - start) / 1000 / 1000 + "ms");

        start = System.nanoTime();
        for (int i = 0; i < times; i++) {
            Class clazz = ClassUtils.forName(className);
            Class[] classes = ClassTypeUtils.getClasses(argsType1);
            method = clazz.getMethod(methodName, classes);
        }
        end = System.nanoTime();
        System.out.println("get method " + times / 10000 + "w times with no cache elaspe " + (end - start) / 1000 /
            1000 + "ms");
    }

}