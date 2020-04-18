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
package com.alipay.sofa.rpc.codec.msgpack;

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.LogCodes;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author leizhiyuan
 */
public class MsgPackHelper {
    /**
     * 请求参数类型缓存 {service+method:class}
     */
    private ConcurrentMap<String, Class> requestClassCache  = new ConcurrentHashMap<String, Class>();

    /**
     * 返回结果类型缓存 {service+method:class}
     */
    private ConcurrentMap<String, Class> responseClassCache = new ConcurrentHashMap<String, Class>();

    /**
     * 从缓存中获取请求值类
     *
     * @param service    接口名
     * @param methodName 方法名
     * @return 请求参数类
     */
    public Class getReqClass(String service, String methodName) {

        String key = buildMethodKey(service, methodName);
        return getCachedClass(service, methodName, key, requestClassCache);
    }

    /**
     * 从缓存中获取返回值类
     *
     * @param service    接口名
     * @param methodName 方法名
     * @return 请求参数类
     */
    public Class getResClass(String service, String methodName) {
        String key = service + "#" + methodName;
        return getCachedClass(service, methodName, key, responseClassCache);
    }

    private Class getCachedClass(String service, String methodName, String key,
                                 ConcurrentMap<String, Class> classCache) {
        Class reqClass = classCache.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = ConfigUniqueNameGenerator.getInterfaceName(service);
            Class clazz = ClassUtils.forName(interfaceClass, true);
            loadClassToCache(key, clazz, methodName);
        }
        return classCache.get(key);
    }

    /**
     * 拼装缓存的key
     *
     * @param serviceName 接口名
     * @param methodName  方法名
     * @return Key
     */
    private String buildMethodKey(String serviceName, String methodName) {
        return serviceName + "#" + methodName;
    }

    /**
     * load method paramters and return types to cache, will not pass through to next
     *
     * @param key        key
     * @param clazz      interface name
     * @param methodName method name
     */
    private void loadClassToCache(String key, Class clazz, String methodName) {
        Method pbMethod = null;
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (methodName.equals(method.getName())) {
                pbMethod = method;
                break;
            }
        }
        if (pbMethod == null) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_METHOD_NOT_FOUND, clazz.getName(),
                methodName));
        }
        Class[] parameterTypes = pbMethod.getParameterTypes();
        if (parameterTypes == null
            || parameterTypes.length != 1) {
            throw new SofaRpcRuntimeException(
                LogCodes.getLog(LogCodes.ERROR_ONLY_ONE_PARAM, "msgpack", clazz.getName()));
        }
        Class reqClass = parameterTypes[0];
        requestClassCache.put(key, reqClass);
        Class resClass = pbMethod.getReturnType();
        if (resClass == void.class) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_VOID_RETURN, "msgpack", clazz.getName()));
        }
        responseClassCache.put(key, resClass);
    }

    public boolean isJavaClass(Object object) {
        Class<?> clazz = object.getClass();
        return clazz != null && isJavaClass(clazz);
    }

    public boolean isJavaClass(Class clazz) {
        return clazz.getClassLoader() == null;
    }

}
