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
package com.alipay.sofa.rpc.test;

import com.alipay.remoting.CustomSerializerManager;
import com.alipay.sofa.rpc.codec.bolt.AbstractSerializationRegister;
import com.alipay.sofa.rpc.codec.bolt.SofaRpcSerialization;
import com.alipay.sofa.rpc.codec.bolt.SofaRpcSerializationRegister;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Register custom serializer to bolt.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "sofaRpcSerializationRegister", override = true, order = 20)
public class TestSofaRpcSerializationRegister extends SofaRpcSerializationRegister {

    private final SofaRpcSerialization rpcSerialization = new SofaRpcSerialization();

    /**
     * we can override or rewrite the method
     */
    @Override
    protected void innerRegisterCustomSerializer() {
        // 注册序列化器到bolt
        if (CustomSerializerManager.getCustomSerializer(SofaRequest.class.getName()) == null) {
            CustomSerializerManager.registerCustomSerializer(SofaRequest.class.getName(),
                rpcSerialization);
        }

        if (CustomSerializerManager.getCustomSerializer(TestSofaRpcSerializationRegister.class.getName()) == null) {
            CustomSerializerManager.registerCustomSerializer(TestSofaRpcSerializationRegister.class.getName(),
                rpcSerialization);
        }
    }
}
