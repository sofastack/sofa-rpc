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
package com.alipay.sofa.rpc.utils;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ServerServiceDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * @author zhaowang
 * @version : SofaProtoUtilsTest.java, v 0.1 2020年06月04日 11:39 上午 zhaowang Exp $
 */
public class SofaProtoUtilsTest {

    @Test
    public void testIsProtoClass() {
        Assert.assertTrue(SofaProtoUtils.isProtoClass(new BindableServiceImpl()));
        Assert.assertFalse(SofaProtoUtils.isProtoClass(""));
    }

    static class BindableServiceImpl implements BindableService {
        @Override
        public ServerServiceDefinition bindService() {
            return null;
        }
    }

    interface TestInterface {
        void methodA();

        void methodB();
    }

    @Test
    public void testGetMethodNames() {
        String interfaceName = TestInterface.class.getName();
        Set<String> methodNames = SofaProtoUtils.getMethodNames(interfaceName);
        Assert.assertEquals(2, methodNames.size());
        Assert.assertTrue(methodNames.contains("methodA"));
        Assert.assertTrue(methodNames.contains("methodB"));
    }

    @Test
    public void testCheckIfUseGeneric() {
        ConsumerConfig asTrue = new ConsumerConfig();
        asTrue.setInterfaceId(NeedGeneric.NeedGenericInterface.class.getName());
        ConsumerConfig asFalse = new ConsumerConfig();
        asFalse.setInterfaceId(DoNotNeedGeneric.NoNotNeedGenericInterface.class.getName());

        Assert.assertTrue(SofaProtoUtils.checkIfUseGeneric(asTrue));
        Assert.assertFalse(SofaProtoUtils.checkIfUseGeneric(asFalse));

    }

    static class NeedGeneric {
        public void getSofaStub1(Channel channel, CallOptions callOptions, int integer) {

        }

        interface NeedGenericInterface {

        }
    }

    static class DoNotNeedGeneric {
        public void getSofaStub(Channel channel, CallOptions callOptions, int integer) {

        }

        interface NoNotNeedGenericInterface {

        }
    }

}