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
import com.alipay.sofa.rpc.test.triple.stream.HelloService;
import com.alipay.sofa.rpc.test.triple.stream.HelloServiceImpl;
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
 * Integration tests for HTTP/1.1 interoperability with HelloService.
 * Tests synchronous (unary) calls via HTTP/1.1.
 *
 * Note: This test class should be run in isolation due to shared global state in the RPC framework.
 * Run individually: mvn test -pl test/test-integration -Dtest=Http1PojoInteropTest
 */
public class Http1PojoInteropTest {

    private static final Logger                 LOGGER       = LoggerFactory
                                                                 .getLogger(Http1PojoInteropTest.class);

    private static final int                    PORT         = 50075;

    private static ProviderConfig<HelloService> providerConfig;
    private static ServerConfig                 serverConfig;
    private static HelloServiceImpl             serviceImpl;

    private static final String                 SERVICE_PATH = "/com.alipay.sofa.rpc.test.triple.stream.HelloService/";

    @BeforeClass
    public static void setUp() throws InterruptedException {
        RpcRunningState.setUnitTestMode(true);

        // Publish HelloService with HTTP/1.1 support enabled
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("http1-pojo-interop-test");

        serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(PORT);

        // Enable HTTP/1.1 support
        Map<String, String> parameters = new HashMap<>();
        parameters.put("triple.http1.enabled", "true");
        serverConfig.setParameters(parameters);

        serviceImpl = new HelloServiceImpl();
        providerConfig = new ProviderConfig<HelloService>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(HelloService.class.getName())
            .setRef(serviceImpl)
            .setRepeatedExportLimit(-1)
            .setServer(serverConfig);

        providerConfig.export();

        // Wait for server to start
        Thread.sleep(2000);
    }

    @AfterClass
    public static void tearDown() {
        if (providerConfig != null) {
            providerConfig.unExport();
        }
        if (serverConfig != null) {
            serverConfig.destroy();
        }
        // Clean up thread-local contexts
        RpcInternalContext.removeContext();
        RpcInvokeContext.removeContext();

        // Clear all RPC caches
        RpcCacheManager.clearAll();

        // Wait for resources to be fully released
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    // ==================== HTTP/1.1 Synchronous Call Tests ====================

    /**
     * Test HTTP/1.1 synchronous call to sayHello method.
     * Method: ServerResponse sayHello(ClientRequest clientRequest)
     */
    @Test
    public void testSyncCallViaHttp1() throws Exception {
        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
        String jsonBody = "{\"msg\":\"hello\",\"count\":10}";

        HttpURLConnection connection = createConnection(url, jsonBody);

        int responseCode = connection.getResponseCode();
        String response = readResponse(connection);

        LOGGER.info("Sync sayHello Response Code: {}", responseCode);
        LOGGER.info("Sync sayHello Response: {}", response);

        Assert.assertEquals("HTTP request should succeed", 200, responseCode);
        Assert.assertTrue("Response should contain msg", response.contains("hello"));
        Assert.assertTrue("Response should contain count", response.contains("10"));

        connection.disconnect();
    }

    /**
     * Test HTTP/1.1 call with different message content.
     */
    @Test
    public void testSyncCallWithDifferentMessage() throws Exception {
        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
        String jsonBody = "{\"msg\":\"test-message\",\"count\":100}";

        HttpURLConnection connection = createConnection(url, jsonBody);

        int responseCode = connection.getResponseCode();
        String response = readResponse(connection);

        LOGGER.info("Different message Response Code: {}", responseCode);
        LOGGER.info("Different message Response: {}", response);

        Assert.assertEquals("HTTP request should succeed", 200, responseCode);
        Assert.assertTrue("Response should contain message", response.contains("test-message"));
        Assert.assertTrue("Response should contain count", response.contains("100"));

        connection.disconnect();
    }

    /**
     * Test HTTP/1.1 call with custom headers.
     */
    @Test
    public void testWithCustomHeaders() throws Exception {
        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
        String jsonBody = "{\"msg\":\"custom-header-test\",\"count\":1}";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("tri-timeout", "5000");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        String response = readResponse(connection);

        LOGGER.info("Custom Headers Response Code: {}", responseCode);
        LOGGER.info("Custom Headers Response: {}", response);

        Assert.assertEquals("HTTP request should succeed", 200, responseCode);
        Assert.assertTrue("Response should contain msg", response.contains("custom-header-test"));

        connection.disconnect();
    }

    /**
     * Test HTTP/1.1 call to non-existent method returns error.
     */
    @Test
    public void testNonExistentMethod() throws Exception {
        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "nonExistentMethod";
        String jsonBody = "{\"msg\":\"test\",\"count\":1}";

        HttpURLConnection connection = createConnection(url, jsonBody);

        int responseCode = connection.getResponseCode();

        LOGGER.info("Non-existent Method Response Code: {}", responseCode);

        // Should return error for non-existent method
        Assert.assertTrue("Should return error for non-existent method", responseCode >= 400);

        connection.disconnect();
    }

    /**
     * Test GET request is rejected (only POST allowed for RPC).
     */
    @Test
    public void testGetMethodRejected() throws Exception {
        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();

        LOGGER.info("GET Method Response Code: {}", responseCode);

        // GET should be rejected with 405 Method Not Allowed
        Assert.assertEquals("GET should be rejected", 405, responseCode);

        connection.disconnect();
    }

    /**
     * Test HTTP/1.1 call with large count value.
     */
    @Test
    public void testSyncCallWithLargeCount() throws Exception {
        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
        String jsonBody = "{\"msg\":\"large-count-test\",\"count\":999999}";

        HttpURLConnection connection = createConnection(url, jsonBody);

        int responseCode = connection.getResponseCode();
        String response = readResponse(connection);

        LOGGER.info("Large count Response Code: {}", responseCode);
        LOGGER.info("Large count Response: {}", response);

        Assert.assertEquals("HTTP request should succeed", 200, responseCode);
        Assert.assertTrue("Response should contain count", response.contains("999999"));

        connection.disconnect();
    }

    /**
     * Test multiple sequential HTTP/1.1 calls.
     */
    @Test
    public void testMultipleSequentialCalls() throws Exception {
        for (int i = 0; i < 3; i++) {
            String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
            String jsonBody = "{\"msg\":\"sequential-" + i + "\",\"count\":" + i + "}";

            HttpURLConnection connection = createConnection(url, jsonBody);

            int responseCode = connection.getResponseCode();
            String response = readResponse(connection);

            LOGGER.info("Sequential call {} Response Code: {}", i, responseCode);
            LOGGER.info("Sequential call {} Response: {}", i, response);

            Assert.assertEquals("HTTP request should succeed", 200, responseCode);
            Assert.assertTrue("Response should contain count", response.contains(String.valueOf(i)));

            connection.disconnect();
        }
    }

    // ==================== Helper Methods ====================

    private HttpURLConnection createConnection(String url, String jsonBody) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        return connection;
    }

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