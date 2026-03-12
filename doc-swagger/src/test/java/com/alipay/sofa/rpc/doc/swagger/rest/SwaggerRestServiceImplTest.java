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
package com.alipay.sofa.rpc.doc.swagger.rest;

import com.alipay.sofa.rpc.doc.swagger.generate.GenerateService;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for SwaggerRestServiceImpl
 *
 * @author SOFA-RPC Team
 */
public class SwaggerRestServiceImplTest {

    @Test
    public void testConstructorWithGenerateService() {
        GenerateService generateService = new GenerateService();
        SwaggerRestServiceImpl service = new SwaggerRestServiceImpl(generateService);
        Assert.assertNotNull(service);
    }

    @Test
    public void testDefaultConstructor() {
        SwaggerRestServiceImpl service = new SwaggerRestServiceImpl();
        Assert.assertNotNull(service);
    }

    @Test
    public void testApi() {
        SwaggerRestServiceImpl service = new SwaggerRestServiceImpl();
        // Test with null protocol - may return empty or default value
        String result = service.api(null);
        // The result depends on GenerateService implementation
        // Just verify the method can be called without exception
        Assert.assertNotNull(result);
    }

    @Test
    public void testApiWithProtocol() {
        SwaggerRestServiceImpl service = new SwaggerRestServiceImpl();
        // Test with specific protocol
        String result = service.api("rest");
        Assert.assertNotNull(result);
    }
}
