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
 * Unit tests for ServiceContext
 *
 * @author SOFA-RPC Team
 */
public class ServiceContextTest {

    @Test
    public void testDefaultInitialization() {
        ServiceContext ctx = new ServiceContext();
        Assert.assertNotNull(ctx.getMethods());
        Assert.assertNotNull(ctx.getMethodTypes());
        Assert.assertTrue(ctx.getMethods().isEmpty());
        Assert.assertTrue(ctx.getMethodTypes().isEmpty());
    }

    @Test
    public void testSettersAndGetters() {
        ServiceContext ctx = new ServiceContext();
        ctx.setFileName("TestService.java");
        ctx.setProtoName("test.proto");
        ctx.setPackageName("com.test");
        ctx.setClassName("TestService");
        ctx.setServiceName("TestService");
        ctx.setDeprecated(true);
        ctx.setJavaDoc("Test JavaDoc");

        Assert.assertEquals("TestService.java", ctx.getFileName());
        Assert.assertEquals("test.proto", ctx.getProtoName());
        Assert.assertEquals("com.test", ctx.getPackageName());
        Assert.assertEquals("TestService", ctx.getClassName());
        Assert.assertEquals("TestService", ctx.getServiceName());
        Assert.assertTrue(ctx.isDeprecated());
        Assert.assertEquals("Test JavaDoc", ctx.getJavaDoc());
    }

    @Test
    public void testUnaryRequestMethods() {
        ServiceContext ctx = new ServiceContext();
        MethodContext method1 = new MethodContext();
        method1.setMethodName("method1");
        method1.setManyInput(false);
        ctx.getMethods().add(method1);

        MethodContext method2 = new MethodContext();
        method2.setMethodName("method2");
        method2.setManyInput(true);
        ctx.getMethods().add(method2);

        Assert.assertEquals(1, ctx.unaryRequestMethods().size());
        Assert.assertEquals("method1", ctx.unaryRequestMethods().get(0).getMethodName());
    }

    @Test
    public void testUnaryMethods() {
        ServiceContext ctx = new ServiceContext();
        MethodContext method1 = new MethodContext();
        method1.setMethodName("method1");
        method1.setManyInput(false);
        method1.setManyOutput(false);
        ctx.getMethods().add(method1);

        MethodContext method2 = new MethodContext();
        method2.setMethodName("method2");
        method2.setManyInput(false);
        method2.setManyOutput(true);
        ctx.getMethods().add(method2);

        Assert.assertEquals(1, ctx.unaryMethods().size());
        Assert.assertEquals("method1", ctx.unaryMethods().get(0).getMethodName());
    }

    @Test
    public void testServerStreamingMethods() {
        ServiceContext ctx = new ServiceContext();
        MethodContext method1 = new MethodContext();
        method1.setMethodName("method1");
        method1.setManyInput(false);
        method1.setManyOutput(true);
        ctx.getMethods().add(method1);

        Assert.assertEquals(1, ctx.serverStreamingMethods().size());
        Assert.assertEquals("method1", ctx.serverStreamingMethods().get(0).getMethodName());
    }

    @Test
    public void testBiStreamingMethods() {
        ServiceContext ctx = new ServiceContext();
        MethodContext method1 = new MethodContext();
        method1.setMethodName("method1");
        method1.setManyInput(true);
        ctx.getMethods().add(method1);

        Assert.assertEquals(1, ctx.biStreamingMethods().size());
        Assert.assertEquals("method1", ctx.biStreamingMethods().get(0).getMethodName());
    }
}
