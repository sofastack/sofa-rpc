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
package com.alipay.sofa.rpc.codec.fury;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.alipay.sofa.rpc.common.utils.ClassTypeUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.LogCodes;

/**
 * @author lipan
 */
public class FuryHelper {
    private final ConcurrentMap<String, Class<?>[]> requestClassCache  = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Class<?>[]> responseClassCache = new ConcurrentHashMap<>();

    public Class<?>[] getReqClass(String service, String methodName) {
        return getClasses(service, methodName, requestClassCache);
    }

    public Class<?>[] getRespClass(String service, String methodName) {
        return getClasses(service, methodName, responseClassCache);
    }

    private Class<?>[] getClasses(String service, String methodName,
                                  ConcurrentMap<String, Class<?>[]> responseClassCache) {
        String key = buildMethodKey(service, methodName);
        Class<?>[] respClass = responseClassCache.get(key);
        if (respClass == null) {
            String interfaceClass = ConfigUniqueNameGenerator.getInterfaceName(service);
            Class<?> clazz = ClassTypeUtils.getClass(interfaceClass);
            loadClassToCache(key, clazz, methodName);
        } else {
            // Check if the class loader has changed due to hot update
            String interfaceClass = ConfigUniqueNameGenerator.getInterfaceName(service);
            ClassLoader currentClassLoader = getClassLoader(interfaceClass);
            if (!currentClassLoader.equals(respClass[0].getClassLoader())) {
                Class<?> clazz = ClassTypeUtils.getClass(interfaceClass);
                loadClassToCache(key, clazz, methodName);
            }
        }
        return responseClassCache.get(key);
    }

    private String buildMethodKey(String serviceName, String methodName) {
        return serviceName + "#" + methodName;
    }

    private void loadClassToCache(String key, Class<?> clazz, String methodName) {
        if (clazz == null) {
            throw new SofaRpcRuntimeException("Failed to load : " + key);
        }
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
        Class<?>[] parameterTypes = pbMethod.getParameterTypes();
        if (parameterTypes.length == 0) {
            throw new SofaRpcRuntimeException(LogCodes.getLog("fury", clazz.getName()));
        }

        requestClassCache.put(key, parameterTypes);
        Class<?> respClass = pbMethod.getReturnType();
        if (respClass == void.class) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_PROTOBUF_RETURN, clazz.getName()));
        }
        responseClassCache.put(key, new Class[] { respClass });
    }

    public ClassLoader getClassLoader(String className) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = FuryHelper.class.getClassLoader();
        }
        Class<?> clazz = ClassTypeUtils.getClass(className);
        if (clazz != null) {
            return clazz.getClassLoader();
        } else {
            return classLoader;
        }
    }
}