/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.sofa.rpc.proxy.bytebuddy;

import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.proxy.Proxy;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;

/**
 * @author bystander
 * @version $Id: BytebuddyProxy.java, v 0.1 2019年01月29日 20:18 bystander Exp $
 */
@Extension("bytebuddy")
public class BytebuddyProxy implements Proxy {
    @Override
    public <T> T getProxy(Class<T> interfaceClass, Invoker proxyInvoker) {
        Class<? extends T> cls = new ByteBuddy()
                .subclass(interfaceClass)
                .method(ElementMatchers.isDeclaredBy(interfaceClass))
                .intercept(MethodDelegation.to(proxyInvoker, "handler"))
                .make()
                .load(interfaceClass.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();

        try {
            return cls.newInstance();
        } catch (Throwable t) {
            throw new SofaRpcRuntimeException("", t);
        }
    }

    @Override
    public Invoker getInvoker(Object proxyObject) {
        try {
            Field field = proxyObject.getClass().getField("proxyInvoker");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return (Invoker) field.get(proxyObject);
        } catch (Exception e) {
            return null;
        }
    }
}