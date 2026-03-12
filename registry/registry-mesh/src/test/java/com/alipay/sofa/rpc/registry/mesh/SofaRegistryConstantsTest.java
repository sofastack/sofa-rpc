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
package com.alipay.sofa.rpc.registry.mesh;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for SofaRegistryConstants
 *
 * @author SOFA-RPC Team
 */
public class SofaRegistryConstantsTest {

    @Test
    public void testConstantValues() {
        Assert.assertEquals("_SERIALIZETYPE", SofaRegistryConstants.SERIALIZE_TYPE_KEY);
        Assert.assertEquals("_WEIGHT", SofaRegistryConstants.WEIGHT_KEY);
        Assert.assertEquals("_WARMUPWEIGHT", SofaRegistryConstants.WARMUP_WEIGHT_KEY);
        Assert.assertEquals("_WARMUPTIME", SofaRegistryConstants.WARMUP_TIME_KEY);
        Assert.assertEquals("_HOSTMACHINE", SofaRegistryConstants.HOST_MACHINE_KEY);
        Assert.assertEquals("_RETRIES", SofaRegistryConstants.RETRIES_KEY);
        Assert.assertEquals("_CONNECTTIMEOUT", SofaRegistryConstants.CONNECTI_TIMEOUT);
        Assert.assertEquals("_CONNECTIONNUM", SofaRegistryConstants.CONNECTI_NUM);
        Assert.assertEquals("_TIMEOUT", SofaRegistryConstants.TIMEOUT);
        Assert.assertEquals("_IDLETIMEOUT", SofaRegistryConstants.IDLE_TIMEOUT);
        Assert.assertEquals("_MAXREADIDLETIME", SofaRegistryConstants.MAX_READ_IDLE);
        Assert.assertEquals("app_name", SofaRegistryConstants.APP_NAME);
        Assert.assertEquals("self_app_name", SofaRegistryConstants.SELF_APP_NAME);
        Assert.assertEquals("v", SofaRegistryConstants.RPC_SERVICE_VERSION);
        Assert.assertEquals("", SofaRegistryConstants.DEFAULT_RPC_SERVICE_VERSION);
        Assert.assertEquals("4.0", SofaRegistryConstants.SOFA4_RPC_SERVICE_VERSION);
        Assert.assertEquals("p", SofaRegistryConstants.RPC_REMOTING_PROTOCOL);
        Assert.assertEquals("clientTimeout", SofaRegistryConstants.KEY_TIMEOUT);
        Assert.assertEquals("retries", SofaRegistryConstants.KEY_RETRIES);
        Assert.assertEquals("sofa.group", SofaRegistryConstants.SOFA_GROUP_KEY);
    }
}
