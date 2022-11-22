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
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientEndInvokeEvent;
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import com.alipay.sofa.rpc.event.ServerStartedEvent;
import com.alipay.sofa.rpc.event.ServerStoppedEvent;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SofaRpcMetricsCollectorTest {

    @Test
    public void testPrometheusMetricsCollect1() throws Exception {
        try (SofaRpcMetricsCollector collector = new SofaRpcMetricsCollector()) {
            CollectorRegistry registry = new CollectorRegistry();
            collector.register(registry);

            SofaRequest request = buildRequest();
            SofaResponse successResponse = buildSuccessResponse();
            SofaResponse failResponse = buildFailResponse();
            RpcInternalContext.getContext()
                    .setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE, 100)
                    .setAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE, 10)
                    .setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, 3)
                    .setAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE, 4);

            List<Collector.MetricFamilySamples> samplesList;

            EventBus.post(new ClientEndInvokeEvent(request, successResponse, null));
            EventBus.post(new ClientEndInvokeEvent(request, failResponse, null));

            EventBus.post(new ServerSendEvent(request, successResponse, null));
            EventBus.post(new ServerSendEvent(request, failResponse, null));

            EventBus.post(new ProviderPubEvent(new ProviderConfig<>()));

            EventBus.post(new ConsumerSubEvent(new ConsumerConfig<>()));
            samplesList = collector.collect();
            Assert.assertEquals(samplesList.size(), 8);

            ServerConfig serverConfig = new ServerConfig();
            EventBus.post(new ServerStartedEvent(serverConfig, new ThreadPoolExecutor(1, 1,
                    0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>())));
            samplesList = collector.collect();
            Assert.assertEquals(samplesList.size(), 14);

            EventBus.post(new ServerStoppedEvent(serverConfig));
            samplesList = collector.collect();
            Assert.assertEquals(samplesList.size(), 8);

//            new HTTPServer(new InetSocketAddress(9000),registry);
//            Thread.currentThread().join();
        }
    }

    @Test
    public void testPrometheusMetricsCollect2() throws Exception {
        MetricsBuilder metricsBuilder = new MetricsBuilder();
        // set buckets
        metricsBuilder.getClientTotalBuilder()
                .exponentialBuckets(1, 2, 15);
        metricsBuilder.getClientFailBuilder()
                .linearBuckets(0, 5, 15);

        Map<String, String> testLabels = new HashMap<>();
        testLabels.put("from", "test");

        try (SofaRpcMetricsCollector collector = new SofaRpcMetricsCollector(testLabels, metricsBuilder)) {
            CollectorRegistry registry = new CollectorRegistry();
            collector.register(registry);

            SofaRequest request = buildRequest();
            SofaResponse successResponse = buildSuccessResponse();
            SofaResponse failResponse = buildFailResponse();
            RpcInternalContext.getContext()
                    .setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE, 100)
                    .setAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE, 10)
                    .setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, 3)
                    .setAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE, 4);

            List<Collector.MetricFamilySamples> samplesList;

            EventBus.post(new ClientEndInvokeEvent(request, successResponse, null));
            EventBus.post(new ClientEndInvokeEvent(request, failResponse, null));

            EventBus.post(new ProviderPubEvent(new ProviderConfig<>()));

            EventBus.post(new ConsumerSubEvent(new ConsumerConfig<>()));

            ServerConfig serverConfig = new ServerConfig();
            EventBus.post(new ServerStartedEvent(serverConfig, new ThreadPoolExecutor(1, 1,
                    0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>())));

            samplesList = collector.collect();
            Assert.assertEquals(samplesList.size(), 14);

//            new HTTPServer(new InetSocketAddress(9000),registry);
//            Thread.currentThread().join();
        }
    }

    private SofaRequest buildRequest() throws NoSuchMethodException {
        SofaRequest request = new SofaRequest();
        request.setInterfaceName(TestService.class.getName());
        request.setMethodName("echoStr");
        request.setMethod(TestService.class.getMethod("func"));
        request.setMethodArgs(new Object[] {});
        request.setMethodArgSigs(new String[] {});
        request.setTargetServiceUniqueName(TestService.class.getName() + ":1.0");
        request.setTargetAppName("targetApp");
        request.setSerializeType((byte) 11);
        request.setTimeout(1024);
        request.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
        request.addRequestProp(RemotingConstants.HEAD_APP_NAME, "app");
        request.addRequestProp(RemotingConstants.HEAD_PROTOCOL, "bolt");
        request.setSofaResponseCallback(new SofaResponseCallback<String>() {
            @Override
            public void onAppResponse(Object appResponse, String methodName, RequestBase request) {

            }

            @Override
            public void onAppException(Throwable throwable, String methodName, RequestBase request) {

            }

            @Override
            public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {

            }
        });
        return request;
    }

    private SofaResponse buildSuccessResponse() {
        SofaResponse response = new SofaResponse();
        response.setAppResponse("123");
        return response;
    }

    private SofaResponse buildFailResponse() {
        SofaResponse response = new SofaResponse();
        response.setAppResponse(new RuntimeException());
        return response;
    }

    private static class TestService {

        public String func() {
            return null;
        }
    }
}
