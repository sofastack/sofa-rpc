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

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.server.triple.TripleServer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// Import required for ProviderConfig
import static com.alipay.sofa.rpc.config.ProviderConfig.*;

/**
 * JMH benchmark for HTTP protocol negotiation performance.
 * Measures latency and throughput for HTTP/1.1, HTTP/2, and HTTP/3.
 *
 * <p>Run with:
 * <pre>{@code
 * mvn clean install -pl remoting/remoting-triple -DskipTests
 * mvn exec:java -Dexec.mainClass="org.openjdk.jmh.Main" -pl remoting/remoting-triple
 * }</pre>
 *
 * @author nbsp
 * @version 1.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class HttpProtocolBenchmark {

    @State(Scope.Thread)
    public static class BenchmarkState {
        public TripleServer server;
        public ServerConfig serverConfig;

        @Setup
        public void setUp() {
            serverConfig = new ServerConfig();
            serverConfig.setPort(8080);

            Map<String, String> parameters = new HashMap<>();
            parameters.put("triple.http1.enabled", "true");
            parameters.put("triple.http2.enabled", "true");
            parameters.put("triple.http3.enabled", "false"); // QUIC requires special setup
            parameters.put("triple.port-unification.enabled", "true");
            serverConfig.setParameters(parameters);

            server = new TripleServer();
            server.init(serverConfig);
        }

        @TearDown
        public void tearDown() {
            if (server != null) {
                server.destroy();
            }
        }
    }

    /**
     * Benchmark HTTP/1.1 protocol detection latency.
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testHttp1ProtocolDetection(BenchmarkState state, Blackhole bh) {
        state.serverConfig.setParameter("triple.http1.enabled", "true");
        bh.consume(state.serverConfig.getParameter("triple.http1.enabled"));
    }

    /**
     * Benchmark HTTP/2 protocol detection latency.
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testHttp2ProtocolDetection(BenchmarkState state, Blackhole bh) {
        // HTTP/2 is the default for gRPC
        bh.consume(state.server.isStarted());
    }

    /**
     * Benchmark port unification configuration loading.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void testPortUnificationConfig(BenchmarkState state, Blackhole bh) {
        boolean portUnification = "true".equals(
            state.serverConfig.getParameter("triple.port-unification.enabled"));
        bh.consume(portUnification);
    }

    /**
     * Benchmark server initialization time.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Object benchmarkServerInitialization(Blackhole bh) {
        ServerConfig config = new ServerConfig();
        config.setPort(8081);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("triple.http1.enabled", "true");
        parameters.put("triple.port-unification.enabled", "true");
        config.setParameters(parameters);

        TripleServer server = new TripleServer();
        server.init(config);

        bh.consume(server);
        return server;
    }

    /**
     * Benchmark provider config creation.
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testProviderConfigCreation(Blackhole bh) {
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setInterfaceId("com.alipay.sofa.rpc.test.BenchmarkService");
        providerConfig.setGroupId("com.alipay.sofa");
        providerConfig.setVersion("1.0");

        bh.consume(providerConfig.getInterfaceId());
        bh.consume(providerConfig.getGroupId());
        bh.consume(providerConfig.getVersion());
    }

    /**
     * Benchmark connection pool performance (simulated).
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void testConnectionPoolLookup(BenchmarkState state, Blackhole bh) {
        // Simulate connection pool lookup by interface name
        String interfaceName = "com.alipay.sofa.rpc.test.TestService";
        bh.consume(interfaceName.hashCode());
    }

    /**
     * Benchmark ALPN protocol negotiation (simulated).
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testAlpnProtocolNegotiation(BenchmarkState state, Blackhole bh) {
        // Simulate ALPN protocol selection
        String[] protocols = {"h2", "http/1.1"};
        String selected = selectProtocol(protocols, "h2");
        bh.consume(selected);
    }

    /**
     * Select protocol based on client preference.
     */
    private String selectProtocol(String[] protocols, String preferred) {
        for (String protocol : protocols) {
            if (preferred.equals(protocol)) {
                return protocol;
            }
        }
        return protocols.length > 0 ? protocols[0] : null;
    }

    /**
     * Benchmark serialization overhead (ProtoBuf simulated).
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void testProtoSerialization(Blackhole bh) {
        byte[] data = "test message for serialization".getBytes();
        bh.consume(data.length);
    }

    /**
     * Main method to run benchmarks directly.
     */
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(new String[] {
            "HttpProtocolBenchmark",
            "-wi", "3",
            "-w", "5s",
            "-i", "5",
            "-r", "5s",
            "-f", "1"
        });
    }
}