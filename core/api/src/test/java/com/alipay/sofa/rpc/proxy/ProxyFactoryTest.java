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
package com.alipay.sofa.rpc.proxy;

import com.alipay.sofa.rpc.invoke.Invoker;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProxyFactoryTest {
    @Test
    public void buildProxy() throws Exception {
        TestProxyInterface testInterface = ProxyFactory.buildProxy("test", TestProxyInterface.class, null);
        Assert.assertEquals(testInterface, null);

        boolean error = false;
        try {
            ProxyFactory.buildProxy("xasdasd", TestProxyInterface.class, null);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void getInvoker() {
        Invoker invoke = ProxyFactory.getInvoker(null, "test");
        Assert.assertEquals(invoke, null);
    }

}