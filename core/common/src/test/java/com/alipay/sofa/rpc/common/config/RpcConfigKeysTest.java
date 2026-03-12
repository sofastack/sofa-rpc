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
package com.alipay.sofa.rpc.common.config;

import com.alipay.sofa.common.config.SofaConfigs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for RpcConfigKeys
 *
 * @author SOFA-RPC Team
 */
public class RpcConfigKeysTest {

    @Before
    public void setUp() {
        // Clean up any system properties before tests
        System.clearProperty("sofa.rpc.mesh.httpConnectionTimeout");
        System.clearProperty("sofa.rpc.mesh.httpReadTimeout");
        System.clearProperty("sofa.rpc.tracer.exposeType");
        System.clearProperty("sofa.rpc.remoting.http.enableSsl");
        System.clearProperty("sofa.rpc.codec.serialize.checkMode");
        System.clearProperty("mesh_http_connect_timeout");
        System.clearProperty("mesh_http_read_timeout");
        System.clearProperty("sofa.rpc.remoting.http.certificatePath");
        System.clearProperty("sofa.rpc.remoting.http.privateKeyPath");
        System.clearProperty("sofa.rpc.generic.throw.exception");
        System.clearProperty("sofa.rpc.serialize.blacklist.override");
        System.clearProperty("sofa.rpc.serialize.whitelist.override");
        System.clearProperty("sofa.rpc.server.thread.pool.type");
        System.clearProperty("sofa.rpc.triple.client.keepAlive.interval");
    }

    @After
    public void tearDown() {
        // Clean up system properties after tests
        System.clearProperty("sofa.rpc.mesh.httpConnectionTimeout");
        System.clearProperty("sofa.rpc.mesh.httpReadTimeout");
        System.clearProperty("sofa.rpc.tracer.exposeType");
        System.clearProperty("sofa.rpc.remoting.http.enableSsl");
        System.clearProperty("sofa.rpc.codec.serialize.checkMode");
        System.clearProperty("mesh_http_connect_timeout");
        System.clearProperty("mesh_http_read_timeout");
        System.clearProperty("sofa.rpc.remoting.http.certificatePath");
        System.clearProperty("sofa.rpc.remoting.http.privateKeyPath");
        System.clearProperty("sofa.rpc.generic.throw.exception");
        System.clearProperty("sofa.rpc.serialize.blacklist.override");
        System.clearProperty("sofa.rpc.serialize.whitelist.override");
        System.clearProperty("sofa.rpc.server.thread.pool.type");
        System.clearProperty("sofa.rpc.triple.client.keepAlive.interval");
    }

    @Test
    public void testMeshHttpConnectionTimeout() {
        // Test default value
        Integer value = SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_CONNECTION_TIMEOUT);
        assertEquals(Integer.valueOf(3000), value);

