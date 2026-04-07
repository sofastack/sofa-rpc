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
 * Integration tests for HTTP/1.1 with HelloService that have uniqueId.
 * Tests multiple service implementations with different uniqueIds.
 * Tests synchronous calls via HTTP/1.1.
 *
 * Note: This test class should be run in isolation.
 * Run individually: mvn test -pl test/test-integration -Dtest=Http1PojoUniqueIdTest
 */
public class Http1PojoUniqueIdTest {

    private static final Logger                 LOGGER       = LoggerFactory
                                                                 .getLogger(Http1PojoUniqueIdTest.class);

    private static final int                    PORT         = 50074;

    private static ProviderConfig<HelloService> providerConfig1;
    private static ProviderConfig<HelloService> providerConfig2;
    private static ServerConfig                 serverConfig;
    private static HelloServiceImpl             serviceImpl1;
    private static HelloServiceImpl             serviceImpl2;

    private static final String                 SERVICE_PATH = "/com.alipay.sofa.rpc.test.triple.stream.HelloService/";

    @BeforeClass
    public static void setUp() throws InterruptedException {
        RpcRunningState.setUnitTestMode(true);

        // Publish two HelloService with different uniqueIds
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("http1-pojo-uniqueid-test");

        serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(PORT);

        // Enable HTTP/1.1 support
        Map<String, String> parameters = new HashMap<>();
        parameters.put("triple.http1.enabled", "true");
        serverConfig.setParameters(parameters);

        // First service with uniqueId "service1"
        serviceImpl1 = new HelloServiceImpl();
        providerConfig1 = new ProviderConfig<HelloService>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("service1")
            .setRef(serviceImpl1)
            .setRepeatedExportLimit(-1)
            .setServer(serverConfig);

        providerConfig1.export();

        // Second service with uniqueId "service2"
        serviceImpl2 = new HelloServiceImpl();
        providerConfig2 = new ProviderConfig<HelloService>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("service2")
            .setRef(serviceImpl2)
            .setRepeatedExportLimit(-1)
            .setServer(serverConfig);

        providerConfig2.export();

        // Wait for server to start
        Thread.sleep(2000);
    }

    @AfterClass
    public static void tearDown() {
        if (providerConfig1 != null) {
            providerConfig1.unExport();
        }
        if (providerConfig2 != null) {
            providerConfig2.unExport();
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
            // ignore
        }
    }

    // ==================== HTTP/1.1 Synchronous Call Tests with uniqueId ====================

    /**
     * Test calling service with uniqueId "service1" via HTTP/1.1.
     */
    @Test
    public void testSyncCallWithUniqueId1ViaHttp1() throws Exception {
        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
        String jsonBody = "{\"msg\":\"service1-test\",\"count\":100}";

        HttpURLConnection connection = createConnection(url, jsonBody, "service1");

        int responseCode = connection.getResponseCode();
        String response = readResponse(connection);

        LOGGER.info("Service1 sayHello Response Code: {}", responseCode);
        LOGGER.info("Service1 sayHello Response: {}", response);

        Assert.assertEquals("HTTP request should succeed", 200, responseCode);
        Assert.assertTrue("Response should contain message", response.contains("service1-test"));
        Assert.assertTrue("Response should contain count", response.contains("100"));

        connection.disconnect();
    }

    /**
     * Test calling service with uniqueId "service2" via HTTP/1.1.
     */
    @Test
    public void testSyncCallWithUniqueId2ViaHttp1() throws Exception {
        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
        String jsonBody = "{\"msg\":\"service2-test\",\"count\":200}";

        HttpURLConnection connection = createConnection(url, jsonBody, "service2");

        int responseCode = connection.getResponseCode();
        String response = readResponse(connection);

        LOGGER.info("Service2 sayHello Response Code: {}", responseCode);
        LOGGER.info("Service2 sayHello Response: {}", response);

        Assert.assertEquals("HTTP request should succeed", 200, responseCode);
        Assert.assertTrue("Response should contain message", response.contains("service2-test"));
        Assert.assertTrue("Response should contain count", response.contains("200"));

        connection.disconnect();
    }

    /**
     * Test calling service without uniqueId header when multiple services exist.
     * This should fail because the server doesn't know which service to route to.
     */
    @Test
    public void testWithoutUniqueIdHeader() throws Exception {
        String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
        String jsonBody = "{\"msg\":\"no-uniqueid\",\"count\":1}";

        HttpURLConnection connection = createConnection(url, jsonBody, null);

        int responseCode = connection.getResponseCode();
        String response = readResponse(connection);

        LOGGER.info("Without uniqueId header Response Code: {}", responseCode);
        LOGGER.info("Without uniqueId header Response: {}", response);

        // Should return 500 because there are multiple services with different uniqueIds
        Assert.assertEquals("Should fail without uniqueId when multiple services exist", 500, responseCode);

        connection.disconnect();
    }

    /**
     * Test multiple concurrent requests to service1.
     */
    @Test
    public void testConcurrentRequestsToService1() throws Exception {
        int numRequests = 5;
        Thread[] threads = new Thread[numRequests];
        final int[] successCount = {0};

        for (int i = 0; i < numRequests; i++) {
            threads[i] = new Thread(() -> {
                try {
                    String url = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
                    String jsonBody = "{\"msg\":\"concurrent\",\"count\":1}";

                    HttpURLConnection connection = createConnection(url, jsonBody, "service1");
                    int responseCode = connection.getResponseCode();

                    if (responseCode == 200) {
                        synchronized (successCount) {
                            successCount[0]++;
                        }
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(10000);
        }

        LOGGER.info("Concurrent requests success count: {}/{}", successCount[0], numRequests);
        Assert.assertEquals("All concurrent requests should succeed", numRequests, successCount[0]);
    }

    /**
     * Test alternating calls between service1 and service2.
     */
    @Test
    public void testAlternatingCallsBetweenServices() throws Exception {
        for (int i = 0; i < 3; i++) {
            // Call service1
            String url1 = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
            String jsonBody1 = "{\"msg\":\"alt-service1-" + i + "\",\"count\":" + (i * 10) + "}";

            HttpURLConnection connection1 = createConnection(url1, jsonBody1, "service1");
            int responseCode1 = connection1.getResponseCode();
            String response1 = readResponse(connection1);

            LOGGER.info("Alternating call service1-{} Response Code: {}", i, responseCode1);
            Assert.assertEquals("HTTP request to service1 should succeed", 200, responseCode1);
            connection1.disconnect();

            // Call service2
            String url2 = "http://127.0.0.1:" + PORT + SERVICE_PATH + "sayHello";
            String jsonBody2 = "{\"msg\":\"alt-service2-" + i + "\",\"count\":" + (i * 20) + "}";

            HttpURLConnection connection2 = createConnection(url2, jsonBody2, "service2");
            int responseCode2 = connection2.getResponseCode();
            String response2 = readResponse(connection2);

            LOGGER.info("Alternating call service2-{} Response Code: {}", i, responseCode2);
            Assert.assertEquals("HTTP request to service2 should succeed", 200, responseCode2);
            connection2.disconnect();
        }
    }

    // ==================== Helper Methods ====================

    private static HttpURLConnection createConnection(String url, String jsonBody, String uniqueId) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        if (uniqueId != null && !uniqueId.isEmpty()) {
            connection.setRequestProperty("tri-unique-id", uniqueId);
        }
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        return connection;
    }

    private static String readResponse(HttpURLConnection connection) throws Exception {
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