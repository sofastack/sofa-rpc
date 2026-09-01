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
package com.alipay.sofa.rpc.profile.jfr;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.profile.ProfileConstants;
import com.alipay.sofa.rpc.profile.ProfileFactory;
import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcClientEvent;
import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcServerEvent;
import jdk.jfr.Recording;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 */
public class JfrProfileTest {

    private final JfrProfile profile = new JfrProfile();

    @After
    public void after() {
        RpcInvokeContext.removeContext();
        RpcInternalContext.removeAllContext();
    }

    @Test
    public void loadFromSofaRpcSpi() {
        Assert.assertTrue(ProfileFactory.getProfile("jfr") instanceof JfrProfile);
    }

    @Test
    public void customEventsAreEnabledByDefault() throws Exception {
        Recording recording = new Recording();
        try {
            recording.start();
            Assert.assertTrue(profile.isEnabled());
        } finally {
            recording.close();
        }
    }

    @Test
    public void recordAndCorrelateClientAndServerInvocation() throws Exception {
        Recording recording = recording();
        Path file = Files.createTempFile("sofa-rpc-profile", ".jfr");
        try {
            recording.start();

            SofaRequest request = request();
            RpcInternalContext clientContext = RpcInternalContext.getContext();
            clientContext.setLocalAddress(InetSocketAddress.createUnresolved("127.0.0.1", 12000));
            clientContext.setRemoteAddress(InetSocketAddress.createUnresolved("127.0.0.1", 12200));
            clientContext.setAttachment(RpcConstants.INTERNAL_KEY_INVOKE_TIMES, 2);
            clientContext.setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, 64);
            clientContext.setAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE, 96);
            Assert.assertEquals("com.example.GreetingService:1.0", request.getTargetServiceUniqueName());
            profile.startRpc(request);
            String profileId = (String) request.getRequestProp(ProfileConstants.PROFILE_ID_KEY);
            Assert.assertNotNull(profileId);
            Assert.assertTrue(profile.isEnabled());

