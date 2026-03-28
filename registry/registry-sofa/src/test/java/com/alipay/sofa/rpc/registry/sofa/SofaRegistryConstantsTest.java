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
package com.alipay.sofa.rpc.registry.sofa;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for SofaRegistryConstants
 *
 * @author SOFA-RPC Team
 */
public class SofaRegistryConstantsTest {

    @Test
    public void testSerializeTypeKey() {
        Assert.assertEquals("_SERIALIZETYPE", SofaRegistryConstants.SERIALIZE_TYPE_KEY);
    }

    @Test
    public void testWeightKey() {
        Assert.assertEquals("_WEIGHT", SofaRegistryConstants.WEIGHT_KEY);
    }

    @Test
    public void testWarmupWeightKey() {
        Assert.assertEquals("_WARMUPWEIGHT", SofaRegistryConstants.WARMUP_WEIGHT_KEY);
    }

    @Test
    public void testWarmupTimeKey() {
        Assert.assertEquals("_WARMUPTIME", SofaRegistryConstants.WARMUP_TIME_KEY);
    }

    @Test
    public void testHostMachineKey() {
        Assert.assertEquals("_HOSTMACHINE", SofaRegistryConstants.HOST_MACHINE_KEY);
    }

    @Test
    public void testRetriesKey() {
        Assert.assertEquals("_RETRIES", SofaRegistryConstants.RETRIES_KEY);
    }

    @Test
    public void testConnectTimeout() {
        Assert.assertEquals("_CONNECTTIMEOUT", SofaRegistryConstants.CONNECTI_TIMEOUT);
    }

    @Test
    public void testConnectionNum() {
        Assert.assertEquals("_CONNECTIONNUM", SofaRegistryConstants.CONNECTI_NUM);
    }

    @Test
    public void testTimeout() {
        Assert.assertEquals("_TIMEOUT", SofaRegistryConstants.TIMEOUT);
    }

    @Test
    public void testIdleTimeout() {
        Assert.assertEquals("_IDLETIMEOUT", SofaRegistryConstants.IDLE_TIMEOUT);
    }

    @Test
    public void testMaxReadIdleTime() {
        Assert.assertEquals("_MAXREADIDLETIME", SofaRegistryConstants.MAX_READ_IDLE);
    }

    @Test
    public void testAppName() {
        Assert.assertEquals("app_name", SofaRegistryConstants.APP_NAME);
    }

    @Test
    public void testSelfAppName() {
        Assert.assertEquals("self_app_name", SofaRegistryConstants.SELF_APP_NAME);
    }

    @Test
    public void testRpcServiceVersion() {
        Assert.assertEquals("v", SofaRegistryConstants.RPC_SERVICE_VERSION);
    }

    @Test
    public void testDefaultRpcServiceVersion() {
        Assert.assertEquals("", SofaRegistryConstants.DEFAULT_RPC_SERVICE_VERSION);
    }

    @Test
    public void testSofa4RpcServiceVersion() {
        Assert.assertEquals("4.0", SofaRegistryConstants.SOFA4_RPC_SERVICE_VERSION);
    }

    @Test
    public void testRpcRemotingProtocol() {
        Assert.assertEquals("p", SofaRegistryConstants.RPC_REMOTING_PROTOCOL);
    }

    @Test
    public void testKeyTimeout() {
        Assert.assertEquals("clientTimeout", SofaRegistryConstants.KEY_TIMEOUT);
    }

    @Test
    public void testKeyRetries() {
        Assert.assertEquals("retries", SofaRegistryConstants.KEY_RETRIES);
    }

    @Test
    public void testSofaGroupKey() {
        Assert.assertEquals("sofa.group", SofaRegistryConstants.SOFA_GROUP_KEY);
    }
}
