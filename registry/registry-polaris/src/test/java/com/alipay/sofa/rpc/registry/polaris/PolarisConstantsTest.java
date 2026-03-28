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
package com.alipay.sofa.rpc.registry.polaris;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for PolarisConstants
 *
 * @author SOFA-RPC Team
 */
public class PolarisConstantsTest {

    @Test
    public void testConstantValues() {
        Assert.assertEquals("connector.protocol", PolarisConstants.POLARIS_SERVER_CONNECTOR_PROTOCOL_KEY);
        Assert.assertEquals("healthCheck.ttl", PolarisConstants.HEALTH_CHECK_TTL_KEY);
        Assert.assertEquals("heartbeat.interval", PolarisConstants.HEARTBEAT_INTERVAL_KEY);
        Assert.assertEquals("heartbeat.coreSize", PolarisConstants.HEARTBEAT_CORE_SIZE_KEY);
        Assert.assertEquals("lookup.interval", PolarisConstants.LOOKUP_INTERVAL_KEY);
        Assert.assertEquals("grpc", PolarisConstants.POLARIS_SERVER_CONNECTOR_PROTOCOL);
        Assert.assertEquals(10, PolarisConstants.DEFAULT_HEALTH_CHECK_TTL);
        Assert.assertEquals(3000, PolarisConstants.DEFAULT_HEARTBEAT_INTERVAL);
        Assert.assertEquals(1, PolarisConstants.DEFAULT_HEARTBEAT_CORE_SIZE);
        Assert.assertEquals(1000, PolarisConstants.DEFAULT_LOOKUP_INTERVAL);
    }
}
