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
package com.alipay.sofa.rpc.codec.antpb;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.google.protobuf.MessageLite;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protobuf serializer.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ProtobufSerializer {

    /**
     * Singleton instance
     */
    private static final ProtobufSerializer  INSTANCE             = new ProtobufSerializer();
    /**
     * Encode method name
     */
    private static final String              METHOD_TOBYTEARRAY   = "toByteArray";
    /**
     * Decode method name
     */
    private static final String              METHOD_PARSEFROM     = "parseFrom";
    /**
     * Support multiple classloader?
     */
    private static final boolean             MULTIPLE_CLASSLOADER = RpcConfigs
                                                                      .getBooleanValue(RpcOptions.MULTIPLE_CLASSLOADER_ENABLE);

    /**
     * Cache of parseFrom method
     */
    private ConcurrentHashMap<Class, Method> parseFromMethodMap   = new ConcurrentHashMap<Class, Method>();

    /**
     * Cache of toByteArray method
     */
    private ConcurrentHashMap<Class, Method> toByteArrayMethodMap = new ConcurrentHashMap<Class, Method>();

    /**
     * 请求参数类型缓存 {service+method:class}
     */
    private ConcurrentHashMap<String, Class> requestClassCache    = new ConcurrentHashMap<String, Class>();

    /**
     * 返回结果类型缓存 {service+method:class}
     */
    private ConcurrentHashMap<String, Class> responseClassCache   = new ConcurrentHashMap<String, Class>();

    /**
     * Can not be new instance.
     */
    private ProtobufSerializer() {
    }

    /**
     * Get singleton instance
     *
     * @return Singleton ProtobufSerializer
     */
    public static ProtobufSerializer getInstance() {
        return INSTANCE;
    }

    /**
     * Encode protobuf message to byte array.
     *
     * @param object protobuf message
     * @return byte[]
     * @throws SerializationException Serialization Exception
     */
    public byte[] encode(Object object) throws SerializationException {
        if (object == null) {
            throw new SerializationException("Unsupported null message!");
        } else if (isProtoBufMessageObject(object)) {
            Class clazz = object.getClass();
            Method method = toByteArrayMethodMap.get(clazz);
            if (method == null) {
                try {
                    method = clazz.getMethod(METHOD_TOBYTEARRAY);
                    method.setAccessible(true);
                    toByteArrayMethodMap.put(clazz, method);
                } catch (Exception e) {
                    throw new SerializationException("Cannot found method " + clazz.getName()
                        + ".toByteArray(), please check the generated code.", e);
                }
            }
            try {
                return (byte[]) method.invoke(object);
            } catch (Exception e) {
                throw new SerializationException("Error when invoke " + clazz.getName() + ".toByteArray().", e);
            }
        } else if (object instanceof String) {
            return ((String) object).getBytes(RpcConstants.DEFAULT_CHARSET);
        } else {
            throw new SerializationException("Unsupported class:" + object.getClass().getName()
                + ", only support protobuf message");
        }
    }

    /**
     * Decode byte array to protobuf message.
     *
     * @param bytes byte[]
     * @param clazz protobuf message class
     * @return protobuf message
     * @throws DeserializationException Deserialization Exception
     */
    public Object decode(byte[] bytes, Class clazz) throws DeserializationException {
        if (clazz == null) {
            throw new DeserializationException("class is null!");
        } else if (isProtoBufMessageClass(clazz)) {
            Method method = parseFromMethodMap.get(clazz);
            if (method == null) {
                try {
                    method = clazz.getMethod(METHOD_PARSEFROM, byte[].class);
                    if (!Modifier.isStatic(method.getModifiers())) {
                        throw new DeserializationException("Cannot found static method " + clazz.getName()
                            + ".parseFrom(byte[]), please check the generated code");
                    }
                    method.setAccessible(true);
                    parseFromMethodMap.put(clazz, method);
                } catch (NoSuchMethodException e) {
                    throw new DeserializationException("Cannot found method " + clazz.getName()
                        + ".parseFrom(byte[]), please check the generated code", e);
                }
            }
            try {
                return method.invoke(null, bytes);
            } catch (Exception e) {
                throw new DeserializationException("Error when invoke " + clazz.getName() + ".parseFrom(byte[]).", e);
            }
        } else if (clazz == String.class) {
            return new String(bytes, RpcConstants.DEFAULT_CHARSET);
        } else {
            throw new DeserializationException("Unsupported class:" + clazz.getName()
                + ", only support protobuf message");
        }
    }

    /**
     * 从缓存中获取请求值类
     *
     * @param service    接口名
     * @param methodName 方法名
     * @return 请求参数类
     * @throws ClassNotFoundException 无法找到该接口
     * @throws CodecException         其它序列化异常
     */
    public Class getReqClass(String service, String methodName, ClassLoader classLoader)
        throws ClassNotFoundException, CodecException {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        String key = buildMethodKey(service, methodName);
        Class reqClass = requestClassCache.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = service.contains(":") ? service.substring(0, service.indexOf(':')) : service;
            Class clazz = Class.forName(interfaceClass, true, classLoader);
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
     * @throws ClassNotFoundException 无法找到该接口
     * @throws CodecException         其它序列化异常
     */
    public Class getResClass(String service, String methodName, ClassLoader classLoader)
        throws ClassNotFoundException, CodecException {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        String key = service + "#" + methodName;
        Class reqClass = responseClassCache.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = service.contains(":") ? service.substring(0, service.indexOf(':')) : service;
            Class clazz = Class.forName(interfaceClass, true, classLoader);
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
     * @throws CodecException 其它序列化异常
     */
    private void loadProtoClassToCache(String key, Class clazz, String methodName) throws CodecException {
        Method pbMethod = null;
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (methodName.equals(method.getName())) {
                pbMethod = method;
                break;
            }
        }
        if (pbMethod == null) {
            throw new CodecException("Cannot found protobuf method: " + clazz.getName() + "." + methodName);
        }
        Class[] parameterTypes = pbMethod.getParameterTypes();
        if (parameterTypes == null
            || parameterTypes.length != 1
            || isProtoBufMessageObject(parameterTypes[0])) {
            throw new CodecException("class based protobuf: " + clazz.getName()
                + ", only support one protobuf parameter!");
        }
        Class reqClass = parameterTypes[0];
        requestClassCache.put(key, reqClass);
        Class resClass = pbMethod.getReturnType();
        if (resClass == void.class || !isProtoBufMessageClass(resClass)) {
            throw new CodecException("class based protobuf: " + clazz.getName()
                + ", only support return protobuf message!");
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
