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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * jax-rs的SPI管理器。
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class JAXRSProviderManager {

    /**
     * 内置的jaxrs Provider类
     */
    private static Set<Class>  internalProviderClasses = Collections.synchronizedSet(new LinkedHashSet<Class>());

    /**
     * 自定义jaxrs Provider实例
     */
    private static Set<Object> customProviderInstances = Collections.synchronizedSet(new LinkedHashSet<Object>());

    /**
     * 注册内置的jaxrs Provider类
     */
    public static void registerInternalProviderClass(Class provider) {
        internalProviderClasses.add(provider);
    }

    /**
     * remove internal jaxrs provider instace
     * @param provider
     */
    public static void removeInternalProviderClass(Class provider) {
        internalProviderClasses.remove(provider);
    }

    /**
     * 获取全部内置的jaxrs Provider类
     *
     * @return 全部内置的jaxrs Provider类
     */
    public static Set<Class> getInternalProviderClasses() {
        return internalProviderClasses;
    }

    /**
     * 注册自定义jaxrs Provider实例
     */
    public static void registerCustomProviderInstance(Object provider) {
        customProviderInstances.add(provider);
    }

    /**
     * remove custom jaxrs provider instace
     * @param provider
     */
    public static void removeCustomProviderInstance(Object provider) {
        customProviderInstances.remove(provider);
    }

    /**
     * 获取全部自定义jaxrs Provider实例
     *
     * @return 自定义jaxrs Provider实例
     */
    public static Set<Object> getCustomProviderInstances() {
        return customProviderInstances;
    }

    /**
     * The CGLIB class separator character "$$"
     */
    public static final String CGLIB_CLASS_SEPARATOR = "$$";

    /**
     * 拿到目标类型
     *
     * @param candidate 原始类
     * @return 目标类型
     */
    public static Class<?> getTargetClass(Object candidate) {
        return (isCglibProxyClass(candidate.getClass()) ? candidate.getClass().getSuperclass() : candidate.getClass());
    }

    /**
     * 是否是cglib代理过的类
     *
     * @param clazz 原始类
     * @return 是否代理类
     */
    public static boolean isCglibProxyClass(Class<?> clazz) {
        return (clazz != null && isCglibProxyClassName(clazz.getName()));
    }

    /**
     * 是否cglib代理过的类名
     *
     * @param className 原始类名
     * @return 是否代理类名
     */
    public static boolean isCglibProxyClassName(String className) {
        return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
    }
}