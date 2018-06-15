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
package com.alipay.sofa.rpc.common;

import com.alipay.sofa.rpc.client.ProviderStatus;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RpcConfigsTest {

    @Test
    public void testAll() {
        Assert.assertNotNull(RpcConfigs.getStringValue("asdasd", RpcOptions.DEFAULT_PROTOCOL));
        try {
            Assert.assertNotNull(RpcConfigs.getStringValue("asdasd", "asdasdzz"));
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }

        Assert.assertTrue(RpcConfigs.getBooleanValue("asdasd", RpcOptions.SERVICE_REGISTER));
        try {
            Assert.assertTrue(RpcConfigs.getBooleanValue("asdasd", "asdasdzz"));
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }

        Assert.assertTrue(RpcConfigs.getIntValue("asdasd", RpcOptions.CONSUMER_INVOKE_TIMEOUT) > 0);
        try {
            Assert.assertTrue(RpcConfigs.getIntValue("asdasd", "asdasdzz") > 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }

        RpcConfigs.putValue("ps001", ProviderStatus.RECOVERING.toString());
        Assert.assertEquals(ProviderStatus.RECOVERING, RpcConfigs.getEnumValue("ps001", ProviderStatus.class));
        try {
            Assert.assertEquals(ProviderStatus.RECOVERING, RpcConfigs.getEnumValue("ps002", ProviderStatus.class));
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }

        String protocol = RpcConfigs.getStringValue(RpcOptions.DEFAULT_PROTOCOL);
        try {
            final Object[] values = new Object[2];
            RpcConfigs.RpcConfigListener listener = new RpcConfigs.RpcConfigListener() {
                @Override
                public void onChange(Object oldValue, Object newValue) {
                    values[0] = oldValue;
                    values[1] = newValue;
                }
            };
            RpcConfigs.subscribe(RpcOptions.DEFAULT_PROTOCOL, listener);

            RpcConfigs.putValue(RpcOptions.DEFAULT_PROTOCOL, "xxx");
            Assert.assertEquals(protocol, values[0]);
            Assert.assertEquals("xxx", values[1]);
            RpcConfigs.unSubscribe(RpcOptions.DEFAULT_PROTOCOL, listener);
        } finally {
            RpcConfigs.putValue(RpcOptions.DEFAULT_PROTOCOL, protocol);
        }
    }
}