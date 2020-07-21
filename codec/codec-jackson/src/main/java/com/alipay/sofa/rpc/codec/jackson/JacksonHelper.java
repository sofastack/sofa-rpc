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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.LogCodes;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author <a href="mailto:zhiyuan.lzy@antfin.com">zhiyuan.lzy</a>
 */
public class JacksonHelper {

    private ObjectMapper                          mapper             = new ObjectMapper();

    /**
     * Request service and method cache {service+method:class}
     */
    private ConcurrentHashMap<String, JavaType[]> requestClassCache  = new ConcurrentHashMap<String, JavaType[]>();

    /**
     * Response service and method cache {service+method:class}
     */
    private ConcurrentHashMap<String, JavaType>   responseClassCache = new ConcurrentHashMap<String, JavaType>();

    /**
     * Fetch request class for cache according  service and method
     *
     * @param service    interface name
     * @param methodName method name
     * @return request class
     */
    public JavaType[] getReqClass(String service, String methodName) {

        String key = buildMethodKey(service, methodName);
        Type[] reqClassList = requestClassCache.get(key);
        if (reqClassList == null) {
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
    public JavaType getResClass(String service, String methodName) {
        String key = service + "#" + methodName;
        JavaType reqType = responseClassCache.get(key);
        if (reqType == null) {
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
        Method jsonMethod = null;
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (methodName.equals(method.getName())) {
                jsonMethod = method;
                break;
            }
        }
        if (jsonMethod == null) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_METHOD_NOT_FOUND, clazz.getName(),
                methodName));
        }

        // parse request types
        Type[] parameterTypes = jsonMethod.getGenericParameterTypes();
        JavaType[] javaTypes = new JavaType[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            JavaType javaType = mapper.getTypeFactory().constructType(parameterTypes[i]);
            javaTypes[i] = javaType;
        }
        requestClassCache.put(key, javaTypes);

        // parse response types
        Type resType = jsonMethod.getGenericReturnType();
        if (resType == void.class) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_VOID_RETURN, "jackson", clazz.getName()));
        }
        JavaType resJavaType = mapper.getTypeFactory().constructType(resType);
        responseClassCache.put(key, resJavaType);
    }
}
