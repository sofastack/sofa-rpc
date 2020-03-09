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
package com.alipay.sofa.rpc.metrics.micrometer;

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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author hujia
 * @date 2020/3/2
 */
public class SofaRpcMetrics extends Subscriber implements MeterBinder, AutoCloseable {

    private final AtomicReference<MeterRegistry> initialed = new AtomicReference<>();

    private final Function<Tags, Timer> clientTotal = tags -> Timer.builder("sofa.client.total")
        .tags(tags)
        .register(initialed.get());
    private final Function<Tags, Timer> clientFail = tags -> Timer.builder("sofa.client.fail")
        .tags(tags)
        .register(initialed.get());
    private final Function<Tags, Timer> serverTotal = tags -> Timer.builder("sofa.server.total")
        .tags(tags)
        .register(initialed.get());
    private final Function<Tags, Timer> serverFail = tags -> Timer.builder("sofa.server.fail")
        .tags(tags)
        .register(initialed.get());
    private final Function<Tags, DistributionSummary> requestSize = tags -> DistributionSummary.builder("sofa.request.size")
        .tags(tags)
        .baseUnit(BaseUnits.BYTES)
        .register(initialed.get());
    private final Function<Tags, DistributionSummary> responseSize = tags -> DistributionSummary.builder("sofa.response.size")
        .tags(tags)
        .baseUnit(BaseUnits.BYTES)
        .register(initialed.get());
    private Counter provider;
    private Counter consumer;

    private final Tags common;

    private final AtomicReference<ServerConfig> serverConfig = new AtomicReference<>();
    private final AtomicReference<ThreadPoolExecutor> executor = new AtomicReference<>();

    public SofaRpcMetrics() {
        this(Collections.emptyList());
    }

    public SofaRpcMetrics(Iterable<Tag> common) {
        this.common = Tags.of(common);
        register();
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("sofa.threadpool.config.core", () -> Optional.of(serverConfig)
            .map(AtomicReference::get)
            .map(ServerConfig::getCoreThreads)
            .orElse(0))
            .tags(common)
            .baseUnit(BaseUnits.THREADS)
            .register(registry);
        Gauge.builder("sofa.threadpool.config.max", () -> Optional.of(serverConfig)
            .map(AtomicReference::get)
            .map(ServerConfig::getMaxThreads)
            .orElse(0))
            .tags(common)
            .baseUnit(BaseUnits.THREADS)
            .register(registry);
        Gauge.builder("sofa.threadpool.config.queue", () -> Optional.of(serverConfig)
            .map(AtomicReference::get)
            .map(ServerConfig::getQueues)
            .orElse(0))
            .tags(common)
            .baseUnit(BaseUnits.TASKS)
            .register(registry);
        Gauge.builder("sofa.threadpool.active", () -> Optional.of(executor)
            .map(AtomicReference::get)
            .map(ThreadPoolExecutor::getActiveCount)
            .orElse(0))
            .tags(common)
            .baseUnit(BaseUnits.THREADS)
            .register(registry);
        Gauge.builder("sofa.threadpool.idle", () -> Optional.of(executor)
            .map(AtomicReference::get)
            .map(e -> e.getPoolSize() - e.getActiveCount())
            .orElse(0))
            .tags(common)
            .baseUnit(BaseUnits.THREADS)
            .register(registry);
        Gauge.builder("sofa.threadpool.queue.size", () -> Optional.of(executor)
            .map(AtomicReference::get)
            .map(ThreadPoolExecutor::getQueue)
            .map(Collection::size)
            .orElse(0))
            .tags(common)
            .baseUnit(BaseUnits.TASKS)
            .register(registry);
        provider = Counter.builder("sofa.provider")
            .tags(common)
            .register(registry);
        consumer = Counter.builder("sofa.consumer")
            .tags(common)
            .register(registry);

        initialed.set(registry);
    }

    private void register() {
        EventBus.register(ClientEndInvokeEvent.class, this);
        EventBus.register(ServerSendEvent.class, this);
        EventBus.register(ServerStartedEvent.class, this);
        EventBus.register(ServerStoppedEvent.class, this);
        EventBus.register(ProviderPubEvent.class, this);
        EventBus.register(ConsumerSubEvent.class, this);
    }

    @Override
    public void onEvent(Event event) {
        if (initialed.get() != null) {
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
    }

    private void onEvent(ClientEndInvokeEvent event) {
        InvokeMeta meta = new InvokeMeta(
            event.getRequest(),
            event.getResponse(),
            getLongAvoidNull(RpcInternalContext.getContext().getAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE))
        );
        RpcInternalContext context = RpcInternalContext.getContext();
        Duration elapsed = meta.elapsed();
        Tags tags = meta.tags(this.common);

        clientTotal.apply(tags).record(elapsed);
        if (!meta.success()) {
            clientFail.apply(tags).record(elapsed);
        }
        requestSize.apply(tags).record(getLongAvoidNull(
            context.getAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE)));
        responseSize.apply(tags).record(getLongAvoidNull(
            context.getAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE)));
    }

    private void onEvent(ServerSendEvent event) {
        InvokeMeta meta = new InvokeMeta(
            event.getRequest(),
            event.getResponse(),
            getLongAvoidNull(RpcInternalContext.getContext().getAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE))
        );
        Duration elapsed = meta.elapsed();
        Tags tags = meta.tags(this.common);
        serverTotal.apply(tags).record(elapsed);
        if (!meta.success()) {
            serverFail.apply(tags).record(elapsed);
        }
    }

    private void onEvent(ServerStartedEvent event) {
        this.serverConfig.set(event.getServerConfig());
        this.executor.set(event.getThreadPoolExecutor());
    }

    private void onEvent(ServerStoppedEvent event) {
        serverConfig.set(null);
        executor.set(null);
    }

    private void onEvent(ProviderPubEvent event) {
        provider.increment();
    }

    private void onEvent(ConsumerSubEvent event) {
        consumer.increment();
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

    @Override
    public void close() {
        EventBus.unRegister(ClientEndInvokeEvent.class, this);
        EventBus.unRegister(ServerSendEvent.class, this);
        EventBus.unRegister(ServerStartedEvent.class, this);
        EventBus.unRegister(ServerStoppedEvent.class, this);
        EventBus.unRegister(ProviderPubEvent.class, this);
        EventBus.unRegister(ConsumerSubEvent.class, this);
    }

    private static class InvokeMeta {

        private final SofaRequest request;
        private final SofaResponse response;
        private final Duration elapsed;

        private InvokeMeta(SofaRequest request, SofaResponse response, long elapsed) {
            this.request = request;
            this.response = response;
            this.elapsed = Duration.ofMillis(elapsed);
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

        public Duration elapsed() {
            return elapsed;
        }

        public boolean success() {
            return response != null
                && !response.isError()
                && response.getErrorMsg() == null
                && (!(response.getAppResponse() instanceof Throwable));
        }

        public Tags tags(Iterable<Tag> common) {
            return Tags.of(common).and(
                Tag.of("app", app()),
                Tag.of("service", service()),
                Tag.of("method", method()),
                Tag.of("protocol", protocol()),
                Tag.of("invoke_type", invokeType()),
                Tag.of("caller_app", callerApp())
            );
        }
    }
}
