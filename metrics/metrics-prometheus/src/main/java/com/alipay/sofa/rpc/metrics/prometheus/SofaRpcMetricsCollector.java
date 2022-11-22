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

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.Event;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import com.alipay.sofa.rpc.event.ServerStartedEvent;
import com.alipay.sofa.rpc.event.ServerStoppedEvent;
import com.alipay.sofa.rpc.event.Subscriber;
import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

public class SofaRpcMetricsCollector extends Collector implements AutoCloseable {

    private static final String[] INVOKE_LABEL_NAMES = new String[]{"app", "service", "method", "protocol", "invoke_type", "caller_app"};

    private String[] commonLabelNames;
    private String[] commonLabelValues;

    private PrometheusSubscriber subscriber;

    private Histogram clientTotal;

    private Histogram clientFail;

    private Histogram serverTotal;

    private Histogram serverFail;

    private Histogram requestSize;

    private Histogram responseSize;

    private Counter providerCounter;

    private Counter consumerCounter;

    private Gauge threadPoolConfigCore;

    private Gauge threadPoolConfigMax;

    private Gauge threadPoolConfigQueue;

    private Gauge threadPoolActive;

    private Gauge threadPoolIdle;

    private Gauge threadPoolQueue;


    private final AtomicReference<ServerConfig> serverConfigReference = new AtomicReference<>();
    private final AtomicReference<ThreadPoolExecutor> executorReference = new AtomicReference<>();

    public SofaRpcMetricsCollector() {
        this(Collections.emptyMap(), MetricsBuilder.defaultOf());
    }

    public SofaRpcMetricsCollector(Map<String, String> commonLabels) {
        this(commonLabels, MetricsBuilder.defaultOf());
    }

    public SofaRpcMetricsCollector(MetricsBuilder metricsBuilder) {
        this(Collections.emptyMap(), metricsBuilder);
    }

    public SofaRpcMetricsCollector(Map<String, String> commonLabels, MetricsBuilder metricsBuilder) {
        this.commonLabelNames = commonLabels.keySet().toArray(new String[0]);
        this.commonLabelValues = commonLabels.values().toArray(new String[0]);
        this.subscriber = new PrometheusSubscriber();

        String[] labelNames;
        int clength = commonLabelNames.length;
        if (clength == 0) {
            labelNames = INVOKE_LABEL_NAMES;
        } else {
            int ilength = INVOKE_LABEL_NAMES.length;
            labelNames = new String[clength + ilength];
            System.arraycopy(commonLabelNames, 0, labelNames, 0, clength);
            System.arraycopy(INVOKE_LABEL_NAMES, 0, labelNames, clength, ilength);
        }

        this.clientTotal = metricsBuilder.buildClientTotal(labelNames);
        this.clientFail = metricsBuilder.buildClientFail(labelNames);
        this.serverTotal = metricsBuilder.buildServerTotal(labelNames);
        this.serverFail = metricsBuilder.buildServerFail(labelNames);
        this.requestSize = metricsBuilder.buildRequestSize(labelNames);
        this.responseSize = metricsBuilder.buildResponseSize(labelNames);

        this.providerCounter = metricsBuilder.buildProviderCounter(commonLabelNames);
        this.consumerCounter = metricsBuilder.buildConsumerCounter(commonLabelNames);
        this.threadPoolConfigCore = metricsBuilder.buildThreadPoolConfigCore(commonLabelNames);
        this.threadPoolConfigMax = metricsBuilder.buildThreadPoolConfigMax(commonLabelNames);
        this.threadPoolConfigQueue = metricsBuilder.buildThreadPoolConfigQueue(commonLabelNames);
        this.threadPoolActive = metricsBuilder.buildThreadPoolActive(commonLabelNames);
        this.threadPoolIdle = metricsBuilder.buildThreadPoolIdle(commonLabelNames);
        this.threadPoolQueue = metricsBuilder.buildThreadPoolQueue(commonLabelNames);

        registerSubscriber();
    }

    private void registerSubscriber() {
        EventBus.register(ClientEndInvokeEvent.class, subscriber);
        EventBus.register(ServerSendEvent.class, subscriber);
        EventBus.register(ServerStartedEvent.class, subscriber);
        EventBus.register(ServerStoppedEvent.class, subscriber);
        EventBus.register(ProviderPubEvent.class, subscriber);
        EventBus.register(ConsumerSubEvent.class, subscriber);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> result = new ArrayList<>();
        result.addAll(clientTotal.collect());
        result.addAll(clientFail.collect());
        result.addAll(serverTotal.collect());
        result.addAll(serverFail.collect());
        result.addAll(requestSize.collect());
        result.addAll(responseSize.collect());
        result.addAll(providerCounter.collect());
        result.addAll(consumerCounter.collect());

        ServerConfig serverConfig = serverConfigReference.get();
        ThreadPoolExecutor threadPoolExecutor = executorReference.get();
        if (serverConfig != null) {
            threadPoolConfigCore.labels(commonLabelValues)
                    .set(serverConfig.getCoreThreads());
            result.addAll(threadPoolConfigCore.collect());


            threadPoolConfigMax.labels(commonLabelValues)
                    .set(serverConfig.getMaxThreads());
            result.addAll(threadPoolConfigMax.collect());


            threadPoolConfigQueue.labels(commonLabelValues)
                    .set(serverConfig.getQueues());
            result.addAll(threadPoolConfigQueue.collect());
        }


        if (threadPoolExecutor != null) {
            threadPoolActive.labels(commonLabelValues)
                    .set(threadPoolExecutor.getActiveCount());
            result.addAll(threadPoolActive.collect());

            threadPoolIdle.labels(commonLabelValues)
                    .set(threadPoolExecutor.getPoolSize() - threadPoolExecutor.getActiveCount());
            result.addAll(threadPoolIdle.collect());


            threadPoolQueue.labels(commonLabelValues)
                    .set(threadPoolExecutor.getQueue().size());
            result.addAll(threadPoolQueue.collect());
        }

        return result;
    }

