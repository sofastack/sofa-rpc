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
package com.alipay.sofa.rpc.common;

import com.alipay.sofa.rpc.common.utils.ClassLoaderUtils;
import com.alipay.sofa.rpc.common.utils.ClassUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务要支持多ClassLoader，需要缓存ClassLoader或者方法等相关信息
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ReflectCache {

    /**
     * 应用对应的ClassLoader
     */
    private static Map<String, ClassLoader> APPNAME_CLASSLOADER_MAP = new ConcurrentHashMap<String, ClassLoader>();

    /**
     * 服务对应的ClassLoader
     */
    private static Map<String, ClassLoader> SERVICE_CLASSLOADER_MAP = new ConcurrentHashMap<String, ClassLoader>();

    /**
     * 注册服务所在的ClassLoader
     *
     * @param appName     应用名
     * @param classloader 应用级别ClassLoader
     */
    public static void registerAppClassLoader(String appName, ClassLoader classloader) {
        APPNAME_CLASSLOADER_MAP.put(appName, classloader);
    }

    /**
     * 得到服务的自定义ClassLoader
     *
     * @param appName 应用名
     * @return 应用级别ClassLoader
     */
    public static ClassLoader getAppClassLoader(String appName) {
        ClassLoader appClassLoader = APPNAME_CLASSLOADER_MAP.get(appName);
        if (appClassLoader == null) {
            return ClassLoaderUtils.getCurrentClassLoader();
        } else {
            return appClassLoader;
        }
    }

    /**
     * 注册服务所在的ClassLoader
     *
     * @param serviceUniqueName 服务唯一名称
     * @param classloader       服务级别ClassLoader
     */
    public static void registerServiceClassLoader(String serviceUniqueName, ClassLoader classloader) {
        SERVICE_CLASSLOADER_MAP.put(serviceUniqueName, classloader);
    }

    /**
     * 得到服务的自定义ClassLoader
     *
     * @param serviceUniqueName 服务唯一名称
     * @return 服务级别ClassLoader
     */
    public static ClassLoader getServiceClassLoader(String serviceUniqueName) {
        ClassLoader appClassLoader = SERVICE_CLASSLOADER_MAP.get(serviceUniqueName);
        if (appClassLoader == null) {
            return ClassLoaderUtils.getCurrentClassLoader();
        } else {
            return appClassLoader;
        }
    }

    /**
     * 方法对象缓存 {service:{方法名#(参数列表):Method}} <br>
     * 用于缓存参数列表，不是按接口，是按ServiceUniqueName
     */
    private final static ConcurrentHashMap<String, Map<String, Method>> METHOD_CACHE = new ConcurrentHashMap<String, Map<String, Method>>();

    /**
     * 缓存服务的公共方法
     *
     * @param serviceUniqueName 服务唯一名称
     * @param clazz             接口类
     */
    public final static void putServiceMethodCache(String serviceUniqueName, Class clazz) {
        // 分析该POJO的所有公开方法
        Map<String, Method> publicMethods = new HashMap<String, Method>();
        for (Method m : clazz.getMethods()) {
            StringBuilder mSigs = new StringBuilder();
            mSigs.append(m.getName());
            for (Class<?> paramType : m.getParameterTypes()) {
                mSigs.append(paramType.getName());
            }
            publicMethods.put(mSigs.toString(), m);
        }
        METHOD_CACHE.put(serviceUniqueName, publicMethods);
    }

    /**
     * 取消缓存服务的公共方法
     *
     * @param serviceUniqueName 服务唯一名称
     */
    public static void invalidateServiceMethodCache(String serviceUniqueName) {
        METHOD_CACHE.remove(serviceUniqueName);
    }

    /**
     * 获取服务方法缓存
     *
     * @param serviceUniqueName 服务唯一名称
     * @param methodName        方法名
     * @param methodSigns       方法描述
     * @return 方法对象
     */
    public static Method getServiceMethod(String serviceUniqueName, String methodName, String[] methodSigns) {
        return getOrInitServiceMethod(serviceUniqueName, methodName, methodSigns, false, null);
    }

    /**
     * 获取服务方法缓存
     *
     * @param serviceUniqueName 服务唯一名称
     * @param methodName        方法名
     * @param methodSigns       方法描述
     * @param init              是否初始化
     * @return 方法对象
     */
    public static Method getOrInitServiceMethod(String serviceUniqueName, String methodName,
                                                String[] methodSigns, boolean init, String interfaceName) {
        Map<String, Method> map = METHOD_CACHE.get(serviceUniqueName);
        if (map == null) {
            if (init) {
                synchronized (ReflectCache.class) {
                    map = METHOD_CACHE.get(serviceUniqueName);
                    if (map == null) {
                        putServiceMethodCache(serviceUniqueName, ClassUtils.forName(interfaceName));
                        map = METHOD_CACHE.get(serviceUniqueName);
                    }
                }
            } else {
                return null;
            }
        }
        StringBuilder methodKeyBuffer = new StringBuilder();
        methodKeyBuffer.append(methodName);
        for (String methodSign : methodSigns) {
            methodKeyBuffer.append(methodSign);
        }
        return map.get(methodKeyBuffer.toString());
    }
}