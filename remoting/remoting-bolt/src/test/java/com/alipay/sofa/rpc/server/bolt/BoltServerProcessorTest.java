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
package com.alipay.sofa.rpc.server.bolt;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.InvokeContext;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import com.alipay.sofa.rpc.event.Event;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerReceiveEvent;
import com.alipay.sofa.rpc.event.Subscriber;
import com.alipay.sofa.rpc.message.bolt.BoltSendableResponseCallback;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 */
public class BoltServerProcessorTest {

    @After
    public void after() {
        RpcInvokeContext.removeContext();
        RpcInternalContext.removeAllContext();
    }

    @Test
    public void retainRequestForAsyncResponseCallback() {
        BoltServer server = Mockito.mock(BoltServer.class);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(12200);
        server.serverConfig = serverConfig;
        Mockito.when(server.getBizExecutor()).thenReturn(Mockito.mock(Executor.class));
        Mockito.when(server.isStarted()).thenReturn(false);

        BizContext bizContext = Mockito.mock(BizContext.class);
        Mockito.when(bizContext.getRemoteHost()).thenReturn("127.0.0.1");
        Mockito.when(bizContext.getRemotePort()).thenReturn(12000);
        Mockito.when(bizContext.getInvokeContext()).thenReturn(new InvokeContext());
        AsyncContext asyncContext = Mockito.mock(AsyncContext.class);

        final SofaRequest request = new SofaRequest();
        request.setTargetServiceUniqueName("com.example.GreetingService:1.0");
        final AtomicReference<SofaRequest> retainedRequest = new AtomicReference<SofaRequest>();
        final AtomicReference<BoltSendableResponseCallback<Object>> callback =
                new AtomicReference<BoltSendableResponseCallback<Object>>();
        Subscriber subscriber = new Subscriber() {
            @Override
            public void onEvent(Event event) {
                if (event.getClass() == ServerReceiveEvent.class) {
                    RpcInvokeContext.getContext().put(RemotingConstants.INVOKE_CTX_IS_ASYNC_CHAIN, Boolean.TRUE);
                    callback.set(new BoltSendableResponseCallback<Object>() {
                        @Override
                        public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                            sendAppResponse(appResponse);
                        }
                    });
                } else if (event.getClass() == ServerSendEvent.class) {
                    ServerSendEvent sendEvent = (ServerSendEvent) event;
                    if ("async-result".equals(sendEvent.getResponse().getAppResponse())) {
                        retainedRequest.set(sendEvent.getRequest());
                    }
                }
            }
        };

        EventBus.register(ServerReceiveEvent.class, subscriber);
        EventBus.register(ServerSendEvent.class, subscriber);
        try {
            new BoltServerProcessor(server).handleRequest(bizContext, asyncContext, request);
            Assert.assertNotNull(callback.get());
            callback.get().sendAppResponse("async-result");
            Assert.assertSame(request, retainedRequest.get());
            Mockito.verify(asyncContext).sendResponse(Mockito.any());
        } finally {
            EventBus.unRegister(ServerReceiveEvent.class, subscriber);
            EventBus.unRegister(ServerSendEvent.class, subscriber);
        }
    }
}
