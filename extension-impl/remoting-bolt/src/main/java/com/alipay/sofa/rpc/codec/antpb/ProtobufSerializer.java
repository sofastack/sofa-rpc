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
import com.alipay.sofa.rpc.common.RpcConstants;
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
     * 方法缓存下 
     */
    private ConcurrentHashMap<Class, Method> parseMethodMap = new ConcurrentHashMap<Class, Method>();

    private static ProtobufSerializer        instance       = new ProtobufSerializer();

    private ProtobufSerializer() {

    }

    public static ProtobufSerializer getInstance() {
        return instance;
    }

    public byte[] encode(Object object) throws SerializationException {
        if (object == null) {
            throw new SerializationException("Unsupported null message");
        } else if (object instanceof MessageLite) {
            MessageLite lite = (MessageLite) object;
            return lite.toByteArray();
        } else if (object instanceof String) {
            return ((String) object).getBytes(RpcConstants.DEFAULT_CHARSET);
        } else {
            throw new SerializationException("Unsupported class:" + object.getClass().getName()
                + ", only support protobuf message");
        }
    }

    public Object decode(byte[] bytes, Class clazz) throws DeserializationException {
        if (MessageLite.class.isAssignableFrom(clazz)) {
            try {
                Method method = parseMethodMap.get(clazz);
                if (method == null) {
                    method = clazz.getMethod("parseFrom", byte[].class);
                    if (!Modifier.isStatic(method.getModifiers())) {
                        throw new CodecException("Cannot found method " + clazz.getName()
                            + ".parseFrom(byte[]), please check the generated code");
                    }
                    method.setAccessible(true);
                    parseMethodMap.put(clazz, method);
                }
                return method.invoke(null, bytes);
            } catch (DeserializationException e) {
                throw e;
            } catch (Exception e) {
                throw new DeserializationException("Cannot found method " + clazz.getName()
                    + ".parseFrom(byte[]), please check the generated code", e);
            }
        } else if (clazz == String.class) {
            return new String(bytes, RpcConstants.DEFAULT_CHARSET);
        } else {
            throw new DeserializationException("Unsupported class:" + clazz.getName()
                + ", only support protobuf message");
        }
    }

    /**
     * 请求参数类型缓存 {service+method:class}
     */
    private static ConcurrentHashMap<String, Class> REQUEST_CLASS_CACHE  = new ConcurrentHashMap<String, Class>();

    /**
     * 返回结果类型缓存 {service+method:class}
     */
    private static ConcurrentHashMap<String, Class> RESPONSE_CLASS_CACHE = new ConcurrentHashMap<String, Class>();

    /**
     * 从缓存中获取请求值类
     *
     * @param service    接口名
     * @param methodName 方法名
     * @return 请求参数类
     * @throws ClassNotFoundException 无法找到该接口
     * @throws CodecException         其它序列化异常
     */
    public static Class getReqClass(String service, String methodName, ClassLoader classLoader)
        throws ClassNotFoundException, CodecException {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        String key = buildMethodKey(service, methodName);
        Class reqClass = REQUEST_CLASS_CACHE.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = service.contains(":") ? service.substring(0, service.indexOf(':')) : service;
            Class clazz = Class.forName(interfaceClass, true, classLoader);
            loadProtoClassToCache(key, clazz, methodName);
        }
        return REQUEST_CLASS_CACHE.get(key);
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
    public static Class getResClass(String service, String methodName, ClassLoader classLoader)
        throws ClassNotFoundException, CodecException {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        String key = service + "#" + methodName;
        Class reqClass = RESPONSE_CLASS_CACHE.get(key);
        if (reqClass == null) {
            // 读取接口里的方法参数和返回值
            String interfaceClass = service.contains(":") ? service.substring(0, service.indexOf(':')) : service;
            Class clazz = Class.forName(interfaceClass, true, classLoader);
            loadProtoClassToCache(key, clazz, methodName);
        }
        return RESPONSE_CLASS_CACHE.get(key);
    }

    /**
     * 拼装缓存的key
     *
     * @param serviceName 接口名
     * @param methodName  方法名
     * @return Key
     */
    private static String buildMethodKey(String serviceName, String methodName) {
        return serviceName + "#" + methodName;
    }

    /**
     * 加载protobuf接口里方法的参数和返回值类型到缓存，不需要传递
     *
     * @param key        缓存的key
     * @param clazz      接口名
     * @param methodName 方法名
     * @throws CodecException         其它序列化异常
     */
    private static void loadProtoClassToCache(String key, Class clazz, String methodName) throws CodecException {
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
            || !MessageLite.class.isAssignableFrom(parameterTypes[0])) {
            throw new CodecException("class based protobuf: " + clazz.getName()
                + ", only support one protobuf parameter!");
        }
        Class reqClass = parameterTypes[0];
        REQUEST_CLASS_CACHE.put(key, reqClass);
        Class resClass = pbMethod.getReturnType();
        if (resClass == void.class || !MessageLite.class.isAssignableFrom(resClass)) {
            throw new CodecException("class based protobuf: " + clazz.getName()
                + ", only support return protobuf message!");
        }
        RESPONSE_CLASS_CACHE.put(key, resClass);
    }
}
