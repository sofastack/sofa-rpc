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

import com.alipay.sofa.rpc.common.annotation.VisibleForTesting;
import com.alipay.sofa.rpc.common.utils.ClassLoaderUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务要支持多ClassLoader，需要缓存ClassLoader或者方法等相关信息
 * <p>
 * // TODO 统一的回收实效策略，例如大小限制、时间限制、哪些可以被回收
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public final class ReflectCache {

    /*----------- ClassLoader Cache ------------*/
    /**
     * 应用对应的ClassLoader
     */
    @VisibleForTesting
    static final ConcurrentHashMap<String, ClassLoader> APPNAME_CLASSLOADER_MAP = new ConcurrentHashMap<String, ClassLoader>();

    /**
     * 服务对应的ClassLoader
     */
    @VisibleForTesting
    static final ConcurrentHashMap<String, ClassLoader> SERVICE_CLASSLOADER_MAP = new ConcurrentHashMap<String, ClassLoader>();

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

    /*----------- Class Cache ------------*/
    /**
     * String-->Class 缓存
     */
    @VisibleForTesting
    static final ConcurrentHashMap<String, Class> CLASS_CACHE    = new ConcurrentHashMap<String, Class>();

    /**
     * Class-->String 缓存
     */
    @VisibleForTesting
    static final ConcurrentHashMap<Class, String> TYPE_STR_CACHE = new ConcurrentHashMap<Class, String>();

    /**
     * 放入Class缓存
     *
     * @param typeStr 对象描述
     * @param clazz   类
     */
    public static void putClassCache(String typeStr, Class clazz) {
        CLASS_CACHE.put(typeStr, clazz);
    }

    /**
     * 得到Class缓存
     *
     * @param typeStr 对象描述
     * @return 类
     */
    public static Class getClassCache(String typeStr) {
        return CLASS_CACHE.get(typeStr);
    }

    /**
     * 放入类描述缓存
     *
     * @param clazz   类
     * @param typeStr 对象描述
     */
    public static void putTypeStrCache(Class clazz, String typeStr) {
        TYPE_STR_CACHE.put(clazz, typeStr);
    }

    /**
     * 得到类描述缓存
     *
     * @param clazz 类
     * @return 类描述
     */
    public static String getTypeStrCache(Class clazz) {
        return TYPE_STR_CACHE.get(clazz);
    }

    /*----------- Method Cache NOT support overload ------------*/

    /**
     * 不支持重载的方法对象缓存 {service:{方法名:Method}}
     */
    @VisibleForTesting
    static final ConcurrentHashMap<String, ConcurrentHashMap<String, Method>>   NOT_OVERLOAD_METHOD_CACHE      = new ConcurrentHashMap<String, ConcurrentHashMap<String, Method>>();

    /**
     * 不支持重载的方法对象参数签名缓存 {service:{方法名:对象参数签名}}
     */
    @VisibleForTesting
    static final ConcurrentHashMap<String, ConcurrentHashMap<String, String[]>> NOT_OVERLOAD_METHOD_SIGS_CACHE = new ConcurrentHashMap<String, ConcurrentHashMap<String, String[]>>();

    /**
     * 往缓存里放入方法
     *
     * @param serviceName 服务名（非接口名）
     * @param method      方法
     */
    public static void putMethodCache(String serviceName, Method method) {
        ConcurrentHashMap<String, Method> cache = NOT_OVERLOAD_METHOD_CACHE.get(serviceName);
        if (cache == null) {
            cache = new ConcurrentHashMap<String, Method>();
            ConcurrentHashMap<String, Method> old = NOT_OVERLOAD_METHOD_CACHE.putIfAbsent(serviceName, cache);
            if (old != null) {
                cache = old;
            }
        }
        cache.putIfAbsent(method.getName(), method);
    }

    /**
     * 从缓存里获取方法
     *
     * @param serviceName 服务名（非接口名）
     * @param methodName  方法名
     * @return 方法
     */
    public static Method getMethodCache(String serviceName, String methodName) {
        ConcurrentHashMap<String, Method> methods = NOT_OVERLOAD_METHOD_CACHE.get(serviceName);
        return methods == null ? null : methods.get(methodName);
    }

    /**
     * 根据服务名使方法缓存失效
     *
     * @param serviceName 服务名（非接口名）
     */
    public static void invalidateMethodCache(String serviceName) {
        NOT_OVERLOAD_METHOD_CACHE.remove(serviceName);
    }

    /**
     * 往缓存里放入方法参数签名
     *
     * @param serviceName 服务名（非接口名）
     * @param methodName  方法名
     * @param argSigs     方法参数签名
     */
    public static void putMethodSigsCache(String serviceName, String methodName, String[] argSigs) {
        ConcurrentHashMap<String, String[]> cacheSigs = NOT_OVERLOAD_METHOD_SIGS_CACHE.get(serviceName);
        if (cacheSigs == null) {
            cacheSigs = new ConcurrentHashMap<String, String[]>();
            ConcurrentHashMap<String, String[]> old = NOT_OVERLOAD_METHOD_SIGS_CACHE
                .putIfAbsent(serviceName, cacheSigs);
            if (old != null) {
                cacheSigs = old;
            }
        }
        cacheSigs.putIfAbsent(methodName, argSigs);
    }

    /**
     * 从缓存里获取方法参数签名
     *
     * @param serviceName 服务名（非接口名）
     * @param methodName  方法名
     * @return 方法参数签名
     */
    public static String[] getMethodSigsCache(String serviceName, String methodName) {
        ConcurrentHashMap<String, String[]> methods = NOT_OVERLOAD_METHOD_SIGS_CACHE.get(serviceName);
        return methods == null ? null : methods.get(methodName);
    }

    /**
     * 根据服务名使方法缓存失效
     *
     * @param serviceName 服务名（非接口名）
     */
    public static void invalidateMethodSigsCache(String serviceName) {
        NOT_OVERLOAD_METHOD_SIGS_CACHE.remove(serviceName);
    }

    /*----------- Method Cache support overload ------------*/

    /**
     * 方法对象缓存 {service:{方法名#(参数列表):Method}} <br>
     * 用于缓存参数列表，不是按接口，是按ServiceUniqueName
     */
    @VisibleForTesting
    final static ConcurrentHashMap<String, ConcurrentHashMap<String, Method>> OVERLOAD_METHOD_CACHE = new ConcurrentHashMap<String, ConcurrentHashMap<String, Method>>();

    /**
     * 往缓存里放入方法
     *
     * @param serviceName 服务名（非接口名）
     * @param method      方法
     */
    public static void putOverloadMethodCache(String serviceName, Method method) {
        ConcurrentHashMap<String, Method> cache = OVERLOAD_METHOD_CACHE.get(serviceName);
        if (cache == null) {
            cache = new ConcurrentHashMap<String, Method>();
            ConcurrentHashMap<String, Method> old = OVERLOAD_METHOD_CACHE.putIfAbsent(serviceName, cache);
            if (old != null) {
                cache = old;
            }
        }
        StringBuilder mSigs = new StringBuilder(128);
        mSigs.append(method.getName());
        for (Class<?> paramType : method.getParameterTypes()) {
            mSigs.append(paramType.getName());
        }
        cache.putIfAbsent(mSigs.toString(), method);
    }

    /**
     * 从缓存里获取方法
     *
     * @param serviceName 服务名（非接口名）
     * @param methodName  方法名
     * @param methodSigs  方法描述
     * @return 方法
     */
    public static Method getOverloadMethodCache(String serviceName, String methodName, String[] methodSigs) {
        ConcurrentHashMap<String, Method> methods = OVERLOAD_METHOD_CACHE.get(serviceName);
        if (methods == null) {
            return null;
        }
        StringBuilder mSigs = new StringBuilder(128);
        mSigs.append(methodName);
        for (String methodSign : methodSigs) {
            mSigs.append(methodSign);
        }
        return methods.get(mSigs.toString());
    }

    /**
     * 取消缓存服务的公共方法
     *
     * @param serviceName 服务名（非接口名）
     */
    public static void invalidateOverloadMethodCache(String serviceName) {
        OVERLOAD_METHOD_CACHE.remove(serviceName);
    }

    /*----------- Cache Management ------------*/
    /**
     * 清理方法
     */
    static void clearAll() {
        CLASS_CACHE.clear();
        TYPE_STR_CACHE.clear();
        APPNAME_CLASSLOADER_MAP.clear();
        SERVICE_CLASSLOADER_MAP.clear();
        NOT_OVERLOAD_METHOD_CACHE.clear();
        NOT_OVERLOAD_METHOD_SIGS_CACHE.clear();
        OVERLOAD_METHOD_CACHE.clear();
    }

}
