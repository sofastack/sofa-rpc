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

import com.alipay.sofa.rpc.proxy.AbstractTestClass;
import com.alipay.sofa.rpc.proxy.TestInterface;
import com.alipay.sofa.rpc.proxy.TestInvoker;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class JavassistProxyTest {
    @Test
    public void getProxy() throws Exception {
        JavassistProxy proxy = new JavassistProxy();
        AbstractTestClass testClass = null;
        try {
            testClass = proxy.getProxy(AbstractTestClass.class, new TestInvoker());
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
    }

}