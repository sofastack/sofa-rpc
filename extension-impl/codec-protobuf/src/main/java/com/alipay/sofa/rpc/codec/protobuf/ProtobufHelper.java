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
package com.alipay.sofa.rpc.codec.protobuf;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.config.ConfigUniqueNameGenerator;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.LogCodes;
import com.google.protobuf.MessageLite;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProtobufHelper {

    /**
     * Support multiple classloader?
     */
    private static final boolean         MULTIPLE_CLASSLOADER = RpcConfigs
                                                                  .getBooleanValue(RpcOptions.MULTIPLE_CLASSLOADER_ENABLE);

    /**
     * Cache of parseFrom method
     */
    ConcurrentMap<Class, Method>         parseFromMethodMap   = new ConcurrentHashMap<Class, Method>();

    /**
     * Cache of toByteArray method
     */
    ConcurrentMap<Class, Method>         toByteArrayMethodMap = new ConcurrentHashMap<Class, Method>();

    /**
     * 请求参数类型缓存 {service+method:class}
     */
    private ConcurrentMap<String, Class> requestClassCache    = new ConcurrentHashMap<String, Class>();

    /**
     * 返回结果类型缓存 {service+method:class}
     */
    private ConcurrentMap<String, Class> responseClassCache   = new ConcurrentHashMap<String, Class>();

    /**
     * 从缓存中获取请求值类
     *
     * @param service    接口名
     * @param methodName 方法名
     * @return 请求参数类
     */
    public Class getReqClass(String service, String methodName) {

        String key = buildMethodKey(service, methodName);
        Class reqClass = requestClassCache.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = ConfigUniqueNameGenerator.getInterfaceName(service);
            Class clazz = ClassUtils.forName(interfaceClass, true);
            loadProtoClassToCache(key, clazz, methodName);
        }
        return requestClassCache.get(key);
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
        Class reqClass = responseClassCache.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = ConfigUniqueNameGenerator.getInterfaceName(service);
            Class clazz = ClassUtils.forName(interfaceClass, true);
            loadProtoClassToCache(key, clazz, methodName);
        }
        return responseClassCache.get(key);
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
     * 加载protobuf接口里方法的参数和返回值类型到缓存，不需要传递
     *
     * @param key        缓存的key
     * @param clazz      接口名
     * @param methodName 方法名
     */
    private void loadProtoClassToCache(String key, Class clazz, String methodName) {
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
            || parameterTypes.length != 1
            || isProtoBufMessageObject(parameterTypes[0])) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_ONLY_ONE_PARAM, "protobuf",
                clazz.getName()));
        }
        Class reqClass = parameterTypes[0];
        requestClassCache.put(key, reqClass);
        Class resClass = pbMethod.getReturnType();
        if (resClass == void.class || !isProtoBufMessageClass(resClass)) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_PROTOBUF_RETURN, clazz.getName()));
        }
        responseClassCache.put(key, resClass);
    }

    /**
     * Is this object instanceof MessageLite
     *
     * @param object unknown object
     * @return instanceof MessageLite
     */
    boolean isProtoBufMessageObject(Object object) {
        if (object == null) {
            return false;
        }
        if (MULTIPLE_CLASSLOADER) {
            return object instanceof MessageLite || isProtoBufMessageClass(object.getClass());
        } else {
            return object instanceof MessageLite;
        }
    }

    /**
     * Is this class is assignable from MessageLite
     *
     * @param clazz unknown class
     * @return is assignable from MessageLite
     */
    boolean isProtoBufMessageClass(Class clazz) {
        return clazz != null && ClassUtils.isAssignableFrom(MessageLite.class, clazz);
    }
}
