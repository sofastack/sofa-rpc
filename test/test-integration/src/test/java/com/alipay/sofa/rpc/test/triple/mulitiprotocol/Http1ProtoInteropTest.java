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
package com.alipay.sofa.rpc.test.triple.mulitiprotocol;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.cache.RpcCacheManager;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.triple.GreeterImpl;
import io.grpc.examples.helloworld.SofaGreeterTriple;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for HTTP/1.1 interoperability with Triple protocol (Protobuf services).
 * Tests HTTP/1.1 access to Triple services with triple.http1.enabled=true.
 *
 * <h3>Manual Testing with curl:</h3>
 *
 * <h4>1. Unary Call (sayHello):</h4>
 * <pre>
 * curl -X POST http://localhost:50072/helloworld.Greeter/SayHello \
 *   -H "Content-Type: application/json" \
 *   -d '{"name":"http1-test"}'
 *
 * # Response: {"message":"Hello http1-test"}
 * </pre>
 *
 * <h4>2. Unary Call with Custom Headers:</h4>
 * <pre>
 * curl -X POST http://localhost:50072/helloworld.Greeter/SayHello \
 *   -H "Content-Type: application/json" \
 *   -H "tri-timeout: 5000" \
 *   -H "tri-service-version: 1.0.0" \
 *   -d '{"name":"header-test"}'
 * </pre>
 *
 * <h4>3. Using grpcurl (for comparison):</h4>
 * <pre>
 * grpcurl -plaintext -d '{"name":"http1-test"}' \
 *   localhost:50072 helloworld.Greeter/SayHello
 * </pre>
 *
 * <h3>Note:</h3>
 * <p>HTTP/1.1 does not support streaming calls. For streaming tests, use HTTP/2 or HTTP/3.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class Http1ProtoInteropTest {

    private static final Logger                               LOGGER       = LoggerFactory
                                                                               .getLogger(Http1ProtoInteropTest.class);

    private static final int                                  PORT         = 50072;

    private static ProviderConfig<SofaGreeterTriple.IGreeter> providerConfig;
    private static ServerConfig                               serverConfig;

    private static final String                               SERVICE_PATH = "/helloworld.Greeter/SayHello";

    @BeforeClass
    public static void setUp() throws InterruptedException {
        RpcRunningState.setUnitTestMode(true);

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("http1-proto-interop-test");

        serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(PORT);

        // Enable HTTP/1.1 support
        Map<String, String> parameters = new HashMap<>();
        parameters.put("triple.http1.enabled", "true");
        serverConfig.setParameters(parameters);

        providerConfig = new ProviderConfig<SofaGreeterTriple.IGreeter>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig);

        providerConfig.export();

        Thread.sleep(2000);

        LOGGER.info("HTTP/1.1 Proto Interop test server started on port: {}", PORT);
    }

    @AfterClass
    public static void tearDown() {
        if (providerConfig != null) {
            providerConfig.unExport();
        }
        if (serverConfig != null) {
            serverConfig.destroy();
        }
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();
        RpcCacheManager.clearAll();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== HTTP/1.1 JSON Tests (via curl-style HTTP) ====================

    /**
     * Test HTTP/1.1 Unary call with JSON content type.
     * Simulates: curl -X POST http://localhost:50072/helloworld.Greeter/SayHello \
     *   -H "Content-Type: application/json" -d '{"name":"http1-test"}'
     */
    @Test
    public void testHttp1UnaryWithJson() throws Exception {
        LOGGER.info("=== Testing HTTP/1.1 Unary with JSON ===");

        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH;
        String jsonBody = "{\"name\":\"http1-json-test\"}";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // Read response
        int responseCode = connection.getResponseCode();
        String response = readResponse(connection);

        LOGGER.info("HTTP/1.1 Unary Response Code: {}", responseCode);
        LOGGER.info("HTTP/1.1 Unary Response: {}", response);

        Assert.assertEquals("HTTP request should succeed", 200, responseCode);
        Assert.assertTrue("Response should contain the name", response.contains("http1-json-test"));

        connection.disconnect();
    }

    /**
     * Test HTTP/1.1 call with custom Triple headers.
     */
    @Test
    public void testHttp1WithCustomHeaders() throws Exception {
        LOGGER.info("=== Testing HTTP/1.1 with Custom Headers ===");

        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH;
        String jsonBody = "{\"name\":\"header-test\"}";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("tri-timeout", "5000");
        connection.setRequestProperty("tri-service-version", "1.0.0");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // Read response
        int responseCode = connection.getResponseCode();
        String response = readResponse(connection);

        LOGGER.info("HTTP/1.1 with Headers Response Code: {}", responseCode);
        LOGGER.info("HTTP/1.1 with Headers Response: {}", response);

        Assert.assertEquals("HTTP request should succeed", 200, responseCode);

        connection.disconnect();
    }

    /**
     * Test HTTP/1.1 call with non-existent method.
     */
    @Test
    public void testHttp1NonExistentMethod() throws Exception {
        LOGGER.info("=== Testing HTTP/1.1 with Non-existent Method ===");

        String url = "http://127.0.0.1:" + PORT + "/helloworld.Greeter/NonExistentMethod";
        String jsonBody = "{\"name\":\"test\"}";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = connection.getResponseCode();

        LOGGER.info("Non-existent method Response Code: {}", responseCode);

        // Should return error for non-existent method
        Assert.assertTrue("Should return error for non-existent method", responseCode >= 400);

        connection.disconnect();
    }

    // ==================== Helper Methods ====================

    private String readResponse(HttpURLConnection connection) throws Exception {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (Exception e) {
            inputStream = connection.getErrorStream();
        }

        if (inputStream == null) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}