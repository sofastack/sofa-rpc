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
package com.alipay.sofa.rpc.codec.sofahessian;

import com.alipay.hessian.generic.io.GenericArraySerializer;
import com.alipay.hessian.generic.io.GenericClassSerializer;
import com.alipay.hessian.generic.io.GenericCollectionSerializer;
import com.alipay.hessian.generic.io.GenericDeserializer;
import com.alipay.hessian.generic.io.GenericMapSerializer;
import com.alipay.hessian.generic.io.GenericObjectSerializer;
import com.alipay.hessian.generic.model.GenericArray;
import com.alipay.hessian.generic.model.GenericClass;
import com.alipay.hessian.generic.model.GenericCollection;
import com.alipay.hessian.generic.model.GenericMap;
import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.rpc.codec.sofahessian.mock.MockError;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.JavaDeserializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static com.alipay.sofa.rpc.codec.sofahessian.serialize.GenericCustomThrowableDeterminerTest.setGenericThrowException;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class GenericMultipleClassLoaderSofaSerializerFactoryTest {
    @Test
    public void getSerializer() throws Exception {
        GenericMultipleClassLoaderSofaSerializerFactory factory = new GenericMultipleClassLoaderSofaSerializerFactory();
        Assert.assertEquals(factory.getSerializer(GenericObject.class).getClass(), GenericObjectSerializer.class);
        Assert.assertEquals(factory.getSerializer(GenericArray.class).getClass(), GenericArraySerializer.class);
        Assert.assertEquals(factory.getSerializer(GenericCollection.class).getClass(),
            GenericCollectionSerializer.class);
        Assert.assertEquals(factory.getSerializer(GenericMap.class).getClass(), GenericMapSerializer.class);
        Assert.assertEquals(factory.getSerializer(GenericClass.class).getClass(), GenericClassSerializer.class);
    }

    @Test
    public void getDeserializer() throws Exception {

    }

    @Test
    public void testCustomThrowableDeserializer() throws Exception {
        GenericMultipleClassLoaderSofaSerializerFactory factory = new GenericMultipleClassLoaderSofaSerializerFactory();

        ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(bsOut);
        hessian2Output.setSerializerFactory(factory);

        MockError mockError = new MockError("MockError");
        hessian2Output.writeObject(mockError);
        hessian2Output.flush();

        Deserializer genericDeserializer = factory.getDeserializer(MockError.class.getName());
        Assert.assertTrue(genericDeserializer instanceof GenericDeserializer);

        ByteArrayInputStream bsIn = new ByteArrayInputStream(bsOut.toByteArray());
        Hessian2Input hessian2Input = new Hessian2Input(bsIn);
        hessian2Input.setSerializerFactory(factory);
        Object result = hessian2Input.readObject();
        Assert.assertTrue(result instanceof GenericObject);
    }

    @Test
    public void testCustomThrowableDeserializerEnabled() throws Exception {
        setGenericThrowException(true);
        try {
            GenericMultipleClassLoaderSofaSerializerFactory factory = new GenericMultipleClassLoaderSofaSerializerFactory();

            ByteArrayOutputStream bsOut = new ByteArrayOutputStream();
            Hessian2Output hessian2Output = new Hessian2Output(bsOut);
            hessian2Output.setSerializerFactory(factory);

            MockError mockError = new MockError("MockError");
            hessian2Output.writeObject(mockError);
            hessian2Output.flush();

            Deserializer javaDeserializer = factory.getDeserializer(MockError.class.getName());
            Assert.assertTrue(javaDeserializer instanceof JavaDeserializer);

            ByteArrayInputStream bsIn = new ByteArrayInputStream(bsOut.toByteArray());
            Hessian2Input hessian2Input = new Hessian2Input(bsIn);
            hessian2Input.setSerializerFactory(factory);
            Object result = hessian2Input.readObject();
            Assert.assertTrue(result instanceof MockError);
            Assert.assertEquals("MockError", ((MockError) result).getMessage());
        } finally {
            setGenericThrowException(false);
        }
    }
}