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
import com.alipay.sofa.rpc.common.utils.ReflectUtils;
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
import com.alipay.sofa.rpc.event.Event;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import com.alipay.sofa.rpc.event.ServerStartedEvent;
import com.alipay.sofa.rpc.event.ServerStoppedEvent;
import com.alipay.sofa.rpc.event.Subscriber;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class SofaRpcMetricsTest {

    @Test
    public void testMicrometerMetrics() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try (SofaRpcMetrics metrics = new SofaRpcMetrics()) {
            metrics.bindTo(registry);

            Method handleEvent = EventBus.class.getDeclaredMethod(
                "handleEvent", Subscriber.class, Event.class);
            handleEvent.setAccessible(true);
            SofaRequest request = buildRequest();
            SofaResponse response = buildResponse();
            RpcInternalContext.getContext()
                .setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE, 100)
                .setAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE, 10)
                .setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, 3)
                .setAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE, 4);

            handleEvent.invoke(EventBus.class, metrics, new ClientEndInvokeEvent(request, response, null));
            handleEvent.invoke(EventBus.class, metrics, new ServerSendEvent(request, response, null));
            ServerConfig serverConfig = new ServerConfig();
            handleEvent.invoke(EventBus.class, metrics, new ServerStartedEvent(serverConfig, new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>())));
            handleEvent.invoke(EventBus.class, metrics, new ServerStoppedEvent(serverConfig));
            handleEvent.invoke(EventBus.class, metrics, new ProviderPubEvent(new ProviderConfig<>()));
            handleEvent.invoke(EventBus.class, metrics, new ConsumerSubEvent(new ConsumerConfig<>()));

            Assert.assertEquals(12, registry.getMeters().size());
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

    private SofaResponse buildResponse() {
        SofaResponse response = new SofaResponse();
        response.setAppResponse("123");
        return response;
    }

    private static class TestService {

        public String func() {
            return null;
        }
    }
}