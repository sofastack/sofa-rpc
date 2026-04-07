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
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Factory for creating QUIC SSL contexts.
 * QUIC requires TLS 1.3 as mandatory.
 *
 * <p>This factory supports:
 * <ul>
 *   <li>Loading certificates from configured keystore</li>
 *   <li>Generating self-signed certificates for development/testing</li>
 * </ul>
 */
public class QuicSslContextFactory {

    private static final Logger LOGGER                = LoggerFactory.getLogger(QuicSslContextFactory.class);

    // Configuration keys
    private static final String KEYSTORE_PATH_KEY     = "ssl.keyStore.path";
    private static final String KEYSTORE_PASSWORD_KEY = "ssl.keyStore.password";
    private static final String KEYSTORE_TYPE_KEY     = "ssl.keyStore.type";
    private static final String CLIENT_AUTH_KEY       = "ssl.client.auth";

    private static final String DEFAULT_KEYSTORE_TYPE = "PKCS12";

    /**
     * Create server SSL context for QUIC.
     * QUIC requires TLS 1.3.
     *
     * @param serverConfig server configuration
     * @return QuicSslContext for server
     * @throws SSLException if SSL context creation fails
     */
    public static QuicSslContext createServerSslContext(ServerConfig serverConfig) throws SSLException {
        Map<String, String> parameters = serverConfig.getParameters();

        try {
            // Get or generate certificate
            CertificateInfo certInfo = getCertificateInfo(parameters);

            QuicSslContextBuilder builder = QuicSslContextBuilder.forServer(
                certInfo.privateKey,
                null,
                certInfo.certificateChain
                );

            // Application protocols - HTTP/3 uses "h3"
            builder.applicationProtocols("h3");

            // Client authentication (optional)
            if (isClientAuthEnabled(parameters)) {
                builder.clientAuth(ClientAuth.REQUIRE);
                TrustManagerFactory tmf = getTrustManagerFactory(parameters);
                if (tmf != null) {
                    builder.trustManager(tmf);
                }
            }

            QuicSslContext context = builder.build();
            LOGGER.info("QUIC SSL context created successfully with TLS 1.3");

            return context;
        } catch (Exception e) {
            throw new SSLException("Failed to create QUIC SSL context", e);
        }
    }

    /**
     * Certificate info holder.
     */
    private static class CertificateInfo {
        PrivateKey        privateKey;
        X509Certificate[] certificateChain;

        CertificateInfo(PrivateKey privateKey, X509Certificate[] certificateChain) {
            this.privateKey = privateKey;
            this.certificateChain = certificateChain;
        }
    }

    /**
     * Get certificate info from configuration or generate self-signed certificate.
     */
    private static CertificateInfo getCertificateInfo(Map<String, String> parameters) throws Exception {
        if (parameters == null) {
            return generateSelfSignedCertificate();
        }

        String keyStorePath = parameters.get(KEYSTORE_PATH_KEY);
        String keyStorePassword = parameters.get(KEYSTORE_PASSWORD_KEY);

        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            try {
                return loadCertificateInfo(keyStorePath, keyStorePassword,
                    parameters.getOrDefault(KEYSTORE_TYPE_KEY, DEFAULT_KEYSTORE_TYPE));
            } catch (Exception e) {
                LOGGER.warn("Failed to load keystore from {}, falling back to self-signed certificate: {}",
                    keyStorePath, e.getMessage());
            }
        }

        // Generate self-signed certificate for development/testing
        LOGGER.info("No keystore configured, generating self-signed certificate for QUIC");
        return generateSelfSignedCertificate();
    }

    /**
     * Load certificate info from keystore file.
     */
    private static CertificateInfo loadCertificateInfo(String keyStorePath,
                                                       String keyStorePassword,
                                                       String keyStoreType) throws Exception {
        char[] password = keyStorePassword != null ? keyStorePassword.toCharArray() : new char[0];

        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (InputStream is = new FileInputStream(keyStorePath)) {
            keyStore.load(is, password);
        }

        // Get the first key entry
        String alias = null;
        for (java.util.Enumeration<String> e = keyStore.aliases(); e.hasMoreElements();) {
            String a = e.nextElement();
            if (keyStore.isKeyEntry(a)) {
                alias = a;
                break;
            }
        }

        if (alias == null) {
            throw new Exception("No key entry found in keystore");
        }

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
        Certificate[] certs = keyStore.getCertificateChain(alias);

        X509Certificate[] x509Certs = new X509Certificate[certs.length];
        for (int i = 0; i < certs.length; i++) {
            x509Certs[i] = (X509Certificate) certs[i];
        }

        LOGGER.info("Loaded keystore from: {}", keyStorePath);
        return new CertificateInfo(privateKey, x509Certs);
    }

    /**
     * Generate self-signed certificate for development/testing.
     */
    private static CertificateInfo generateSelfSignedCertificate() throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();

        X509Certificate[] certChain = new X509Certificate[] { ssc.cert() };

        LOGGER.warn("Using self-signed certificate for QUIC. " +
            "This should NOT be used in production environments!");

        return new CertificateInfo(ssc.key(), certChain);
    }

    /**
     * Check if client authentication is enabled.
     */
    private static boolean isClientAuthEnabled(Map<String, String> parameters) {
        if (parameters == null) {
            return false;
        }
        String clientAuth = parameters.get(CLIENT_AUTH_KEY);
        return "true".equalsIgnoreCase(clientAuth) || "require".equalsIgnoreCase(clientAuth);
    }

    /**
     * Get trust manager factory for client authentication.
     */
    private static TrustManagerFactory getTrustManagerFactory(Map<String, String> parameters) {
        // TODO: Support truststore configuration if needed
        return null;
    }

    /**
     * Check if QUIC SSL support is available.
     *
     * @return true if QUIC SSL classes are available
     */
    public static boolean isQuicSslAvailable() {
        try {
            Class.forName("io.netty.incubator.codec.quic.QuicSslContext");
            Class.forName("io.netty.incubator.codec.quic.QuicSslContextBuilder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}