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
package com.alipay.sofa.rpc.doc.swagger.resource;

import io.swagger.converter.ModelConverterContextImpl;
import io.swagger.jackson.ModelResolver;
import io.swagger.models.Model;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.Json;
import io.swagger.util.ParameterProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TestGenerate {

    public static final class SomeParameter {
        private String[]     strings;
        private List<String> stringList;

        public SomeParameter[] getObjects() {
            return objects;
        }

        public void setObjects(SomeParameter[] objects) {
            this.objects = objects;
        }

        private SomeParameter[] objects;

        public List<String> getStringList() {
            return stringList;
        }

        public void setStringList(List<String> stringList) {
            this.stringList = stringList;
        }

        public String[] getStrings() {
            return strings;
        }

        public void setStrings(String[] strings) {
            this.strings = strings;
        }
    }

    public void someMethod(SomeParameter someParameter) {
        System.out.printf("someMethod");
    }

    @Test
    public void testGenerateParameter() throws NoSuchMethodException {
        Method someMethod = TestGenerate.class.getMethod("someMethod", SomeParameter.class);
        Type type = someMethod.getGenericParameterTypes()[0];
        Swagger swagger = new Swagger();
        BodyParameter parameter = new BodyParameter();
        Parameter parameter1 = ParameterProcessor.applyAnnotations(swagger, parameter, type,
            new ArrayList<Annotation>());
        Assert.assertTrue(parameter1 instanceof BodyParameter);
        Model schema = ((BodyParameter) parameter1).getSchema();
        Assert.assertTrue(schema instanceof RefModel);
        Assert.assertEquals("#/definitions/SomeParameter", ((RefModel) schema).get$ref());
    }

    @Test
    public void testGenerateModel() throws NoSuchMethodException {
        doTestGenerateModel();
    }

    private void doTestGenerateModel() throws NoSuchMethodException {
        Method someMethod = TestGenerate.class.getMethod("someMethod", SomeParameter.class);
        Type type = someMethod.getGenericParameterTypes()[0];

        ModelResolver converter = new ModelResolver(Json.mapper());
        ModelConverterContextImpl context = new ModelConverterContextImpl(converter);
        Property property = context.resolveProperty(type, null);
        Assert.assertTrue(property instanceof RefProperty);
        Assert.assertEquals("#/definitions/SomeParameter", ((RefProperty) property).get$ref());
    }
}
