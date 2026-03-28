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
package com.alipay.sofa.rpc.registry.consul;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for ConsulConstants
 *
 * @author SOFA-RPC Team
 */
public class ConsulConstantsTest {

    @Test
    public void testConsulServiceNameKey() {
        Assert.assertEquals("consulServiceName", ConsulConstants.CONSUL_SERVICE_NAME_KEY);
    }

    @Test
    public void testTokenKey() {
        Assert.assertEquals("token", ConsulConstants.TOKEN_KEY);
    }

    @Test
    public void testHeartbeatIntervalKey() {
        Assert.assertEquals("heartbeat.interval", ConsulConstants.HEARTBEAT_INTERVAL_KEY);
    }

    @Test
    public void testHeartbeatCoreSizeKey() {
        Assert.assertEquals("heartbeat.coreSize", ConsulConstants.HEARTBEAT_CORE_SIZE_KEY);
    }

    @Test
    public void testLookupIntervalKey() {
        Assert.assertEquals("lookup.interval", ConsulConstants.LOOKUP_INTERVAL_KEY);
    }

    @Test
    public void testWatchTimeoutKey() {
        Assert.assertEquals("watch.timeout", ConsulConstants.WATCH_TIMEOUT_KEY);
    }

    @Test
    public void testHealthCheckTypeKey() {
        Assert.assertEquals("healthCheck.type", ConsulConstants.HEALTH_CHECK_TYPE_KEY);
    }

    @Test
    public void testHealthCheckTtlKey() {
        Assert.assertEquals("healthCheck.ttl", ConsulConstants.HEALTH_CHECK_TTL_KEY);
    }

    @Test
    public void testHealthCheckHostKey() {
        Assert.assertEquals("healthCheck.host", ConsulConstants.HEALTH_CHECK_HOST_KEY);
    }

    @Test
    public void testHealthCheckPortKey() {
        Assert.assertEquals("healthCheck.port", ConsulConstants.HEALTH_CHECK_PORT_KEY);
    }

    @Test
    public void testHealthCheckTimeoutKey() {
        Assert.assertEquals("healthCheck.timeout", ConsulConstants.HEALTH_CHECK_TIMEOUT_KEY);
    }

    @Test
    public void testHealthCheckIntervalKey() {
        Assert.assertEquals("healthCheck.interval", ConsulConstants.HEALTH_CHECK_INTERVAL_KEY);
    }

    @Test
    public void testHealthCheckProtocolKey() {
        Assert.assertEquals("healthCheck.protocol", ConsulConstants.HEALTH_CHECK_PROTOCOL_KEY);
    }

    @Test
    public void testHealthCheckPathKey() {
        Assert.assertEquals("healthCheck.path", ConsulConstants.HEALTH_CHECK_PATH_KEY);
    }

    @Test
    public void testHealthCheckMethodKey() {
        Assert.assertEquals("healthCheck.method", ConsulConstants.HEALTH_CHECK_METHOD_KEY);
    }

    @Test
    public void testDefaultConsulPort() {
        Assert.assertEquals(8500, ConsulConstants.DEFAULT_CONSUL_PORT);
    }

    @Test
    public void testDefaultHeartbeatInterval() {
        Assert.assertEquals(3000, ConsulConstants.DEFAULT_HEARTBEAT_INTERVAL);
    }

    @Test
    public void testDefaultHeartbeatCoreSize() {
        Assert.assertEquals(1, ConsulConstants.DEFAULT_HEARTBEAT_CORE_SIZE);
    }

    @Test
    public void testDefaultLookupInterval() {
        Assert.assertEquals(1000, ConsulConstants.DEFAULT_LOOKUP_INTERVAL);
    }

    @Test
    public void testDefaultWatchTimeout() {
        Assert.assertEquals(5, ConsulConstants.DEFAULT_WATCH_TIMEOUT);
    }

    @Test
    public void testDefaultHealthCheckTtl() {
        Assert.assertEquals("10s", ConsulConstants.DEFAULT_HEALTH_CHECK_TTL);
    }

    @Test
    public void testDefaultHealthCheckTimeout() {
        Assert.assertEquals("1s", ConsulConstants.DEFAULT_HEALTH_CHECK_TIMEOUT);
    }

    @Test
    public void testDefaultHealthCheckInterval() {
        Assert.assertEquals("5s", ConsulConstants.DEFAULT_HEALTH_CHECK_INTERVAL);
    }

    @Test
    public void testDefaultHealthCheckProtocol() {
        Assert.assertEquals("http", ConsulConstants.DEFAULT_HEALTH_CHECK_PROTOCOL);
    }

    @Test
    public void testDefaultHealthCheckPath() {
        Assert.assertEquals("/health", ConsulConstants.DEFAULT_HEALTH_CHECK_PATH);
    }

    @Test
    public void testDefaultHealthCheckMethod() {
        Assert.assertEquals("GET", ConsulConstants.DEFAULT_HEALTH_CHECK_METHOD);
    }
}
