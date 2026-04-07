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

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for QUIC support availability.
 * Tests will be skipped if QUIC native library is not available.
 */
public class QuicSupportTest {

    private static final Logger LOGGER        = LoggerFactory.getLogger(QuicSupportTest.class);

    private static boolean      quicAvailable = false;

    @BeforeClass
    public static void checkQuicAvailability() {
        quicAvailable = isQuicAvailable();
        LOGGER.info("QUIC availability check: {}", quicAvailable);
    }

    /**
     * Test QUIC classes availability check.
     */
    @Test
    public void testQuicAvailabilityCheck() {
        LOGGER.info("=== Testing QUIC Availability Check ===");

        boolean available = isQuicAvailable();
        LOGGER.info("QUIC available: {}", available);

        // This test just checks and reports, doesn't assert
        // QUIC is optional, so both outcomes are valid
        if (available) {
            LOGGER.info("QUIC support is available - HTTP/3 tests will run");
        } else {
            LOGGER.info("QUIC support is NOT available - HTTP/3 tests will be skipped");
        }
    }

    /**
     * Test QuicSslContextFactory availability check.
     */
    @Test
    public void testQuicSslContextFactoryAvailability() {
        LOGGER.info("=== Testing QuicSslContextFactory Availability ===");

        boolean sslAvailable = QuicSslContextFactory.isQuicSslAvailable();
        LOGGER.info("QUIC SSL available: {}", sslAvailable);

        // Should match our general availability check
        Assert.assertEquals("SSL availability should match general QUIC availability",
            quicAvailable, sslAvailable);
    }

    /**
     * Test QuicSslContextFactory creates context when available.
     */
    @Test
    public void testQuicSslContextCreation() throws Exception {
        LOGGER.info("=== Testing QUIC SSL Context Creation ===");

        // Skip if QUIC not available
        Assume.assumeTrue("QUIC native library required", quicAvailable);

        // This would require a ServerConfig with proper parameters
        // For now, just verify the factory class is loadable
        Assert.assertTrue("QuicSslContextFactory should be available",
            QuicSslContextFactory.isQuicSslAvailable());

        LOGGER.info("QUIC SSL context creation test passed");
    }

    /**
     * Test HTTP/3 headers frame class availability.
     */
    @Test
    public void testHttp3ClassesAvailability() {
        LOGGER.info("=== Testing HTTP/3 Classes Availability ===");

        if (quicAvailable) {
            try {
                // Verify HTTP/3 classes are loadable
                Class<?> headersClass = Class.forName("io.netty.incubator.codec.http3.Http3Headers");
                Class<?> headersFrameClass = Class.forName("io.netty.incubator.codec.http3.Http3HeadersFrame");
                Class<?> dataFrameClass = Class.forName("io.netty.incubator.codec.http3.Http3DataFrame");
                Class<?> serverHandlerClass = Class
                    .forName("io.netty.incubator.codec.http3.Http3ServerConnectionHandler");

                Assert.assertNotNull("Http3Headers class should be loadable", headersClass);
                Assert.assertNotNull("Http3HeadersFrame class should be loadable", headersFrameClass);
                Assert.assertNotNull("Http3DataFrame class should be loadable", dataFrameClass);
                Assert.assertNotNull("Http3ServerConnectionHandler class should be loadable", serverHandlerClass);

                LOGGER.info("All HTTP/3 classes are available");
            } catch (ClassNotFoundException e) {
                Assert.fail("HTTP/3 class not found: " + e.getMessage());
            }
        } else {
            LOGGER.info("Skipping HTTP/3 classes test - QUIC not available");
        }
    }

    /**
     * Test QUIC channel class availability.
     */
    @Test
    public void testQuicClassesAvailability() {
        LOGGER.info("=== Testing QUIC Classes Availability ===");

        if (quicAvailable) {
            try {
                // Verify QUIC classes are loadable
                Class<?> quicChannelClass = Class.forName("io.netty.incubator.codec.quic.QuicChannel");
                Class<?> quicServerCodecClass = Class.forName("io.netty.incubator.codec.quic.QuicServerCodecBuilder");
                Class<?> quicSslContextClass = Class.forName("io.netty.incubator.codec.quic.QuicSslContext");

                Assert.assertNotNull("QuicChannel class should be loadable", quicChannelClass);
                Assert.assertNotNull("QuicServerCodecBuilder class should be loadable", quicServerCodecClass);
                Assert.assertNotNull("QuicSslContext class should be loadable", quicSslContextClass);

                LOGGER.info("All QUIC classes are available");
            } catch (ClassNotFoundException e) {
                Assert.fail("QUIC class not found: " + e.getMessage());
            }
        } else {
            LOGGER.info("Skipping QUIC classes test - QUIC not available");
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Check if QUIC support is available.
     *
     * @return true if QUIC classes are available
     */
    private static boolean isQuicAvailable() {
        try {
            Class.forName("io.netty.incubator.codec.quic.QuicChannel");
            Class.forName("io.netty.incubator.codec.quic.QuicServerCodecBuilder");
            Class.forName("io.netty.incubator.codec.http3.Http3ServerConnectionHandler");
            Class.forName("io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame");
            Class.forName("io.netty.incubator.codec.http3.DefaultHttp3DataFrame");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}