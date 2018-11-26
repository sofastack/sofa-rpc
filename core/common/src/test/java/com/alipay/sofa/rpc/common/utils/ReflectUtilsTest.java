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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.alipay.sofa.rpc.common.utils.ReflectUtils.getPropertyGetterMethod;
import static com.alipay.sofa.rpc.common.utils.ReflectUtils.getPropertyNameFromBeanReadMethod;
import static com.alipay.sofa.rpc.common.utils.ReflectUtils.getPropertySetterMethod;
import static com.alipay.sofa.rpc.common.utils.ReflectUtils.isBeanPropertyReadMethod;
import static com.alipay.sofa.rpc.common.utils.ReflectUtils.isBeanPropertyWriteMethod;
import static com.alipay.sofa.rpc.common.utils.ReflectUtils.isPublicInstanceField;

public class ReflectUtilsTest {

    private final Logger LOGGER = LoggerFactory.getLogger(ReflectUtilsTest.class);

    @Test
    public void isPrimitives() {
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
    public void getCodeBase() {
        Assert.assertNull(ReflectUtils.getCodeBase(null));

        String codebase = ReflectUtils.getCodeBase(ReflectUtils.class);
        LOGGER.info(codebase);
        Assert.assertNotNull(codebase);

        String codebase2 = ReflectUtils.getCodeBase(ReflectUtilsTest.class);
        LOGGER.info(codebase2);
        Assert.assertNotNull(codebase2);
    }

    @Test
    public void testGetPropertySetterMethod() throws Exception {
        Method method = TestReflect.class.getMethod("setS", int.class);
        Assert.assertEquals(method, getPropertySetterMethod(TestReflect.class, "s", int.class));

        method = TestReflect.class.getMethod("setB", boolean.class);
        Assert.assertEquals(method, getPropertySetterMethod(TestReflect.class, "b", boolean.class));

        boolean error = false;
        try {
            getPropertySetterMethod(TestReflect.class, "xxx", String.class);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void testGetPropertyGetterMethod() throws Exception {
        Method method = TestReflect.class.getMethod("getS");
        Assert.assertEquals(method, getPropertyGetterMethod(TestReflect.class, "s"));

        method = TestReflect.class.getMethod("isB");
        Assert.assertEquals(method, getPropertyGetterMethod(TestReflect.class, "b"));

        boolean error = false;
        try {
            getPropertyGetterMethod(TestReflect.class, "xxx");
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void testIsBeanPropertyReadMethod() throws Exception {
        Assert.assertFalse(isBeanPropertyReadMethod(null));
        Assert.assertTrue(isBeanPropertyReadMethod(TestReflect.class.getMethod("getS")));
        Assert.assertFalse(isBeanPropertyReadMethod(TestReflect.class.getMethod("get")));
        Assert.assertFalse(isBeanPropertyReadMethod(TestReflect.class.getMethod("is")));
        Assert.assertFalse(isBeanPropertyReadMethod(TestReflect.class.getDeclaredMethod("get1")));
        Assert.assertFalse(isBeanPropertyReadMethod(TestReflect.class.getMethod("get2")));
        Assert.assertFalse(isBeanPropertyReadMethod(TestReflect.class.getMethod("get3")));
        Assert.assertFalse(isBeanPropertyReadMethod(TestReflect.class.getMethod("get4", String.class)));
        Assert.assertFalse(isBeanPropertyReadMethod(TestReflect.class.getMethod("aget5")));
        Assert.assertFalse(isBeanPropertyReadMethod(TestReflect.class.getMethod("ais5")));
    }

    @Test
    public void testGetPropertyNameFromBeanReadMethod() throws Exception {
        Method method = TestReflect.class.getMethod("getS");
        Assert.assertEquals("s", getPropertyNameFromBeanReadMethod(method));

        method = TestReflect.class.getMethod("getName");
        Assert.assertEquals("name", getPropertyNameFromBeanReadMethod(method));

        method = TestReflect.class.getMethod("isB");
        Assert.assertEquals("b", getPropertyNameFromBeanReadMethod(method));

        method = TestReflect.class.getMethod("is");
        Assert.assertNull(getPropertyNameFromBeanReadMethod(method));
    }

    @Test
    public void testIsBeanPropertyWriteMethod() throws Exception {
        Assert.assertFalse(isBeanPropertyWriteMethod(null));
        Assert.assertTrue(isBeanPropertyWriteMethod(TestReflect.class.getMethod("setS", int.class)));
        Assert.assertFalse(isBeanPropertyWriteMethod(TestReflect.class.getMethod("set", int.class)));
        Assert.assertFalse(isBeanPropertyWriteMethod(TestReflect.class.getDeclaredMethod("set1", int.class)));
        Assert.assertFalse(isBeanPropertyWriteMethod(TestReflect.class.getMethod("set2", int.class)));
        Assert.assertFalse(isBeanPropertyWriteMethod(TestReflect.class.getMethod("set3", int.class, int.class)));
        Assert.assertFalse(isBeanPropertyWriteMethod(TestReflect.class.getMethod("aset4", int.class)));
    }

    @Test
    public void testIsPublicInstanceField() throws Exception {
        Assert.assertTrue(isPublicInstanceField(TestReflect.class.getField("f1")));
        Assert.assertFalse(isPublicInstanceField(TestReflect.class.getDeclaredField("f2")));
        Assert.assertFalse(isPublicInstanceField(TestReflect.class.getDeclaredField("f3")));
        Assert.assertFalse(isPublicInstanceField(TestReflect.class.getDeclaredField("f4")));
        Assert.assertFalse(isPublicInstanceField(TestReflect.class.getDeclaredField("f5")));
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

        Method method4 = ReflectUtils.getMethod(TestBean.class, "setAlias", String.class);
        Assert.assertFalse(method == method4);
        Assert.assertTrue(method3.equals(method4));

        boolean error = false;
        try {
            ReflectUtils.getMethod(className, methodName + "xxx", argsType1);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }
}
