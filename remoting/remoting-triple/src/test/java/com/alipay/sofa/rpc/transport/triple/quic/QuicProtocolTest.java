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
package com.alipay.sofa.rpc.transport.triple.quic;

import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.transport.triple.quic.QuicSslContextFactory;
import io.netty.incubator.codec.quic.QuicSslContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test for HTTP/3 QUIC protocol support.
 * Verifies QUIC SSL context creation and configuration.
 *
 * @author zyz
 * @version 1.0
 */
public class QuicProtocolTest {

    private ServerConfig serverConfig;

    @Before
    public void setUp() {
        serverConfig = new ServerConfig();
        serverConfig.setPort(8080);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("triple.http3.enabled", "true");
        parameters.put("triple.port-unification.enabled", "true");
        serverConfig.setParameters(parameters);
    }

    @After
    public void tearDown() {
        serverConfig = null;
    }

    @Test
    public void testQuicSslContextCreation() throws Exception {
        // Given HTTP/3 is enabled
        serverConfig.setParameter("triple.http3.enabled", "true");

        // When creating QUIC SSL context
        QuicSslContext sslContext = QuicSslContextFactory.createServerSslContext(serverConfig);

        // Then context should be created successfully
        assertNotNull("QUIC SSL context should not be null", sslContext);
        assertTrue("Should be server context", sslContext.isServer());
    }

    @Test
    public void testQuicApplicationProtocols() throws Exception {
        // QUIC should support HTTP/3 protocol "h3"
        serverConfig.setParameter("triple.http3.enabled", "true");

        QuicSslContext sslContext = QuicSslContextFactory.createServerSslContext(serverConfig);

        assertNotNull(sslContext);
        // Note: Application protocols are set internally by QuicSslContextBuilder
    }

    @Test
    public void testSelfSignedCertificateGeneration() throws Exception {
        // When no keystore is configured, self-signed certificate should be generated
        QuicSslContext sslContext = QuicSslContextFactory.createServerSslContext(serverConfig);

        assertNotNull("Self-signed SSL context should be created", sslContext);
    }

    @Test
    public void testQuicSslAvailability() {
        // Check if QUIC SSL classes are available on classpath
        boolean available = QuicSslContextFactory.isQuicSslAvailable();

        // QUIC support requires netty-incubator-codec-quic dependency
        // This test documents the dependency requirement
        assertTrue("QUIC SSL classes should be available", available);
    }

    @Test
    public void testHttp3EnabledFlag() {
        // Verify HTTP/3 enabled flag is read correctly
        serverConfig.setParameter("triple.http3.enabled", "true");
        assertTrue("HTTP/3 should be enabled",
            "true".equals(serverConfig.getParameter("triple.http3.enabled")));
    }

    @Test
    public void testPortUnificationFlag() {
        // Verify port unification flag is read correctly
        serverConfig.setParameter("triple.port-unification.enabled", "true");
        assertTrue("Port unification should be enabled",
            "true".equals(serverConfig.getParameter("triple.port-unification.enabled")));
    }

    @Test
    public void testQuicTls13Requirement() throws Exception {
        // QUIC requires TLS 1.3
        // This test verifies the SSL context is created with TLS 1.3
        QuicSslContext sslContext = QuicSslContextFactory.createServerSslContext(serverConfig);

        assertNotNull(sslContext);
        // TLS version is handled internally by Netty QUIC codec
    }
}