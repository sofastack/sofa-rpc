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

import com.alipay.sofa.rpc.common.cache.ReflectCache;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ClassTypeUtilsTest {

    //  匿名类
    Object anonymous = new Comparable<String>() {
                         @Override
                         public int compareTo(String o) {
                             return 0;
                         }
                     };

    // 成员类
    private class MemberClass {

    }

    @Test
    public void canonicalNameToJvmName() throws Exception {
    }

    @Test
    public void jvmNameToCanonicalName() throws Exception {
    }

    @Test
    public void testGetClass() {
        Assert.assertEquals(String.class, ClassTypeUtils.getClass("java.lang.String"));
        Assert.assertEquals(int.class, ClassTypeUtils.getClass("int"));

        Assert.assertEquals(boolean.class, ClassTypeUtils.getClass("boolean"));
        Assert.assertEquals(byte.class, ClassTypeUtils.getClass("byte"));
        Assert.assertEquals(char.class, ClassTypeUtils.getClass("char"));
        Assert.assertEquals(double.class, ClassTypeUtils.getClass("double"));
        Assert.assertEquals(float.class, ClassTypeUtils.getClass("float"));
        Assert.assertEquals(int.class, ClassTypeUtils.getClass("int"));
        Assert.assertEquals(long.class, ClassTypeUtils.getClass("long"));
        Assert.assertEquals(short.class, ClassTypeUtils.getClass("short"));
        Assert.assertEquals(void.class, ClassTypeUtils.getClass("void"));

        // 本地类
        class LocalType {

        }
        Assert.assertEquals(anonymous.getClass(),
            ClassTypeUtils.getClass("com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$1"));
        Assert.assertEquals(LocalType.class,
            ClassTypeUtils.getClass("com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$1LocalType"));
        Assert.assertEquals(MemberClass.class,
            ClassTypeUtils.getClass("com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$MemberClass"));
        Assert.assertEquals(StaticClass.class, ClassTypeUtils.getClass("com.alipay.sofa.rpc.common.utils.StaticClass"));

        Assert.assertEquals(String[].class, ClassTypeUtils.getClass("java.lang.String[]"));
        Assert.assertEquals(boolean[].class, ClassTypeUtils.getClass("boolean[]"));
        Assert.assertEquals(byte[].class, ClassTypeUtils.getClass("byte[]"));
        Assert.assertEquals(char[].class, ClassTypeUtils.getClass("char[]"));
        Assert.assertEquals(double[].class, ClassTypeUtils.getClass("double[]"));
        Assert.assertEquals(float[].class, ClassTypeUtils.getClass("float[]"));
        Assert.assertEquals(int[].class, ClassTypeUtils.getClass("int[]"));
        Assert.assertEquals(long[].class, ClassTypeUtils.getClass("long[]"));
        Assert.assertEquals(short[].class, ClassTypeUtils.getClass("short[]"));
        Assert.assertEquals(Array.newInstance(anonymous.getClass(), 2, 3).getClass(),
            ClassTypeUtils.getClass("com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$1[][]"));
        Assert.assertEquals(LocalType[][].class,
            ClassTypeUtils.getClass("com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$1LocalType[][]"));
        Assert.assertEquals(MemberClass[].class,
            ClassTypeUtils.getClass("com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$MemberClass[]"));
        Assert.assertEquals(StaticClass[].class,
            ClassTypeUtils.getClass("com.alipay.sofa.rpc.common.utils.StaticClass[]"));
        Assert.assertEquals(int[][].class, ClassTypeUtils.getClass("int[][]"));

        Assert.assertEquals(String[].class, ClassTypeUtils.getClass(String[].class.getName()));
        Assert.assertEquals(boolean[].class, ClassTypeUtils.getClass(boolean[].class.getName()));
        Assert.assertEquals(byte[].class, ClassTypeUtils.getClass(byte[].class.getName()));
        Assert.assertEquals(char[].class, ClassTypeUtils.getClass(char[].class.getName()));
        Assert.assertEquals(double[].class, ClassTypeUtils.getClass(double[].class.getName()));
        Assert.assertEquals(float[].class, ClassTypeUtils.getClass(float[].class.getName()));
        Assert.assertEquals(int[].class, ClassTypeUtils.getClass(int[].class.getName()));
        Assert.assertEquals(long[].class, ClassTypeUtils.getClass(long[].class.getName()));
        Assert.assertEquals(short[].class, ClassTypeUtils.getClass(short[].class.getName()));
        Assert.assertEquals(int[][].class, ClassTypeUtils.getClass(int[][].class.getName()));
    }

    @Test
    public void testGetClassAccordingToTCL() {
        // incompatible with JDK 9+
        URLClassLoader current = (URLClassLoader) this.getClass().getClassLoader();
        TempClassLoader t0 = new TempClassLoader(current.getURLs(), null);
        TempClassLoader t1 = new TempClassLoader(current.getURLs(), null);

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(t0);
        Class c0 = ClassTypeUtils.getClass(ClassTypeUtils.class.getCanonicalName());
        Thread.currentThread().setContextClassLoader(t1);
        Class c1 = ClassTypeUtils.getClass(ClassTypeUtils.class.getCanonicalName());

        Thread.currentThread().setContextClassLoader(t0);
        Assert.assertEquals(c0, ReflectCache.getClassCache(ClassTypeUtils.class.getCanonicalName()));
        Thread.currentThread().setContextClassLoader(t1);
        Assert.assertEquals(c1, ReflectCache.getClassCache(ClassTypeUtils.class.getCanonicalName()));
        Thread.currentThread().setContextClassLoader(old);
    }

    @Test
    public void testGetTypeStr() {

        Assert.assertEquals(ClassTypeUtils.getTypeStr(String.class), "java.lang.String");

        Assert.assertEquals(ClassTypeUtils.getTypeStr(boolean.class), "boolean");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(byte.class), "byte");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(char.class), "char");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(double.class), "double");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(float.class), "float");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(int.class), "int");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(long.class), "long");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(short.class), "short");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(void.class), "void");

        // 本地类
        class LocalType {

        }
        Assert.assertEquals(ClassTypeUtils.getTypeStr(anonymous.getClass()),
            "com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$1");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(LocalType.class),
            "com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$2LocalType");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(MemberClass.class),
            "com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$MemberClass");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(StaticClass.class),
            "com.alipay.sofa.rpc.common.utils.StaticClass");

        Assert.assertEquals(ClassTypeUtils.getTypeStr(String[][][].class), "java.lang.String[][][]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(boolean[].class), "boolean[]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(byte[].class), "byte[]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(char[].class), "char[]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(double[].class), "double[]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(float[].class), "float[]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(int[].class), "int[]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(long[].class), "long[]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(short[].class), "short[]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(Array.newInstance(anonymous.getClass(), 2, 3).getClass()),
            "com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$1[][]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(LocalType[][].class),
            "com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$2LocalType[][]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(MemberClass[].class),
            "com.alipay.sofa.rpc.common.utils.ClassTypeUtilsTest$MemberClass[]");
        Assert.assertEquals(ClassTypeUtils.getTypeStr(StaticClass[].class),
            "com.alipay.sofa.rpc.common.utils.StaticClass[]");

        Assert.assertArrayEquals(ClassTypeUtils.getTypeStrs(new Class[] { String[].class }),
            new String[] { "java.lang.String[]" });
        Assert.assertArrayEquals(ClassTypeUtils.getTypeStrs(new Class[] { String[].class }, false),
            new String[] { "java.lang.String[]" });
        Assert.assertArrayEquals(ClassTypeUtils.getTypeStrs(new Class[] { String[].class }, true),
            new String[] { String[].class.getName() });

    }

}

class StaticClass {

}

class TempClassLoader extends URLClassLoader {

    public TempClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}