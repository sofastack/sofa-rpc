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
package com.alipay.sofa.rpc.doc.swagger.generate;

import io.swagger.models.Info;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for GenerateService
 *
 * @author SOFA-RPC Team
 */
public class GenerateServiceTest {

    @Test
    public void testDefaultConstructor() {
        GenerateService service = new GenerateService();
        Assert.assertNotNull(service);
    }

    @Test
    public void testConstructorWithParameters() {
        GenerateService service = new GenerateService("rest", "/api/");
        Assert.assertNotNull(service);
    }

    @Test
    public void testGenerate() {
        GenerateService service = new GenerateService();
        // Generate with default protocol
        String result = service.generate();
        // Result should be a JSON string
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("swagger"));
    }

    @Test
    public void testGenerateWithProtocol() {
        GenerateService service = new GenerateService();
        // Generate with specific protocol
        String result = service.generate("bolt");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("swagger"));
    }

    @Test
    public void testGenerateWithNullProtocol() {
        GenerateService service = new GenerateService();
        // Generate with null protocol - should use default
        String result = service.generate(null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("swagger"));
    }

    @Test
    public void testGetInfo() {
        GenerateService service = new GenerateService();
        Info info = service.getInfo();
        Assert.assertNotNull(info);
        Assert.assertEquals("Swagger API", info.getTitle());
        Assert.assertEquals("", info.getVersion());
    }
}
