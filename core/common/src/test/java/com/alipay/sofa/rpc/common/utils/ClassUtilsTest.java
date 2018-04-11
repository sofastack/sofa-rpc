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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 *
 */
public class ClassUtilsTest {

    @Test
    public void forName1() throws Exception {
        Class clazz = ClassUtils.forName("java.lang.String");
        Assert.assertEquals(clazz, String.class);
        boolean error = false;
        try {
            ClassUtils.forName("asdasdasdsad");
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void forName2() throws Exception {
        Class clazz = ClassUtils.forName("java.lang.String", false);
        Assert.assertEquals(clazz, String.class);
        boolean error = false;
        try {
            ClassUtils.forName("asdasdasdsad", true);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void forName3() throws Exception {
        Class clazz = ClassUtils.forName("java.lang.String", Thread.currentThread().getContextClassLoader());
        Assert.assertEquals(clazz, String.class);
        boolean error = false;
        try {
            ClassUtils.forName("asdasdasdsad", Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void getAllMethods() throws Exception {
        List<Method> methods = ClassUtils.getAllMethods(TestBean.class);
        Assert.assertTrue(methods.size() >= 8);
    }

    @Test
    public void getAllFields() throws Exception {
        List<Field> fields = ClassUtils.getAllFields(TestBean.class);
        Assert.assertEquals(fields.size(), 4);
    }

    @Test
    public void testNewInstance() throws Exception {
        short s = ClassUtils.newInstance(short.class);
        Assert.assertTrue(s == 0);
        Short s2 = ClassUtils.newInstance(Short.class);
        Assert.assertTrue(s2 == 0);

        int i = ClassUtils.newInstance(int.class);
        Assert.assertTrue(i == 0);
        Integer integer = ClassUtils.newInstance(Integer.class);
        Assert.assertTrue(integer == 0);

        long l = ClassUtils.newInstance(long.class);
        Assert.assertTrue(l == 0);
        Long l2 = ClassUtils.newInstance(Long.class);
        Assert.assertTrue(l2 == 0);

        double d = ClassUtils.newInstance(double.class);
        Assert.assertTrue(d == 0.0d);
        Double d2 = ClassUtils.newInstance(Double.class);
        Assert.assertTrue(d2 == 0.0d);

        float f = ClassUtils.newInstance(float.class);
        Assert.assertTrue(f == 0.0f);
        Float f2 = ClassUtils.newInstance(Float.class);
        Assert.assertTrue(f2 == 0.0f);

        byte b = ClassUtils.newInstance(byte.class);
        Assert.assertTrue(b == 0);
        Byte b2 = ClassUtils.newInstance(Byte.class);
        Assert.assertTrue(b2 == 0);

        char c = ClassUtils.newInstance(char.class);
        Assert.assertTrue(c == 0);
        Character c2 = ClassUtils.newInstance(Character.class);
        Assert.assertTrue(c2 == 0);

        boolean bl = ClassUtils.newInstance(boolean.class);
        Assert.assertFalse(bl);
        Boolean bl2 = ClassUtils.newInstance(Boolean.class);
        Assert.assertFalse(bl2);

        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass1.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass2.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass3.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass4.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass5.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestMemberClass6.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestClass1.class));
        Assert.assertNotNull(ClassUtils.newInstance(TestClass2.class));
        TestClass3 class3 = ClassUtils.newInstance(TestClass3.class);
        Assert.assertNotNull(class3);
        Assert.assertNull(class3.getName());
        Assert.assertEquals(class3.getAge(), 0);

    }

    @Test
    public void testNewInstanceWithArgs() throws Exception {
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestMemberClass3.class, null, null));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestMemberClass3.class,
            new Class[] { String.class }, new Object[] { "2222" }));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestMemberClass6.class, null, null));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestMemberClass6.class,
            new Class[] { int.class }, new Object[] { 222 }));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestClass3.class, null, null));
        Assert.assertNotNull(ClassUtils.newInstanceWithArgs(TestClass3.class,
            new Class[] { String.class, int.class }, new Object[] { "xxx", 222 }));
    }

    @Test
    public void getMethodKey() throws Exception {
        Assert.assertEquals(ClassUtils.getMethodKey("xxx", "yyy"), "xxx#yyy");
    }

    @Test
    public void getDefaultArg() throws Exception {
        Assert.assertEquals((short) 0, ClassUtils.getDefaultArg(short.class));
        Assert.assertEquals(0, ClassUtils.getDefaultArg(int.class));
        Assert.assertEquals(0l, ClassUtils.getDefaultArg(long.class));
        Assert.assertEquals(0d, ClassUtils.getDefaultArg(double.class));
        Assert.assertEquals(0f, ClassUtils.getDefaultArg(float.class));
        Assert.assertEquals((byte) 0, ClassUtils.getDefaultArg(byte.class));
        Assert.assertEquals((char) 0, ClassUtils.getDefaultArg(char.class));
        Assert.assertEquals(false, ClassUtils.getDefaultArg(boolean.class));

        Assert.assertEquals((short) 0, ClassUtils.getDefaultArg(Short.class));
        Assert.assertEquals(0, ClassUtils.getDefaultArg(Integer.class));
        Assert.assertEquals(0l, ClassUtils.getDefaultArg(Long.class));
        Assert.assertEquals(0d, ClassUtils.getDefaultArg(Double.class));
        Assert.assertEquals(0f, ClassUtils.getDefaultArg(Float.class));
        Assert.assertEquals((byte) 0, ClassUtils.getDefaultArg(Byte.class));
        Assert.assertEquals((char) 0, ClassUtils.getDefaultArg(Character.class));
        Assert.assertEquals(false, ClassUtils.getDefaultArg(Boolean.class));

        Assert.assertEquals(null, ClassUtils.getDefaultArg(Void.class));
        Assert.assertEquals(null, ClassUtils.getDefaultArg(void.class));
        Assert.assertEquals(null, ClassUtils.getDefaultArg(String.class));
    }

    private static class TestMemberClass1 {

    }

    private static class TestMemberClass2 {
        private TestMemberClass2() {
            System.out.println("init TestMemberClass2 ");
        }
    }

    private static class TestMemberClass3 {
        private TestMemberClass3(String s) {
            System.out.println("init TestMemberClass3 ");
        }

        private TestMemberClass3(String s, int i) {
            System.out.println("init TestMemberClass3 with 2 arg");
        }
    }

    private class TestMemberClass4 {

    }

    private class TestMemberClass5 {
        private TestMemberClass5() {
            System.out.println("init TestMemberClass5 ");
        }
    }

    private class TestMemberClass6 {
        private TestMemberClass6(int s) {
            System.out.println("init TestMemberClass6 ");
        }

        private TestMemberClass6(String s, int i) {
            System.out.println("init TestMemberClass6 with 2 arg");
        }
    }

}