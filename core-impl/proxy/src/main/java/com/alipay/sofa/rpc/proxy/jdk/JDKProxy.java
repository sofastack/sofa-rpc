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
package com.alipay.sofa.rpc.proxy.jdk;

import com.alipay.sofa.rpc.common.utils.ClassLoaderUtils;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.proxy.Proxy;

import java.lang.reflect.InvocationHandler;

/**
 * Proxy implement base on jdk
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension("jdk")
public class JDKProxy implements Proxy {

    @Override
    public <T> T getProxy(Class<T> interfaceClass, Invoker proxyInvoker) {
        InvocationHandler handler = new JDKInvocationHandler(interfaceClass, proxyInvoker);
        ClassLoader classLoader = ClassLoaderUtils.getCurrentClassLoader();
        T result = (T) java.lang.reflect.Proxy.newProxyInstance(classLoader,
            new Class[] { interfaceClass }, handler);
        return result;
    }

    @Override
    public Invoker getInvoker(Object proxyObject) {
        return parseInvoker(proxyObject);
    }

    /**
     * Parse proxy invoker from proxy object
     *
     * @param proxyObject Proxy object
     * @return proxy invoker
     */
    public static Invoker parseInvoker(Object proxyObject) {
        InvocationHandler handler = java.lang.reflect.Proxy.getInvocationHandler(proxyObject);
        if (handler instanceof JDKInvocationHandler) {
            return ((JDKInvocationHandler) handler).getProxyInvoker();
        }
        return null;
    }
}
