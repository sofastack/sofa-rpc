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

import com.alipay.sofa.rpc.common.utils.ClassUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.message.MessageBuilder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * JDK代理处理器，拦截请求变为invocation进行调用
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class JDKInvocationHandler implements InvocationHandler {

    /**
     * 代理类
     */
    private Class   proxyClass;

    /**
     * 代理调用器
     */
    private Invoker proxyInvoker;

    /**
     * Instantiates a new Jdk invocation handler.
     *
     * @param proxyClass   the proxy class
     * @param proxyInvoker the proxy invoker
     */
    public JDKInvocationHandler(Class proxyClass, Invoker proxyInvoker) {
        this.proxyClass = proxyClass;
        this.proxyInvoker = proxyInvoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] paramValues)
        throws Throwable {
        String methodName = method.getName();
        Class[] paramTypes = method.getParameterTypes();
        if ("toString".equals(methodName) && paramTypes.length == 0) {
            return proxyInvoker.toString();
        } else if ("hashCode".equals(methodName) && paramTypes.length == 0) {
            return proxyInvoker.hashCode();
        } else if ("equals".equals(methodName) && paramTypes.length == 1) {
            Object another = paramValues[0];
            return proxy == another ||
                (proxy.getClass().isInstance(another) && proxyInvoker.equals(JDKProxy.parseInvoker(another)));
        }
        SofaRequest sofaRequest = MessageBuilder.buildSofaRequest(method.getDeclaringClass(),
            method, paramTypes, paramValues);
        SofaResponse response = proxyInvoker.invoke(sofaRequest);
        if (response.isError()) {
            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, response.getErrorMsg());
        }
        Object ret = response.getAppResponse();
        if (ret instanceof Throwable) {
            throw (Throwable) ret;
        } else {
            if (ret == null) {
                return ClassUtils.getDefaultPrimitiveValue(method.getReturnType());
            }
            return ret;
        }
    }

    /**
     * Gets proxy class.
     *
     * @return the proxy class
     */
    public Class getProxyClass() {
        return proxyClass;
    }

    /**
     * Gets proxy invoker.
     *
     * @return the proxy invoker
     */
    public Invoker getProxyInvoker() {
        return proxyInvoker;
    }
}
