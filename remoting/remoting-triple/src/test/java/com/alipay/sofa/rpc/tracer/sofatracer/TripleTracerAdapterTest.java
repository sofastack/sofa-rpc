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
package com.alipay.sofa.rpc.tracer.sofatracer;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Even
 * @date 2022/11/26 3:15 PM
 */
public class TripleTracerAdapterTest {

    @Test
    public void testBuilderLogServiceName() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {
        Method buildLogServiceName = TripleTracerAdapter.class.getDeclaredMethod("buildLogServiceName", String.class,
            String.class);
        buildLogServiceName.setAccessible(true);

        String logServiceNameWithUniqueId = (String) buildLogServiceName.invoke(null, "testInterfaceId", "uniqueId");
        Assert.assertEquals("testInterfaceId:1.0:uniqueId", logServiceNameWithUniqueId);

        String logServiceNameWithOutUniqueId = (String) buildLogServiceName.invoke(null, "testInterfaceId", "");
        Assert.assertEquals("testInterfaceId:1.0", logServiceNameWithOutUniqueId);

    }

}