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
package com.alipay.sofa.rpc.registry.utils;

import com.alipay.sofa.rpc.config.ServerConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link RegistryUtils#getServerHost(ServerConfig)}
 * and {@link RegistryUtils#getServerPort(ServerConfig)}.
 *
 * @author songlan
 */
public class RegistryUtilsTest {

    @Test
    public void testGetServerHost_virtualHostConfigured() {
        ServerConfig server = new ServerConfig()
            .setHost("0.0.0.0")
            .setVirtualHost("10.0.0.1");
        Assert.assertEquals("10.0.0.1", RegistryUtils.getServerHost(server));
    }

    @Test
    public void testGetServerHost_noVirtualHost_returnsHost() {
        ServerConfig server = new ServerConfig()
            .setHost("192.168.1.1");
        Assert.assertEquals("192.168.1.1", RegistryUtils.getServerHost(server));
    }

    @Test
    public void testGetServerHost_anyHostFallbacksToLocal() {
        ServerConfig server = new ServerConfig()
            .setHost("0.0.0.0");
        String host = RegistryUtils.getServerHost(server);
        Assert.assertNotNull(host);
        Assert.assertFalse("0.0.0.0".equals(host));
    }

    @Test
    public void testGetServerHost_localhostFallbacksToLocal() {
        ServerConfig server = new ServerConfig()
            .setHost("127.0.0.1");
        String host = RegistryUtils.getServerHost(server);
        Assert.assertNotNull(host);
        Assert.assertFalse("127.0.0.1".equals(host));
    }

    @Test
    public void testGetServerPort_virtualPortConfigured() {
        ServerConfig server = new ServerConfig()
            .setPort(12200)
            .setVirtualPort(80);
        Assert.assertEquals(80, RegistryUtils.getServerPort(server));
    }

    @Test
    public void testGetServerPort_noVirtualPort_returnsPort() {
        ServerConfig server = new ServerConfig()
            .setPort(12200);
        Assert.assertEquals(12200, RegistryUtils.getServerPort(server));
    }

    @Test
    public void testGetServerPort_bothConfigured_prefersVirtualPort() {
        ServerConfig server = new ServerConfig()
            .setHost("0.0.0.0")
            .setPort(12200)
            .setVirtualHost("10.0.0.1")
            .setVirtualPort(80);
        Assert.assertEquals("10.0.0.1", RegistryUtils.getServerHost(server));
        Assert.assertEquals(80, RegistryUtils.getServerPort(server));
    }

    @Test
    public void testGetServerPort_neitherConfigured_returnsDefaultPort() {
        ServerConfig server = new ServerConfig();
        Assert.assertEquals(server.getPort(), RegistryUtils.getServerPort(server));
    }
}
