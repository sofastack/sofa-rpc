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
package com.alipay.sofa.rpc.proxy.bytebuddy;

import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.proxy.Proxy;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bystander
 * @version $Id: BytebuddyProxy.java, v 0.1 2019年01月29日 20:18 bystander Exp $
 */
@Extension("bytebuddy")
public class BytebuddyProxy implements Proxy {

    /**
     * 原始类和代理类的映射
     */
    private static final Map<Class, Class> PROXY_CLASS_MAP = new ConcurrentHashMap<Class, Class>();

    @Override
    public <T> T getProxy(Class<T> interfaceClass, Invoker proxyInvoker) {

        Class<? extends T> cls = PROXY_CLASS_MAP.get(interfaceClass);
        if (cls == null) {
            cls = new ByteBuddy()
                .subclass(interfaceClass)
                .method(
                    ElementMatchers.isDeclaredBy(interfaceClass).or(ElementMatchers.isEquals())
                        .or(ElementMatchers.isToString().or(ElementMatchers.isHashCode())))
                .intercept(MethodDelegation.to(new BytebuddyInvocationHandler(proxyInvoker), "handler"))
                .make()
                .load(interfaceClass.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();

            PROXY_CLASS_MAP.put(interfaceClass, cls);
        }
        try {
            return cls.newInstance();
        } catch (Throwable t) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_PROXY_CONSTRUCT, "bytebuddy"), t);
        }

    }

    @Override
    public Invoker getInvoker(Object proxyObject) {
        return parseInvoker(proxyObject);
    }

    public static Invoker parseInvoker(Object proxyObject) {
        try {
            Field field = proxyObject.getClass().getField("handler");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            BytebuddyInvocationHandler interceptor = (BytebuddyInvocationHandler) field.get(proxyObject);

            return interceptor.getProxyInvoker();
        } catch (Exception e) {
            return null;
        }
    }
}