        // Test custom value
        System.setProperty("sofa.rpc.mesh.httpConnectionTimeout", "5000");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_CONNECTION_TIMEOUT);
        assertEquals(Integer.valueOf(5000), value);
    }

    @Test
    public void testMeshHttpReadTimeout() {
        // Test default value
        Integer value = SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_READ_TIMEOUT);
        assertEquals(Integer.valueOf(15000), value);

        // Test custom value
        System.setProperty("sofa.rpc.mesh.httpReadTimeout", "20000");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_READ_TIMEOUT);
        assertEquals(Integer.valueOf(20000), value);
    }

    @Test
    public void testTracerExposeType() {
        // Test default value
        String value = SofaConfigs.getOrDefault(RpcConfigKeys.TRACER_EXPOSE_TYPE);
        assertEquals("DISK", value);

        // Test custom value
        System.setProperty("sofa.rpc.tracer.exposeType", "MEMORY");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.TRACER_EXPOSE_TYPE);
        assertEquals("MEMORY", value);
    }

    @Test
    public void testRemotingHttpSslEnable() {
        // Test default value
        Boolean value = SofaConfigs.getOrDefault(RpcConfigKeys.REMOTING_HTTP_SSL_ENABLE);
        assertEquals(Boolean.FALSE, value);

        // Test custom value
        System.setProperty("sofa.rpc.remoting.http.enableSsl", "true");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.REMOTING_HTTP_SSL_ENABLE);
        assertEquals(Boolean.TRUE, value);
    }

    @Test
    public void testCertificatePath() {
        // Test default value
        String value = SofaConfigs.getOrDefault(RpcConfigKeys.CERTIFICATE_PATH);
        assertEquals("", value);

        // Test custom value
        System.setProperty("sofa.rpc.remoting.http.certificatePath", "/path/to/cert");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.CERTIFICATE_PATH);
        assertEquals("/path/to/cert", value);
    }

    @Test
    public void testPrivateKeyPath() {
        // Test default value
        String value = SofaConfigs.getOrDefault(RpcConfigKeys.PRIVATE_KEY_PATH);
        assertEquals("", value);

        // Test custom value
        System.setProperty("sofa.rpc.remoting.http.privateKeyPath", "/path/to/key");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.PRIVATE_KEY_PATH);
        assertEquals("/path/to/key", value);
    }

    @Test
    public void testGenericThrowException() {
        // Test default value
        Boolean value = SofaConfigs.getOrDefault(RpcConfigKeys.GENERIC_THROW_EXCEPTION);
        assertEquals(Boolean.FALSE, value);

        // Test custom value
        System.setProperty("sofa.rpc.generic.throw.exception", "true");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.GENERIC_THROW_EXCEPTION);
        assertEquals(Boolean.TRUE, value);
    }

    @Test
    public void testGenericThrowableFields() {
        // Test default value
        String[] value = SofaConfigs.getOrDefault(RpcConfigKeys.GENERIC_THROWABLE_FIELDS);
        assertNotNull(value);
        assertTrue(value.length > 0);
    }

    @Test
    public void testSerializeBlacklistOverride() {
        // Test default value
        String value = SofaConfigs.getOrDefault(RpcConfigKeys.SERIALIZE_BLACKLIST_OVERRIDE);
        assertEquals("", value);

        // Test custom value
        System.setProperty("sofa.rpc.serialize.blacklist.override", "com.example.BlackClass");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.SERIALIZE_BLACKLIST_OVERRIDE);
        assertEquals("com.example.BlackClass", value);
    }

    @Test
    public void testSerializeWhitelistOverride() {
        // Test default value
        String value = SofaConfigs.getOrDefault(RpcConfigKeys.SERIALIZE_WHITELIST_OVERRIDE);
        assertEquals("", value);

        // Test custom value
        System.setProperty("sofa.rpc.serialize.whitelist.override", "com.example.WhiteClass");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.SERIALIZE_WHITELIST_OVERRIDE);
        assertEquals("com.example.WhiteClass", value);
    }

    @Test
    public void testSerializeCheckerMode() {
        // Test default value
        String value = SofaConfigs.getOrDefault(RpcConfigKeys.SERIALIZE_CHECKER_MODE);
        assertEquals("STRICT", value);

        // Test custom value - WARN
        System.setProperty("sofa.rpc.codec.serialize.checkMode", "WARN");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.SERIALIZE_CHECKER_MODE);
        assertEquals("WARN", value);

        // Test custom value - DISABLE
        System.setProperty("sofa.rpc.codec.serialize.checkMode", "DISABLE");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.SERIALIZE_CHECKER_MODE);
        assertEquals("DISABLE", value);
    }

    @Test
    public void testServerThreadPoolType() {
        // Test default value
        String value = SofaConfigs.getOrDefault(RpcConfigKeys.SERVER_THREAD_POOL_TYPE);
        assertEquals("cached", value);

        // Test custom value
        System.setProperty("sofa.rpc.server.thread.pool.type", "fixed");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.SERVER_THREAD_POOL_TYPE);
        assertEquals("fixed", value);
    }

    @Test
    public void testTripleClientKeepAliveInterval() {
        // Test default value
        Integer value = SofaConfigs.getOrDefault(RpcConfigKeys.TRIPLE_CLIENT_KEEP_ALIVE_INTERVAL);
        assertEquals(Integer.valueOf(0), value);

        // Test custom value
        System.setProperty("sofa.rpc.triple.client.keepAlive.interval", "30");
        value = SofaConfigs.getOrDefault(RpcConfigKeys.TRIPLE_CLIENT_KEEP_ALIVE_INTERVAL);
        assertEquals(Integer.valueOf(30), value);
    }

    @Test
    public void testAllConfigKeysAreNonNull() {
        // Verify all config keys are non-null
        assertNotNull(RpcConfigKeys.MESH_HTTP_CONNECTION_TIMEOUT);
        assertNotNull(RpcConfigKeys.MESH_HTTP_READ_TIMEOUT);
        assertNotNull(RpcConfigKeys.TRACER_EXPOSE_TYPE);
        assertNotNull(RpcConfigKeys.REMOTING_HTTP_SSL_ENABLE);
        assertNotNull(RpcConfigKeys.CERTIFICATE_PATH);
        assertNotNull(RpcConfigKeys.PRIVATE_KEY_PATH);
        assertNotNull(RpcConfigKeys.TRIPLE_EXPOSE_OLD_UNIQUE_ID_SERVICE);
        assertNotNull(RpcConfigKeys.GENERIC_THROW_EXCEPTION);
        assertNotNull(RpcConfigKeys.GENERIC_THROWABLE_FIELDS);
        assertNotNull(RpcConfigKeys.SERIALIZE_BLACKLIST_OVERRIDE);
        assertNotNull(RpcConfigKeys.SERIALIZE_WHITELIST_OVERRIDE);
        assertNotNull(RpcConfigKeys.SERIALIZE_CHECKER_MODE);
        assertNotNull(RpcConfigKeys.SERVER_THREAD_POOL_TYPE);
        assertNotNull(RpcConfigKeys.TRIPLE_CLIENT_KEEP_ALIVE_INTERVAL);
    }

    @Test
    public void testAlternativePropertyNames() {
        // Test MESH_HTTP_CONNECTION_TIMEOUT with alternative name
        System.setProperty("mesh_http_connect_timeout", "7000");
        Integer value = SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_CONNECTION_TIMEOUT);
        assertEquals(Integer.valueOf(7000), value);

        // Test MESH_HTTP_READ_TIMEOUT with alternative name
        System.setProperty("mesh_http_read_timeout", "25000");
        Integer readValue = SofaConfigs.getOrDefault(RpcConfigKeys.MESH_HTTP_READ_TIMEOUT);
        assertEquals(Integer.valueOf(25000), readValue);
    }
}
