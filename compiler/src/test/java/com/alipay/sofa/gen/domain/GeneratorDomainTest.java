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
 * Unit tests for ServiceContext and MethodContext classes
 *
 * @author SOFA-RPC Team
 */
public class GeneratorDomainTest {

    @Test
    public void testServiceContextCreation() {
        ServiceContext context = new ServiceContext();

        Assert.assertNotNull(context);
        Assert.assertNotNull(context.getMethods());
        Assert.assertNotNull(context.getMethodTypes());
    }

    @Test
    public void testServiceContextSettersAndGetters() {
        ServiceContext context = new ServiceContext();

        context.setFileName("TestService.java");
        Assert.assertEquals("TestService.java", context.getFileName());

        context.setProtoName("test.proto");
        Assert.assertEquals("test.proto", context.getProtoName());

        context.setPackageName("com.alipay.sofa.rpc.test");
        Assert.assertEquals("com.alipay.sofa.rpc.test", context.getPackageName());

        context.setClassName("SofaTripleTest");
        Assert.assertEquals("SofaTripleTest", context.getClassName());

        context.setServiceName("TestService");
        Assert.assertEquals("TestService", context.getServiceName());

        context.setDeprecated(true);
        Assert.assertTrue(context.isDeprecated());

        context.setJavaDoc("Test Java Doc");
        Assert.assertEquals("Test Java Doc", context.getJavaDoc());
    }

    @Test
    public void testServiceContextAddMethod() {
        ServiceContext context = new ServiceContext();
        MethodContext method = new MethodContext();
        method.setMethodName("testMethod");

        context.getMethods().add(method);
        Assert.assertEquals(1, context.getMethods().size());
        Assert.assertEquals("testMethod", context.getMethods().get(0).getMethodName());
    }

    @Test
    public void testServiceContextAddMethodType() {
        ServiceContext context = new ServiceContext();

        context.getMethodTypes().add("RequestType1");
        context.getMethodTypes().add("RequestType2");

        Assert.assertEquals(2, context.getMethodTypes().size());
        Assert.assertTrue(context.getMethodTypes().contains("RequestType1"));
        Assert.assertTrue(context.getMethodTypes().contains("RequestType2"));
    }

    @Test
    public void testMethodContextCreation() {
        MethodContext method = new MethodContext();

        Assert.assertNotNull(method);
        Assert.assertFalse(method.isManyInput());
        Assert.assertFalse(method.isManyOutput());
    }

    @Test
    public void testMethodContextSettersAndGetters() {
        MethodContext method = new MethodContext();

        method.setMethodName("testMethod");
        Assert.assertEquals("testMethod", method.getMethodName());

        method.setInputType("com.alipay.TestRequest");
        Assert.assertEquals("com.alipay.TestRequest", method.getInputType());

        method.setOutputType("com.alipay.TestResponse");
        Assert.assertEquals("com.alipay.TestResponse", method.getOutputType());

        method.setDeprecated(true);
        Assert.assertTrue(method.isDeprecated());

        method.setManyInput(true);
        Assert.assertTrue(method.isManyInput());

        method.setManyOutput(true);
        Assert.assertTrue(method.isManyOutput());

        method.setMethodNumber(1);
        Assert.assertEquals(1, method.getMethodNumber());

        method.setJavaDoc("Method Java Doc");
        Assert.assertEquals("Method Java Doc", method.getJavaDoc());
    }

    @Test
    public void testMethodContextStreamingTypes() {
        MethodContext method = new MethodContext();

        // Test unary (one-to-one)
        method.setManyInput(false);
        method.setManyOutput(false);
        Assert.assertFalse(method.isManyInput());
        Assert.assertFalse(method.isManyOutput());

        // Test server streaming (one-to-many)
        method.setManyInput(false);
        method.setManyOutput(true);
        Assert.assertFalse(method.isManyInput());
        Assert.assertTrue(method.isManyOutput());

        // Test client streaming (many-to-one)
        method.setManyInput(true);
        method.setManyOutput(false);
        Assert.assertTrue(method.isManyInput());
        Assert.assertFalse(method.isManyOutput());

        // Test bidirectional streaming (many-to-many)
        method.setManyInput(true);
        method.setManyOutput(true);
        Assert.assertTrue(method.isManyInput());
        Assert.assertTrue(method.isManyOutput());
    }

