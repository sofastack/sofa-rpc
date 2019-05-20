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
package com.alipay.sofa.rpc.proxy.javassist;

import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.proxy.AbstractTestClass;
import com.alipay.sofa.rpc.proxy.TestInterface;
import com.alipay.sofa.rpc.proxy.TestInvoker;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class JavassistProxyTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(JavassistProxyTest.class);

    @Test
    public void getProxy() throws Exception {
        JavassistProxy proxy = new JavassistProxy();
        AbstractTestClass testClass = null;
        try {
            testClass = proxy.getProxy(AbstractTestClass.class, new TestInvoker());
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        Assert.assertNull(testClass);

        TestInvoker invoker = new TestInvoker();
        TestInterface testInterface = proxy.getProxy(TestInterface.class, invoker);
        Assert.assertNotNull(testInterface);

        Class clazz = testInterface.getClass().getInterfaces()[0];
        Assert.assertEquals(TestInterface.class, clazz);

        Assert.assertTrue(Proxy.class.isAssignableFrom(testInterface.getClass()));
        Assert.assertFalse(Proxy.isProxyClass(testInterface.getClass()));

        Assert.assertEquals(proxy.getInvoker(testInterface).getClass(), TestInvoker.class);
        Assert.assertEquals(testInterface.toString(), invoker.toString());
        Assert.assertEquals(testInterface.hashCode(), invoker.hashCode());

        TestInterface another1 = proxy.getProxy(TestInterface.class, invoker);
        TestInterface another2 = proxy.getProxy(TestInterface.class, new TestInvoker());
        Assert.assertFalse(testInterface.equals(invoker));
        Assert.assertFalse(testInterface.equals(another2));
        Assert.assertEquals(testInterface, another1);

        Assert.assertEquals(678, another1.sayNum(true));
        SofaRequest request = invoker.getRequest();
        Assert.assertEquals(TestInterface.class.getCanonicalName(), request.getInterfaceName());
        Assert.assertEquals("sayNum", request.getMethodName());
        Assert.assertEquals("boolean", request.getMethodArgSigs()[0]);
        Assert.assertEquals(true, request.getMethodArgs()[0]);
        Assert.assertNotNull(request.getMethod());

        Assert.assertEquals("sayHello", another1.sayHello("xxxx"));
        another1.sayNoting();
        Assert.assertArrayEquals(new int[] { 6, 7, 8 }, another1.sayNums(null, new HashMap()));
        Assert.assertNull(another1.sayNum2(1.2D));

        boolean error = false;
        try {
            another1.throwbiz1();
        } catch (Throwable e) {
            error = true;
        }
        Assert.assertFalse(error);

        error = false;
        try {
            another1.throwbiz2();
        } catch (Throwable e) {
            error = true;
        }
        Assert.assertFalse(error);

        try {
            another1.throwRPC();
        } catch (Throwable e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

}