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
package com.alipay.sofa.rpc.codec.jackson;

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:zhiyuan.lzy@antfin.com">zhiyuan.lzy</a>
 */
public class JacksonHelper {

    /**
     * Request service and method cache {service+method:class}
     */
    private ConcurrentHashMap<String, Class> requestClassCache  = new ConcurrentHashMap<String, Class>();

    /**
     * Response service and method cache {service+method:class}
     */
    private ConcurrentHashMap<String, Class> responseClassCache = new ConcurrentHashMap<String, Class>();

    /**
     * Fetch request class for cache according  service and method
     *
     * @param service    interface name
     * @param methodName method name
     * @return request class
     */
    public Class getReqClass(String service, String methodName) {

        String key = buildMethodKey(service, methodName);
        Class reqClass = requestClassCache.get(key);
        if (reqClass == null) {
            //read interface and method from cache
            String interfaceClass = ConfigUniqueNameGenerator.getInterfaceName(service);
            Class clazz = ClassUtils.forName(interfaceClass, true);
            loadClassToCache(key, clazz, methodName);
        }
        return requestClassCache.get(key);
    }

    /**
     * Fetch result class for cache according  service and method
     *
     * @param service    interface name
     * @param methodName method name
     * @return response class
     */
    public Class getResClass(String service, String methodName) {
        String key = service + "#" + methodName;
        Class reqClass = responseClassCache.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = ConfigUniqueNameGenerator.getInterfaceName(service);
            Class clazz = ClassUtils.forName(interfaceClass, true);
            loadClassToCache(key, clazz, methodName);
        }
        return responseClassCache.get(key);
    }

    /**
     * build cache key
     *
     * @param serviceName interface name
     * @param methodName  method name
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
            throw new SofaRpcRuntimeException("Cannot found method: " + clazz.getName() + "." + methodName);
        }
        Class[] parameterTypes = pbMethod.getParameterTypes();
        if (parameterTypes == null
            || parameterTypes.length != 1) {
            throw new SofaRpcRuntimeException("class based jackson: " + clazz.getName()
                + ", only support one parameter!");
        }
        Class reqClass = parameterTypes[0];
        requestClassCache.put(key, reqClass);
        Class resClass = pbMethod.getReturnType();
        if (resClass == void.class) {
            throw new SofaRpcRuntimeException("class based jackson: " + clazz.getName()
                + ", do not support void return type!");
        }
        responseClassCache.put(key, resClass);
    }
}
