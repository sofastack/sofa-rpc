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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射工具类
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ReflectUtils {

    /**
     * 是否默认类型，基本类型+string+date
     *
     * @param clazz the cls
     * @return the boolean
     */
    public static boolean isPrimitives(Class<?> clazz) {
        if (clazz.isArray()) { // 数组，检查数组类型
            return isPrimitiveType(clazz.getComponentType());
        }
        return isPrimitiveType(clazz);
    }

    private static boolean isPrimitiveType(Class<?> clazz) {
        return clazz.isPrimitive() // 基本类型
            // 基本类型的对象
            ||
            Boolean.class == clazz
            || Character.class == clazz
            || Number.class.isAssignableFrom(clazz)
            // string 或者 date
            || String.class == clazz
            || Date.class.isAssignableFrom(clazz);
    }

    /**
     * 得到类所在地址，可以是文件，也可以是jar包
     *
     * @param cls the cls
     * @return the code base
     */
    public static String getCodeBase(Class<?> cls) {

        if (cls == null) {
            return null;
        }
        ProtectionDomain domain = cls.getProtectionDomain();
        if (domain == null) {
            return null;
        }
        CodeSource source = domain.getCodeSource();
        if (source == null) {
            return null;
        }
        URL location = source.getLocation();
        if (location == null) {
            return null;
        }
        return location.getFile();
    }

    /**
     * 方法对象缓存 {接口名#方法名#(参数列表):Method} <br>
     * 用于用户传了参数列表
     */
    private final static ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<String, Method>();

    /**
     * 加载Method方法，如果cache找不到，则新反射一个
     *
     * @param clazzName  类名
     * @param methodName 方法名
     * @param argsType   参数列表
     * @return Method对象
     * @throws ClassNotFoundException 如果指定的类加载器无法定位该类
     * @throws NoSuchMethodException  如果找不到匹配的方法
     */
    public static Method getMethod(String clazzName, String methodName, String[] argsType)
        throws ClassNotFoundException, NoSuchMethodException {
        StringBuilder sb = new StringBuilder(256);
        sb.append(clazzName).append("#").append(methodName).append("(");
        if (argsType != null && argsType.length > 0) {
            for (String argType : argsType) {
                sb.append(argType).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");

        String key = sb.toString();
        // 先从缓存里取
        Method method = METHOD_CACHE.get(key);
        if (method == null) {
            Class clazz = ClassUtils.forName(clazzName);
            Class[] classes = ClassTypeUtils.getClasses(argsType);
            method = clazz.getMethod(methodName, classes);
            Method old = METHOD_CACHE.putIfAbsent(key, method);
            if (old != null) {
                method = old;
            }
        }
        return method;
    }

    /**
     * 加载Method方法，如果cache找不到，则新反射一个
     *
     * @param clazz      类
     * @param methodName 方法名
     * @param argsType   参数列表
     * @return Method对象
     * @throws ClassNotFoundException 如果指定的类加载器无法定位该类
     * @throws NoSuchMethodException  如果找不到匹配的方法
     */
    public static Method getMethod(Class clazz, String methodName, String[] argsType)
        throws NoSuchMethodException, ClassNotFoundException {
        return getMethod(clazz.getCanonicalName(), methodName, argsType);
    }

    /**
     * 方法对象缓存 {接口名#方法名:Method}<br>
     * 用于用户没传了参数列表
     */
    private final static ConcurrentHashMap<String, Class[]> METHOD_ARGS_TYPE_CACHE = new ConcurrentHashMap<String, Class[]>();

    /**
     * 缓存接口的方法，重载方法会被覆盖
     *
     * @param interfaceId 接口名
     * @param methodName  方法名
     * @param argsType    参数列表
     */
    public static void cacheMethodArgsType(String interfaceId, String methodName, Class[] argsType) {
        String key = interfaceId + "#" + methodName;
        METHOD_ARGS_TYPE_CACHE.put(key, argsType);
    }

    /**
     * 缓存接口的方法，重载方法会被覆盖，没有则返回空
     *
     * @param interfaceId 接口类
     * @param methodName  方法名
     * @return 方法的参数列表，没有则返回空
     */
    public static Class[] getMethodArgsType(String interfaceId, String methodName) {
        String key = interfaceId + "#" + methodName;
        return METHOD_ARGS_TYPE_CACHE.get(key);
    }

    /**
     * 得到set方法
     *
     * @param clazz         类
     * @param property      属性
     * @param propertyClazz 属性
     * @return Method 方法对象
     * @throws NoSuchMethodException 没找到
     */
    public static Method getPropertySetterMethod(Class clazz, String property, Class propertyClazz)
        throws NoSuchMethodException {
        String methodName = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
        try {
            Method method = clazz.getMethod(methodName, propertyClazz);
            return method;
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException("No setter method for " + clazz.getName() + "#" + property);
        }
    }

    /**
     * 得到get/is方法
     *
     * @param clazz    类
     * @param property 属性
     * @return Method 方法对象
     * @throws NoSuchMethodException 没找到方法
     */
    public static Method getPropertyGetterMethod(Class clazz, String property) throws NoSuchMethodException {
        String methodName = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
        Method method;
        try {
            method = clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            try {
                methodName = "is" + property.substring(0, 1).toUpperCase() + property.substring(1);
                method = clazz.getMethod(methodName);
            } catch (NoSuchMethodException e1) {
                throw new NoSuchMethodException("No getter method for " + clazz.getName() + "#" + property);
            }
        }
        return method;
    }

    protected static boolean isBeanPropertyReadMethod(Method method) {
        return method != null
            && Modifier.isPublic(method.getModifiers())
            && !Modifier.isStatic(method.getModifiers())
            && method.getReturnType() != void.class
            && method.getDeclaringClass() != Object.class
            && method.getParameterTypes().length == 0
            && (method.getName().startsWith("get") || method.getName().startsWith("is"))
            && (!"get".equals(method.getName()) && !"is".equals(method.getName())); // 排除就叫get和is的方法
    }

    protected static String getPropertyNameFromBeanReadMethod(Method method) {
        if (isBeanPropertyReadMethod(method)) {
            if (method.getName().startsWith("get")) {
                return method.getName().substring(3, 4).toLowerCase()
                    + method.getName().substring(4);
            }
            if (method.getName().startsWith("is")) {
                return method.getName().substring(2, 3).toLowerCase()
                    + method.getName().substring(3);
            }
        }
        return null;
    }

    protected static boolean isBeanPropertyWriteMethod(Method method) {
        return method != null
            && Modifier.isPublic(method.getModifiers())
            && !Modifier.isStatic(method.getModifiers())
            && method.getDeclaringClass() != Object.class
            && method.getParameterTypes().length == 1
            && method.getName().startsWith("set")
            && !"set".equals(method.getName()); // 排除就叫set的方法
    }

    protected static boolean isPublicInstanceField(Field field) {
        return Modifier.isPublic(field.getModifiers())
            && !Modifier.isStatic(field.getModifiers())
            && !Modifier.isFinal(field.getModifiers())
            && !field.isSynthetic();
    }
}
