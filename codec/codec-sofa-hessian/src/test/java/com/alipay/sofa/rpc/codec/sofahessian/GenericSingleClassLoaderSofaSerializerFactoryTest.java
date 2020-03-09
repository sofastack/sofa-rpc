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
import com.alipay.hessian.generic.io.GenericClassDeserializer;
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
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class GenericSingleClassLoaderSofaSerializerFactoryTest {

    GenericSingleClassLoaderSofaSerializerFactory factory = new GenericSingleClassLoaderSofaSerializerFactory();

    @Test
    public void getSerializer() throws Exception {
        Assert.assertEquals(factory.getSerializer(GenericObject.class).getClass(), GenericObjectSerializer.class);
        Assert.assertEquals(factory.getSerializer(GenericArray.class).getClass(), GenericArraySerializer.class);
        Assert.assertEquals(factory.getSerializer(GenericCollection.class).getClass(),
            GenericCollectionSerializer.class);
        Assert.assertEquals(factory.getSerializer(GenericMap.class).getClass(), GenericMapSerializer.class);
        Assert.assertEquals(factory.getSerializer(GenericClass.class).getClass(), GenericClassSerializer.class);
    }

    @Test
    public void getDeserializer() throws Exception {
        Assert.assertEquals(GenericClassDeserializer.class,
            factory.getDeserializer(Class.class.getCanonicalName()).getClass());
        Assert.assertEquals(GenericDeserializer.class,
            factory.getDeserializer(GenericObject.class.getCanonicalName()).getClass());
    }
}