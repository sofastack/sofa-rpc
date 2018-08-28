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
package com.alipay.sofa.rpc.codec;

import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SerializerFactoryTest {
    @Test
    public void getSerializer() {
        Serializer serializer = SerializerFactory.getSerializer((byte) 117);
        Assert.assertNotNull(serializer);
        Assert.assertEquals(TestSerializer.class, serializer.getClass());
    }

    @Test
    public void getSerializer1() {
        Serializer serializer = SerializerFactory.getSerializer("test");
        Assert.assertNotNull(serializer);
        Assert.assertEquals(TestSerializer.class, serializer.getClass());
    }

    @Test
    public void getCodeByAlias() {
        Assert.assertTrue(SerializerFactory.getCodeByAlias("test") == 117);
    }

    @Test
    public void getAliasByCode() {
        Assert.assertEquals("test", SerializerFactory.getAliasByCode((byte) 117));
    }

    @Test
    public void getSerializerNotExist() {
        try {
            SerializerFactory.getSerializer((byte) 999);
            Assert.fail();
        } catch (SofaRpcRuntimeException e) {
        }
    }

}
