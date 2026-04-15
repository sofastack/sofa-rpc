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
package com.alipay.sofa.rpc.transport.triple;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.server.triple.HelloService;
import com.alipay.sofa.rpc.server.triple.HelloServiceImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * End-to-end integration test for the triple (HTTP/2) protocol.
 *
 * <p>Starts a real in-process TripleServer, configures a ProviderConfig to export
 * {@link HelloService}, then uses a ConsumerConfig with {@code directUrl} to call
 * the service over HTTP/2 without any service registry. Verifies that:
 * <ul>
 *   <li>Unary sync calls work correctly (string, primitive, array)</li>
 *   <li>Concurrent calls succeed without cross-stream contamination</li>
 *   <li>The SPI registration {@code tri=} is honoured end-to-end</li>
 * </ul>
 *
 * <p>Port: 50077 (not used by any other test in this module).
 */
public class TripleHttp2IntegrationTest {

    private static final int                    PORT = 50077;

    private static ProviderConfig<HelloService> providerConfig;
    private static ConsumerConfig<HelloService> consumerConfig;
    private static HelloService                 helloService;

    @BeforeClass
    public static void startServer() throws InterruptedException {
        RpcRunningState.setUnitTestMode(true);

        // ── Provider (server) side ────────────────────────────────────────────
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("tri")
            .setPort(PORT)
            .setDaemon(true);

        providerConfig = new ProviderConfig<HelloService>()
            .setApplication(new ApplicationConfig().setAppName("hello-server"))
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();

        // ── Consumer (client) side ────────────────────────────────────────────
        consumerConfig = new ConsumerConfig<HelloService>()
            .setApplication(new ApplicationConfig().setAppName("hello-client"))
            .setInterfaceId(HelloService.class.getName())
            .setProtocol("tri")
            .setDirectUrl("tri://127.0.0.1:" + PORT)
            .setTimeout(5000)
            .setRegister(false);

        helloService = consumerConfig.refer();

        // Give the gRPC server a moment to be fully ready
        Thread.sleep(500);
    }

    @AfterClass
    public static void stopServer() {
        if (consumerConfig != null) {
            try {
                consumerConfig.unRefer();
            } catch (Exception ignored) {
            }
        }
        if (providerConfig != null) {
            try {
                providerConfig.unExport();
            } catch (Exception ignored) {
            }
        }
    }

    // ── Unary call: String parameter ─────────────────────────────────────────

    @Test
    public void testHello_returnsGreeting() {
        String result = helloService.hello("world");
        Assert.assertEquals("hello world", result);
    }

    @Test
    public void testHello_emptyName() {
        String result = helloService.hello("");
        Assert.assertEquals("hello ", result);
    }

    @Test
    public void testHello_chineseCharacters() {
        String result = helloService.hello("世界");
        Assert.assertEquals("hello 世界", result);
    }

    @Test
    public void testHello_longString() {
        String name = repeat("x", 1024);
        String result = helloService.hello(name);
        Assert.assertEquals("hello " + name, result);
    }

    // ── Unary call: primitive type ────────────────────────────────────────────

    @Test
    public void testPrimitiveType_returnsEcho() {
        Assert.assertEquals(0L, helloService.testPrimitiveType(0L));
        Assert.assertEquals(42L, helloService.testPrimitiveType(42L));
        Assert.assertEquals(Long.MAX_VALUE, helloService.testPrimitiveType(Long.MAX_VALUE));
        Assert.assertEquals(Long.MIN_VALUE, helloService.testPrimitiveType(Long.MIN_VALUE));
        Assert.assertEquals(-1L, helloService.testPrimitiveType(-1L));
    }

    // ── Unary call: array parameter ───────────────────────────────────────────

    @Test
    public void testArray_returnsEcho() {
        long[] input = { 1L, 2L, 3L, Long.MAX_VALUE };
        long[] result = helloService.testArray(input);
        Assert.assertArrayEquals(input, result);
    }

    @Test
    public void testArray_empty() {
        long[] result = helloService.testArray(new long[0]);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void testArray_singleElement() {
        long[] result = helloService.testArray(new long[] { 99L });
        Assert.assertArrayEquals(new long[] { 99L }, result);
    }

    // ── Multiple sequential calls (connection reuse) ──────────────────────────

    @Test
    public void testMultipleSequentialCalls() {
        for (int i = 0; i < 10; i++) {
            String result = helloService.hello("user" + i);
            Assert.assertEquals("hello user" + i, result);
        }
    }

    // ── Concurrent calls ──────────────────────────────────────────────────────

    @Test
    public void testConcurrentCalls() throws InterruptedException {
        int threads = 5;
        String[] results = new String[threads];
        Throwable[] errors = new Throwable[threads];

        Thread[] pool = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool[i] = new Thread(() -> {
                try {
                    results[idx] = helloService.hello("concurrent-" + idx);
                } catch (Throwable t) {
                    errors[idx] = t;
                }
            });
        }
        for (Thread t : pool) {
            t.start();
        }
        for (Thread t : pool) {
            t.join(10000);
        }

        for (int i = 0; i < threads; i++) {
            Assert.assertNull("Thread " + i + " threw: " + errors[i], errors[i]);
            Assert.assertEquals("hello concurrent-" + i, results[i]);
        }
    }

    // ── Timeout: configures correctly via ConsumerConfig ─────────────────────

    @Test
    public void testTimeout_callSucceedsWithinTimeout() {
        // Just verify that a normal call finishes well within the configured 5s timeout
        long start = System.currentTimeMillis();
        String result = helloService.hello("timeout-test");
        long elapsed = System.currentTimeMillis() - start;

        Assert.assertEquals("hello timeout-test", result);
        Assert.assertTrue("Call should complete within 5s, took " + elapsed + "ms", elapsed < 5000);
    }

    // ── Sanity: server and consumer config objects are wired correctly ─────────

    @Test
    public void testServerConfig_protocolIsTri() {
        Assert.assertEquals("tri", providerConfig.getServer().get(0).getProtocol());
        Assert.assertEquals(PORT, providerConfig.getServer().get(0).getPort());
    }

    @Test
    public void testConsumerConfig_protocolIsTri() {
        Assert.assertEquals("tri", consumerConfig.getProtocol());
    }

    @Test
    public void testProxyIsNotNull() {
        Assert.assertNotNull(helloService);
    }

    // ── Error handling: calling unreachable service ───────────────────────────

    @Test
    public void testUnreachableServer_throwsSofaRpcException() {
        ConsumerConfig<HelloService> badConfig = new ConsumerConfig<HelloService>()
            .setApplication(new ApplicationConfig().setAppName("bad-client"))
            .setInterfaceId(HelloService.class.getName())
            .setProtocol("tri")
            .setDirectUrl("tri://127.0.0.1:19999") // nothing listening here
            .setTimeout(1000)
            .setRegister(false);

        HelloService proxy = badConfig.refer();
        try {
            proxy.hello("x");
            Assert.fail("Should throw SofaRpcException for unreachable server");
        } catch (SofaRpcException e) {
            // expected
        } finally {
            badConfig.unRefer();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
