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
package com.alipay.sofa.rpc.test;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for HelloService and HelloServiceImpl
 *
 * @author SOFA-RPC Team
 */
public class HelloServiceTest {

    @Test
    public void testHelloServiceInterface() {
        Assert.assertNotNull(HelloService.class);
    }

    @Test
    public void testHelloServiceImpl() {
        HelloService service = new HelloServiceImpl();
        Assert.assertNotNull(service);
    }

    @Test
    public void testSayHello() {
        HelloService service = new HelloServiceImpl();
        String result = service.sayHello("World", 25);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("World"));
    }

    @Test
    public void testSayHelloWithCustomResult() {
        HelloService service = new HelloServiceImpl("custom result");
        String result = service.sayHello("World", 25);
        Assert.assertEquals("custom result", result);
    }
}
