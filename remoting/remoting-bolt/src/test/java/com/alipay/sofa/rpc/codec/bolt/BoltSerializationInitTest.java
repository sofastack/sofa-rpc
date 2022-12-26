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
package com.alipay.sofa.rpc.codec.bolt;

import com.alipay.remoting.CustomSerializerManager;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.test.TestSofaRpcSerializationRegister;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author junyuan
 * @version BoltSerializationInitTest.java, v 0.1 2022年12月23日 16:26 junyuan Exp $
 */
public class BoltSerializationInitTest {

    @Test
    public void SerializerRegisterOverrideTest() {
        BoltSerializationRegister boltSerializationRegister = ExtensionLoaderFactory.getExtensionLoader(
            BoltSerializationRegister.class).getExtension("sofaRpcSerializationRegister");
        boltSerializationRegister.doRegisterCustomSerializer();

        Assert.assertNull("testRegister未能覆盖原版register",
            CustomSerializerManager.getCustomSerializer(SofaResponse.class.getName()));

        Assert.assertNotNull("testRegister未能覆盖原版register", CustomSerializerManager.getCustomSerializer(
            TestSofaRpcSerializationRegister.class.getName()));
    }

    @After
    public void clearClassSerializerMap() {
        CustomSerializerManager.clear();
    }

}