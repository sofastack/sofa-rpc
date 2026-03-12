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
package com.alipay.sofa.gen.domain;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for MethodContext
 *
 * @author SOFA-RPC Team
 */
public class MethodContextTest {

    @Test
    public void testDefaultInitialization() {
        MethodContext ctx = new MethodContext();
        Assert.assertFalse(ctx.isManyInput());
        Assert.assertFalse(ctx.isManyOutput());
        Assert.assertFalse(ctx.isDeprecated());
        Assert.assertEquals(0, ctx.getMethodNumber());
    }

    @Test
    public void testSettersAndGetters() {
        MethodContext ctx = new MethodContext();
        ctx.setMethodName("TestMethod");
        ctx.setInputType("TestInput");
        ctx.setOutputType("TestOutput");
        ctx.setDeprecated(true);
        ctx.setManyInput(true);
        ctx.setManyOutput(true);
        ctx.setReactiveCallsMethodName("manyToMany");
        ctx.setGrpcCallsMethodName("asyncBidiStreamingCall");
        ctx.setMethodNumber(1);
        ctx.setJavaDoc("Test JavaDoc");

        Assert.assertEquals("TestMethod", ctx.getMethodName());
        Assert.assertEquals("TestInput", ctx.getInputType());
        Assert.assertEquals("TestOutput", ctx.getOutputType());
        Assert.assertTrue(ctx.isDeprecated());
        Assert.assertTrue(ctx.isManyInput());
        Assert.assertTrue(ctx.isManyOutput());
        Assert.assertEquals("manyToMany", ctx.getReactiveCallsMethodName());
        Assert.assertEquals("asyncBidiStreamingCall", ctx.getGrpcCallsMethodName());
        Assert.assertEquals(1, ctx.getMethodNumber());
        Assert.assertEquals("Test JavaDoc", ctx.getJavaDoc());
    }

    @Test
    public void testMethodNameUpperUnderscore() {
        MethodContext ctx = new MethodContext();
        ctx.setMethodName("testMethod");
        Assert.assertEquals("TEST_METHOD", ctx.methodNameUpperUnderscore());
    }

    @Test
    public void testMethodNameUpperUnderscoreWithCamelCase() {
        MethodContext ctx = new MethodContext();
        ctx.setMethodName("testMethodName");
        Assert.assertEquals("TEST_METHOD_NAME", ctx.methodNameUpperUnderscore());
    }

    @Test
    public void testMethodNamePascalCase() {
        MethodContext ctx = new MethodContext();
        ctx.setMethodName("test_method");
        Assert.assertEquals("Testmethod", ctx.methodNamePascalCase());
    }

    @Test
    public void testMethodNamePascalCaseNoUnderscore() {
        MethodContext ctx = new MethodContext();
        ctx.setMethodName("testMethod");
        Assert.assertEquals("TestMethod", ctx.methodNamePascalCase());
    }

    @Test
    public void testMethodNameCamelCase() {
        MethodContext ctx = new MethodContext();
        ctx.setMethodName("TestMethod");
        Assert.assertEquals("testMethod", ctx.methodNameCamelCase());
    }

    @Test
    public void testToString() {
        MethodContext ctx = new MethodContext();
        ctx.setMethodName("test");
        String str = ctx.toString();
        Assert.assertNotNull(str);
        Assert.assertTrue(str.contains("methodName='test'"));
    }
}
