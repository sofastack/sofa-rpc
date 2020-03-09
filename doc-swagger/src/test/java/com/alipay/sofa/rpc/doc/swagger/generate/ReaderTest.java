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

import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.doc.swagger.resource.TestSwaggerService;
import com.alipay.sofa.rpc.doc.swagger.resource.TestSwaggerServiceImpl;
import io.swagger.models.Model;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class ReaderTest {

    public static final String COMPLEX = "/com.alipay.sofa.rpc.doc.swagger.resource.TestSwaggerService/complex";

    @Test
    public void testRead() {
        Map<Class<?>, Object> interfaceMapRef = new HashMap<>();
        interfaceMapRef.put(TestSwaggerService.class, new TestSwaggerServiceImpl());
        Swagger swagger = new Swagger();
        swagger.setBasePath("/rest/");
        Reader.read(swagger, interfaceMapRef, "");

        Assert.assertEquals("2.0", swagger.getSwagger());
        Assert.assertEquals("/rest/", swagger.getBasePath());
        Map<String, Path> paths = swagger.getPaths();
        Assert.assertEquals(TestSwaggerService.class.getMethods().length, paths.size());
        Assert.assertTrue(paths.containsKey(COMPLEX));
        List<Parameter> parameters = paths.get(COMPLEX).getPost().getParameters();
        Assert.assertTrue(CommonUtils.isNotEmpty(parameters));
        Parameter parameter = parameters.get(0);
        Assert.assertTrue(parameter instanceof BodyParameter);
        Model schema = ((BodyParameter) parameter).getSchema();
        Assert.assertTrue(schema instanceof RefModel);
        String ref = ((RefModel) schema).get$ref();
        Assert.assertEquals("#/definitions/ComplexPojo", ref);

    }
}