    @Test
    public void testServiceContextUnaryRequestMethods() {
        ServiceContext context = new ServiceContext();

        MethodContext unaryMethod = new MethodContext();
        unaryMethod.setMethodName("unaryMethod");
        unaryMethod.setManyInput(false);
        unaryMethod.setManyOutput(false);

        MethodContext serverStreamingMethod = new MethodContext();
        serverStreamingMethod.setMethodName("serverStreaming");
        serverStreamingMethod.setManyInput(false);
        serverStreamingMethod.setManyOutput(true);

        MethodContext clientStreamingMethod = new MethodContext();
        clientStreamingMethod.setMethodName("clientStreaming");
        clientStreamingMethod.setManyInput(true);
        clientStreamingMethod.setManyOutput(false);

        context.getMethods().add(unaryMethod);
        context.getMethods().add(serverStreamingMethod);
        context.getMethods().add(clientStreamingMethod);

        Assert.assertEquals(2, context.unaryRequestMethods().size());
    }

    @Test
    public void testServiceContextUnaryMethods() {
        ServiceContext context = new ServiceContext();

        MethodContext unaryMethod = new MethodContext();
        unaryMethod.setMethodName("unaryMethod");
        unaryMethod.setManyInput(false);
        unaryMethod.setManyOutput(false);

        MethodContext serverStreamingMethod = new MethodContext();
        serverStreamingMethod.setMethodName("serverStreaming");
        serverStreamingMethod.setManyInput(false);
        serverStreamingMethod.setManyOutput(true);

        MethodContext biStreamingMethod = new MethodContext();
        biStreamingMethod.setMethodName("biStreaming");
        biStreamingMethod.setManyInput(true);
        biStreamingMethod.setManyOutput(true);

        context.getMethods().add(unaryMethod);
        context.getMethods().add(serverStreamingMethod);
        context.getMethods().add(biStreamingMethod);

        Assert.assertEquals(1, context.unaryMethods().size());
        Assert.assertEquals("unaryMethod", context.unaryMethods().get(0).getMethodName());
    }

    @Test
    public void testServiceContextServerStreamingMethods() {
        ServiceContext context = new ServiceContext();

        MethodContext unaryMethod = new MethodContext();
        unaryMethod.setMethodName("unaryMethod");
        unaryMethod.setManyInput(false);
        unaryMethod.setManyOutput(false);

        MethodContext serverStreamingMethod = new MethodContext();
        serverStreamingMethod.setMethodName("serverStreaming");
        serverStreamingMethod.setManyInput(false);
        serverStreamingMethod.setManyOutput(true);

        context.getMethods().add(unaryMethod);
        context.getMethods().add(serverStreamingMethod);

        Assert.assertEquals(1, context.serverStreamingMethods().size());
        Assert.assertEquals("serverStreaming", context.serverStreamingMethods().get(0).getMethodName());
    }

    @Test
    public void testServiceContextBiStreamingMethods() {
        ServiceContext context = new ServiceContext();

        MethodContext unaryMethod = new MethodContext();
        unaryMethod.setMethodName("unaryMethod");
        unaryMethod.setManyInput(false);
        unaryMethod.setManyOutput(false);

        MethodContext biStreamingMethod = new MethodContext();
        biStreamingMethod.setMethodName("biStreaming");
        biStreamingMethod.setManyInput(true);
        biStreamingMethod.setManyOutput(true);

        context.getMethods().add(unaryMethod);
        context.getMethods().add(biStreamingMethod);

        Assert.assertEquals(1, context.biStreamingMethods().size());
        Assert.assertEquals("biStreaming", context.biStreamingMethods().get(0).getMethodName());
    }
}
