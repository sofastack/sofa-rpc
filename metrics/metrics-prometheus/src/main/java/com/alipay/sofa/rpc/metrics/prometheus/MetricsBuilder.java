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
package com.alipay.sofa.rpc.metrics.prometheus;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

public class MetricsBuilder {
    public static final String BYTES                        = "bytes";

    public static final String TASKS                        = "tasks";

    public static final String THREADS                      = "threads";

    private Histogram.Builder  clientTotalBuilder           = Histogram.build();

    private Histogram.Builder  clientFailBuilder            = Histogram.build();

    private Histogram.Builder  serverTotalBuilder           = Histogram.build();

    private Histogram.Builder  serverFailBuilder            = Histogram.build();

    private Histogram.Builder  requestSizeBuilder           = Histogram.build();

    private Histogram.Builder  responseSizeBuilder          = Histogram.build();

    private Counter.Builder    providerCounterBuilder       = Counter.build();

    private Counter.Builder    consumerCounterBuilder       = Counter.build();

    private Gauge.Builder      threadPoolConfigCoreBuilder  = Gauge.build();

    private Gauge.Builder      threadPoolConfigMaxBuilder   = Gauge.build();

    private Gauge.Builder      threadPoolConfigQueueBuilder = Gauge.build();

    private Gauge.Builder      threadPoolActiveBuilder      = Gauge.build();

    private Gauge.Builder      threadPoolIdleBuilder        = Gauge.build();

    private Gauge.Builder      threadPoolQueueBuilder       = Gauge.build();

    public Histogram.Builder getClientTotalBuilder() {
        return clientTotalBuilder;
    }

    public Histogram.Builder getClientFailBuilder() {
        return clientFailBuilder;
    }

    public Histogram.Builder getServerTotalBuilder() {
        return serverTotalBuilder;
    }

    public Histogram.Builder getServerFailBuilder() {
        return serverFailBuilder;
    }

    public Histogram.Builder getRequestSizeBuilder() {
        return requestSizeBuilder;
    }

    public Histogram.Builder getResponseSizeBuilder() {
        return responseSizeBuilder;
    }

    public Counter.Builder getProviderCounterBuilder() {
        return providerCounterBuilder;
    }

    public Counter.Builder getConsumerCounterBuilder() {
        return consumerCounterBuilder;
    }

    public Gauge.Builder getThreadPoolConfigCoreBuilder() {
        return threadPoolConfigCoreBuilder;
    }

    public Gauge.Builder getThreadPoolConfigMaxBuilder() {
        return threadPoolConfigMaxBuilder;
    }

    public Gauge.Builder getThreadPoolConfigQueueBuilder() {
        return threadPoolConfigQueueBuilder;
    }

    public Gauge.Builder getThreadPoolActiveBuilder() {
        return threadPoolActiveBuilder;
    }

    public Gauge.Builder getThreadPoolIdleBuilder() {
        return threadPoolIdleBuilder;
    }

    public Gauge.Builder getThreadPoolQueueBuilder() {
        return threadPoolQueueBuilder;
    }

    Histogram buildClientTotal(String[] labelNames) {
        return clientTotalBuilder
            .name("sofa_client_total")
            .help("sofa_client_total")
            .labelNames(labelNames)
            .create();
    }

    Histogram buildClientFail(String[] labelNames) {
        return clientFailBuilder
            .name("sofa_client_fail")
            .help("sofa_client_fail")
            .labelNames(labelNames)
            .create();
    }

    Histogram buildServerTotal(String[] labelNames) {
        return serverTotalBuilder
            .name("sofa_server_total")
            .help("sofa_server_total")
            .labelNames(labelNames)
            .create();
    }

    Histogram buildServerFail(String[] labelNames) {
        return serverFailBuilder
            .name("sofa_server_fail")
            .help("sofa_server_fail")
            .labelNames(labelNames)
            .create();
    }

    Histogram buildRequestSize(String[] labelNames) {
        return requestSizeBuilder
            .name("sofa_request_size")
            .help("sofa_request_size")
            .unit(BYTES)
            .labelNames(labelNames)
            .create();
    }

    Histogram buildResponseSize(String[] labelNames) {
        return responseSizeBuilder
            .name("sofa_response_size")
            .help("sofa_response_size")
            .unit(BYTES)
            .labelNames(labelNames)
            .create();
    }

    Counter buildProviderCounter(String[] labelNames) {
        return providerCounterBuilder
            .name("sofa_provider")
            .help("sofa_provider")
            .labelNames(labelNames)
            .create();
    }

    Counter buildConsumerCounter(String[] labelNames) {
        return consumerCounterBuilder
            .name("sofa_consumer")
            .help("sofa_consumer")
            .labelNames(labelNames)
            .create();
    }

    Gauge buildThreadPoolConfigCore(String[] labelNames) {
        return threadPoolConfigCoreBuilder
            .name("sofa_threadpool_config_core")
            .help("sofa_threadpool_config_core")
            .unit(THREADS)
            .labelNames(labelNames)
            .create();
    }

    Gauge buildThreadPoolConfigMax(String[] labelNames) {
        return threadPoolConfigMaxBuilder
            .name("sofa_threadpool_config_max")
            .help("sofa_threadpool_config_max")
            .unit(THREADS)
            .labelNames(labelNames)
            .create();
    }

    Gauge buildThreadPoolConfigQueue(String[] labelNames) {
        return threadPoolConfigQueueBuilder
            .name("sofa_threadpool_config_queue")
            .help("sofa_threadpool_config_queue")
            .unit(TASKS)
            .labelNames(labelNames)
            .create();
    }

    Gauge buildThreadPoolActive(String[] labelNames) {
        return threadPoolActiveBuilder
            .name("sofa_threadpool_active")
            .help("sofa_threadpool_active")
            .unit(THREADS)
            .labelNames(labelNames)
            .create();
    }

    Gauge buildThreadPoolIdle(String[] labelNames) {
        return threadPoolIdleBuilder
            .name("sofa_threadpool_idle")
            .help("sofa_threadpool_idle")
            .unit(THREADS)
            .labelNames(labelNames)
            .create();
    }

    Gauge buildThreadPoolQueue(String[] labelNames) {
        return threadPoolQueueBuilder
            .name("sofa_threadpool_queue_size")
            .help("sofa_threadpool_queue_size")
            .unit(TASKS)
            .labelNames(labelNames)
            .create();
    }

    public static MetricsBuilder defaultOf() {
        return new MetricsBuilder();
    }

}