            profile.recordPhase(clientContext, RpcConstants.INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO, 10_000L);
            profile.recordPhase(clientContext, RpcConstants.INTERNAL_KEY_CLIENT_FILTER_TIME_NANO, 20_000L);
            profile.clientEnd(request, successResponse(), null);

            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
            RpcInternalContext serverContext = RpcInternalContext.getContext();
            serverContext.setLocalAddress(InetSocketAddress.createUnresolved("127.0.0.1", 12200));
            serverContext.setRemoteAddress(InetSocketAddress.createUnresolved("127.0.0.1", 12000));
            serverContext.setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, 80);
            serverContext.setAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE, 120);
            profile.recordPhase(serverContext, RpcConstants.INTERNAL_KEY_REQ_DESERIALIZE_TIME_NANO, 25_000L);
            profile.serverReceived(request);
            profile.recordPhase(serverContext, RpcConstants.INTERNAL_KEY_PROCESS_WAIT_TIME_NANO, 30_000L);
            profile.recordPhase(serverContext, RpcConstants.INTERNAL_KEY_IMPL_ELAPSE_NANO, 40_000L);
            profile.serverSend(request, successResponse(), null);

            recording.stop();
            recording.dump(file);

            List<EventSnapshot> events = rpcEvents(file);
            Assert.assertEquals(2, events.size());
            EventSnapshot client = find(events, JfrEventNames.CLIENT_INVOCATION);
            EventSnapshot server = find(events, JfrEventNames.SERVER_INVOCATION);
            Assert.assertEquals(profileId, client.value("profileId"));
            Assert.assertEquals(profileId, server.value("profileId"));
            Assert.assertEquals("com.example.GreetingService:1.0", client.value("service"));
            Assert.assertEquals("hello", client.value("method"));
            Assert.assertEquals("SUCCESS", client.value("result"));
            Assert.assertEquals(2, client.intValue("invokeCount"));
            Assert.assertEquals(64L, client.longValue("requestSize"));
            Assert.assertEquals(96L, client.longValue("responseSize"));
            Assert.assertEquals(10_000L, client.longValue("routerTime"));
            Assert.assertEquals(20_000L, client.longValue("clientFilterTime"));
            Assert.assertEquals(80L, server.longValue("requestSize"));
            Assert.assertEquals(120L, server.longValue("responseSize"));
            Assert.assertEquals(25_000L, server.longValue("requestDeserializationTime"));
            Assert.assertEquals(30_000L, server.longValue("threadPoolWaitTime"));
            Assert.assertEquals(40_000L, server.longValue("businessTime"));
            Assert.assertTrue(client.durationNanos >= 0L);
        } finally {
            recording.close();
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void commitClientEventOnCallbackThread() throws Exception {
        final Recording recording = recording();
        Path file = Files.createTempFile("sofa-rpc-profile-async", ".jfr");
        try {
            recording.start();
            final SofaRequest request = request("com.example.AsyncGreetingService:1.0");
            profile.startRpc(request);
            final RpcInternalContext callbackContext = RpcInternalContext.getContext();
            profile.recordPhase(callbackContext, RpcConstants.INTERNAL_KEY_CLIENT_BALANCER_TIME_NANO, 42_000L);
            final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

            Thread callback = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        RpcInternalContext.setContext(callbackContext);
                        profile.clientEnd(request, successResponse(), null);
                    } catch (Throwable e) {
                        failure.set(e);
                    } finally {
                        RpcInvokeContext.removeContext();
                        RpcInternalContext.removeAllContext();
                    }
                }
            }, "jfr-profile-test-callback");
            callback.start();
            callback.join();
            Assert.assertNull(failure.get());

            recording.stop();
            recording.dump(file);
            EventSnapshot event = find(rpcEvents(file), JfrEventNames.CLIENT_INVOCATION);
            Assert.assertEquals(42_000L, event.longValue("loadBalancerTime"));
            Assert.assertEquals("SUCCESS", event.value("result"));
        } finally {
            recording.close();
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void isolateOverlappingAsyncInvocationPhases() throws Exception {
        Recording recording = recording();
        Path file = Files.createTempFile("sofa-rpc-profile-concurrent-async", ".jfr");
        try {
            recording.start();

            SofaRequest firstRequest = request("com.example.FirstAsyncService:1.0");
            firstRequest.setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE);
            RpcInternalContext firstContext = RpcInternalContext.getContext();
            profile.startRpc(firstRequest);
            profile.recordPhase(firstContext, RpcConstants.INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO, 11_000L);

            RpcInternalContext.removeContext();
            SofaRequest secondRequest = request("com.example.SecondAsyncService:1.0");
            secondRequest.setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE);
            RpcInternalContext secondContext = RpcInternalContext.getContext();
            profile.startRpc(secondRequest);
            profile.recordPhase(secondContext, RpcConstants.INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO, 22_000L);

            // 两次调用共享同一个线程级RpcInvokeContext，写入冲突值不应污染调用级阶段数据。
            RpcInvokeContext.getContext().put(RpcConstants.INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO, 99_000L);
            profile.clientEnd(secondRequest, successResponse(), null);

            RpcInternalContext.removeContext();
            RpcInternalContext.setContext(firstContext);
            profile.clientEnd(firstRequest, successResponse(), null);

            recording.stop();
            recording.dump(file);
            List<EventSnapshot> events = rpcEvents(file);
            Assert.assertEquals(2, events.size());
            Assert.assertEquals(11_000L,
                findByService(events, "com.example.FirstAsyncService:1.0").longValue("routerTime"));
            Assert.assertEquals(22_000L,
                findByService(events, "com.example.SecondAsyncService:1.0").longValue("routerTime"));
        } finally {
            recording.close();
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void recordErrorWithoutSensitiveMessage() throws Exception {
        Recording recording = recording();
        Path file = Files.createTempFile("sofa-rpc-profile-error", ".jfr");
        try {
            recording.start();
            SofaRequest request = request("com.example.ErrorGreetingService:1.0");
            profile.startRpc(request);
            SofaRpcException error = new SofaRpcException(RpcErrorType.CLIENT_TIMEOUT,
                "password=must-not-be-recorded");
            profile.clientEnd(request, null, error);

            recording.stop();
            recording.dump(file);
            EventSnapshot event = find(rpcEvents(file), JfrEventNames.CLIENT_INVOCATION);
            Assert.assertEquals("EXCEPTION", event.value("result"));
            Assert.assertEquals(SofaRpcException.class.getName(), event.value("errorClass"));
            Assert.assertEquals(RpcErrorType.CLIENT_TIMEOUT, event.intValue("errorCode"));
            Assert.assertFalse(event.text.contains("must-not-be-recorded"));
            Assert.assertFalse(event.hasField("errorMessage"));
        } finally {
            recording.close();
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void finishServerEventAcrossRpcContexts() throws Exception {
        Recording recording = recording();
        Path file = Files.createTempFile("sofa-rpc-profile-server-async", ".jfr");
        try {
            recording.start();
            SofaRequest request = request("com.example.AsyncServerService:1.0");
            request.setMethodName(null);

            RpcInternalContext serverContext = RpcInternalContext.getContext();
            serverContext.setProviderSide(true);
            serverContext.setLocalAddress(InetSocketAddress.createUnresolved("127.0.0.1", 12200));
            serverContext.setRemoteAddress(InetSocketAddress.createUnresolved("127.0.0.1", 12000));
            serverContext.setAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE, 128);
            profile.serverReceived(request);
            profile.recordPhase(serverContext, RpcConstants.INTERNAL_KEY_PROCESS_WAIT_TIME_NANO, 30_000L);

            // 模拟Bolt异步代理在下游客户端回调上下文中发送上游响应。
            RpcInvokeContext.removeContext();
            RpcInternalContext.removeAllContext();
            RpcInternalContext callbackContext = RpcInternalContext.getContext();
            callbackContext.setProviderSide(false);
            callbackContext.setLocalAddress(InetSocketAddress.createUnresolved("127.0.0.1", 13000));
            callbackContext.setRemoteAddress(InetSocketAddress.createUnresolved("127.0.0.1", 13200));
            callbackContext.setAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE, 256);
            RpcInvokeContext.getContext().put(RpcConstants.INTERNAL_KEY_PROCESS_WAIT_TIME_NANO, 99_000L);
            request.setMethodName("asyncHello");
            profile.serverSend(request, successResponse(), null);

            recording.stop();
            recording.dump(file);
            EventSnapshot event = find(rpcEvents(file), JfrEventNames.SERVER_INVOCATION);
            Assert.assertEquals("asyncHello", event.value("method"));
            Assert.assertEquals("127.0.0.1:12200", event.value("localAddress"));
            Assert.assertEquals("127.0.0.1:12000", event.value("remoteAddress"));
            Assert.assertEquals(30_000L, event.longValue("threadPoolWaitTime"));
            Assert.assertEquals(128L, event.longValue("requestSize"));
            Assert.assertEquals(256L, event.longValue("responseSize"));
        } finally {
            recording.close();
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void replaceInvalidIncomingProfileId() throws Exception {
        Recording recording = recording();
        Path file = Files.createTempFile("sofa-rpc-profile-invalid-id", ".jfr");
        try {
            recording.start();
            SofaRequest request = request("com.example.InvalidProfileIdService:1.0");
            request.addRequestProp(ProfileConstants.PROFILE_ID_KEY, "invalid-external-profile-id");
            profile.serverReceived(request);
            profile.serverSend(request, successResponse(), null);

            recording.stop();
            recording.dump(file);
            EventSnapshot event = find(rpcEvents(file), JfrEventNames.SERVER_INVOCATION);
            String profileId = String.valueOf(event.value("profileId"));
            Assert.assertEquals(36, profileId.length());
            Assert.assertFalse("invalid-external-profile-id".equals(profileId));
        } finally {
            recording.close();
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void cleanupIncompleteState() throws Exception {
        Recording recording = recording();
        Path file = Files.createTempFile("sofa-rpc-profile-incomplete", ".jfr");
        try {
            recording.start();
            SofaRequest request = request("com.example.IncompleteGreetingService:1.0");
            profile.startRpc(request);
            profile.checkState();
            profile.checkState();

            recording.stop();
            recording.dump(file);
            List<EventSnapshot> events = rpcEvents(file);
            Assert.assertEquals(1, events.size());
            Assert.assertEquals("INCOMPLETE", events.get(0).value("result"));
        } finally {
            recording.close();
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void doNothingWithoutActiveRecording() {
        SofaRequest request = request();
        profile.startRpc(request);
        Assert.assertNull(request.getRequestProp(ProfileConstants.PROFILE_ID_KEY));
        Assert.assertFalse(profile.isEnabled());
    }

    private static Recording recording() {
        Recording recording = new Recording();
        recording.enable(SofaRpcClientEvent.class).withoutThreshold().withoutStackTrace();
        recording.enable(SofaRpcServerEvent.class).withoutThreshold().withoutStackTrace();
        return recording;
    }

    private static SofaRequest request() {
        return request("com.example.GreetingService:1.0");
    }

    private static SofaRequest request(String service) {
        // 不同Recording使用独立服务名，规避旧版JDK 8的JFR字符串池缺陷JDK-8257621。
        SofaRequest request = new SofaRequest();
        request.setTargetServiceUniqueName(service);
        request.setMethodName("hello");
        request.setTargetAppName("provider-app");
        request.setInvokeType(RpcConstants.INVOKER_TYPE_SYNC);
        request.addRequestProp(RemotingConstants.HEAD_APP_NAME, "consumer-app");
        request.addRequestProp(RemotingConstants.HEAD_PROTOCOL, "bolt");
        request.addRequestProp(RemotingConstants.HEAD_INVOKE_TYPE, RpcConstants.INVOKER_TYPE_SYNC);
        return request;
    }

    private static SofaResponse successResponse() {
        SofaResponse response = new SofaResponse();
        response.setAppResponse("ok");
        return response;
    }

    private static List<EventSnapshot> rpcEvents(Path file) throws Exception {
        List<EventSnapshot> result = new ArrayList<EventSnapshot>();
        RecordingFile recording = new RecordingFile(file);
        try {
            while (recording.hasMoreEvents()) {
                RecordedEvent event = recording.readEvent();
                String name = event.getEventType().getName();
                if (JfrEventNames.CLIENT_INVOCATION.equals(name) || JfrEventNames.SERVER_INVOCATION.equals(name)) {
                    result.add(new EventSnapshot(event));
                }
            }
        } finally {
            recording.close();
        }
        return result;
    }

    private static EventSnapshot find(List<EventSnapshot> events, String eventName) {
        for (EventSnapshot event : events) {
            if (eventName.equals(event.eventName)) {
                return event;
            }
        }
        Assert.fail("Missing event " + eventName);
        return null;
    }

    private static EventSnapshot findByService(List<EventSnapshot> events, String service) {
        for (EventSnapshot event : events) {
            if (service.equals(event.value("service"))) {
                return event;
            }
        }
        Assert.fail("Missing service " + service);
        return null;
    }

    /**
     * 部分JDK 8实现会惰性解析字符串常量池，因此在RecordingFile关闭前复制事件字段。
     */
    private static final class EventSnapshot {

        private final String              eventName;

        private final String              text;

        private final long                durationNanos;

        private final Map<String, Object> values = new HashMap<String, Object>();

        private EventSnapshot(RecordedEvent event) {
            eventName = event.getEventType().getName();
            text = event.toString();
            durationNanos = event.getDuration().toNanos();
            for (ValueDescriptor field : event.getFields()) {
                Object value = event.getValue(field.getName());
                values.put(field.getName(), value instanceof CharSequence ? value.toString() : value);
            }
        }

        private Object value(String field) {
            return values.get(field);
        }

        private int intValue(String field) {
            return ((Number) values.get(field)).intValue();
        }

        private long longValue(String field) {
            return ((Number) values.get(field)).longValue();
        }

        private boolean hasField(String field) {
            return values.containsKey(field);
        }
    }
}