    @Override
    public void close() throws Exception {
        EventBus.unRegister(ClientEndInvokeEvent.class, subscriber);
        EventBus.unRegister(ServerSendEvent.class, subscriber);
        EventBus.unRegister(ServerStartedEvent.class, subscriber);
        EventBus.unRegister(ServerStoppedEvent.class, subscriber);
        EventBus.unRegister(ProviderPubEvent.class, subscriber);
        EventBus.unRegister(ConsumerSubEvent.class, subscriber);
    }

    private static Long getLongAvoidNull(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof Integer) {
            return Long.parseLong(object.toString());
        }

        return (Long) object;
    }

    private static String getStringAvoidNull(Object object) {
        if (object == null) {
            return null;
        }

        return (String) object;
    }


    private class PrometheusSubscriber extends Subscriber {

        @Override
        public void onEvent(Event event) {
            if (event instanceof ClientEndInvokeEvent) {
                onEvent((ClientEndInvokeEvent) event);
            } else if (event instanceof ServerSendEvent) {
                onEvent((ServerSendEvent) event);
            } else if (event instanceof ServerStartedEvent) {
                onEvent((ServerStartedEvent) event);
            } else if (event instanceof ServerStoppedEvent) {
                onEvent((ServerStoppedEvent) event);
            } else if (event instanceof ProviderPubEvent) {
                onEvent((ProviderPubEvent) event);
            } else if (event instanceof ConsumerSubEvent) {
                onEvent((ConsumerSubEvent) event);
            } else {
                throw new IllegalArgumentException("unexpected event: " + event);
            }
        }

        private void onEvent(ClientEndInvokeEvent event) {
            InvokeMeta meta = new InvokeMeta(
                    event.getRequest(),
                    event.getResponse(),
                    getLongAvoidNull(RpcInternalContext.getContext().getAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE))
            );
            long elapsed = meta.elapsed();
            String[] labelValues = meta.labelValues(commonLabelValues);

            clientTotal.labels(labelValues).observe(elapsed);
            if (!meta.success()) {
                clientFail.labels(labelValues).observe(elapsed);
            }

            RpcInternalContext context = RpcInternalContext.getContext();
            requestSize.labels(labelValues)
                    .observe(getLongAvoidNull(context.getAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE)));
            responseSize.labels(labelValues)
                    .observe(getLongAvoidNull(context.getAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE)));
        }

        private void onEvent(ServerSendEvent event) {
            InvokeMeta meta = new InvokeMeta(
                    event.getRequest(),
                    event.getResponse(),
                    getLongAvoidNull(RpcInternalContext.getContext().getAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE))
            );
            long elapsed = meta.elapsed();
            String[] labelValues = meta.labelValues(commonLabelValues);

            serverTotal.labels(labelValues).observe(elapsed);
            if (!meta.success()) {
                serverFail.labels(labelValues).observe(elapsed);
            }
        }

        private void onEvent(ServerStartedEvent event) {
            serverConfigReference.set(event.getServerConfig());
            executorReference.set(event.getThreadPoolExecutor());
        }

        private void onEvent(ServerStoppedEvent event) {
            serverConfigReference.set(null);
            executorReference.set(null);
        }

        private void onEvent(ProviderPubEvent event) {
            providerCounter.labels(commonLabelValues)
                    .inc();
        }

        private void onEvent(ConsumerSubEvent event) {
            consumerCounter.labels(commonLabelValues)
                    .inc();
        }
    }

    private static class InvokeMeta {

        private final SofaRequest request;
        private final SofaResponse response;
        private final long elapsed;

        private InvokeMeta(SofaRequest request, SofaResponse response, long elapsed) {
            this.request = request;
            this.response = response;
            this.elapsed = elapsed;
        }

        public String app() {
            return Optional.ofNullable(request.getTargetAppName()).orElse("");
        }

        public String callerApp() {
            return Optional.ofNullable(getStringAvoidNull(
                    request.getRequestProp(RemotingConstants.HEAD_APP_NAME))).orElse("");
        }

        public String service() {
            return Optional.ofNullable(request.getTargetServiceUniqueName()).orElse("");
        }

        public String method() {
            return Optional.ofNullable(request.getMethodName()).orElse("");
        }

        public String protocol() {
            return Optional.ofNullable(getStringAvoidNull(
                    request.getRequestProp(RemotingConstants.HEAD_PROTOCOL))).orElse("");
        }

        public String invokeType() {
            return Optional.ofNullable(request.getInvokeType()).orElse("");
        }

        public long elapsed() {
            return elapsed;
        }

        public boolean success() {
            return response != null
                    && !response.isError()
                    && response.getErrorMsg() == null
                    && (!(response.getAppResponse() instanceof Throwable));
        }

        public String[] labelValues(String[] commonLabelValues) {
            String[] labelValues;
            String[] invokeLabelValues = new String[]{app(), service(), method(), protocol(), invokeType(), callerApp()};
            int clength = commonLabelValues.length;
            if (clength == 0) {
                labelValues = invokeLabelValues;
            } else {
                int ilength = invokeLabelValues.length;
                labelValues = new String[clength + ilength];
                System.arraycopy(commonLabelValues, 0, labelValues, 0, clength);
                System.arraycopy(invokeLabelValues, 0, labelValues, clength, ilength);
            }
            return labelValues;
        }
    }

}
