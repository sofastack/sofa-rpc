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
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.profile.Profile;
import com.alipay.sofa.rpc.profile.ProfileConstants;
import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcClientEvent;
import com.alipay.sofa.rpc.profile.jfr.event.SofaRpcServerEvent;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Java Flight Recorder的RPC调用过程采集实现。
 *
 * @author <a href="mailto:13622993145@163.com">weilhaung</a>
 * @since 5.14.4
 */
@Extension("jfr")
public class JfrProfile extends Profile {

    private static final String    CLIENT_STATE_KEY  = ".sofa_rpc_jfr_client_state";

    private static final String    SERVER_STATE_KEY  = ".sofa_rpc_jfr_server_state";

    private static final String    PHASE_STATE_KEY   = ".sofa_rpc_jfr_phase_state";

    private static final EventType CLIENT_EVENT_TYPE = EventType
                                                         .getEventType(SofaRpcClientEvent.class);

    private static final EventType SERVER_EVENT_TYPE = EventType
                                                         .getEventType(SofaRpcServerEvent.class);

    @Override
    public boolean isEnabled() {
        return FlightRecorder.isAvailable() && (CLIENT_EVENT_TYPE.isEnabled() || SERVER_EVENT_TYPE.isEnabled());
    }

    @Override
    public void startRpc(SofaRequest request) {
        if (!CLIENT_EVENT_TYPE.isEnabled() || request == null) {
            return;
        }

        RpcInternalContext internalContext = RpcInternalContext.getContext();
        PhaseDurations phases = replacePhaseDurations(internalContext);
        SofaRpcClientEvent event = new SofaRpcClientEvent();
        event.begin();
        event.profileId = newProfileId();
        request.addRequestProp(ProfileConstants.PROFILE_ID_KEY, event.profileId);
        fillClientRequest(event, request, internalContext);
        internalContext.setAttachment(CLIENT_STATE_KEY, new ClientState(event, phases, internalContext));
    }

    @Override
    public void clientBeforeSend(SofaRequest request) {
        ClientState state = clientState();
        if (state != null) {
            RpcInternalContext context = RpcInternalContext.getContext();
            fillAddresses(state.event, context);
            updateSize(state.event, request, null, context, state.internalContext);
        }
    }

    @Override
    public void clientAfterSend(SofaRequest request) {
        ClientState state = clientState();
        if (state != null) {
            RpcInternalContext context = RpcInternalContext.getContext();
            fillAddresses(state.event, context);
            updateSize(state.event, request, null, context, state.internalContext);
        }
    }

    @Override
    public void clientReceived(SofaRequest request, SofaResponse response, Throwable throwable) {
        ClientState state = clientState();
        if (state != null) {
            updateSize(state.event, request, response, RpcInternalContext.getContext(), state.internalContext);
        }
    }

    @Override
    public void clientEnd(SofaRequest request, SofaResponse response, Throwable throwable) {
        RpcInternalContext context = RpcInternalContext.getContext();
        ClientState state = (ClientState) context.removeAttachment(CLIENT_STATE_KEY);
        if (state == null) {
            return;
        }

        SofaRpcClientEvent event = state.event;
        fillClientRequest(event, request, context);
        fillClientPhases(event, state.phases);
        fillResult(event, response, throwable);
        updateSize(event, request, response, context, state.internalContext);
        removePhaseDurations(state.internalContext, state.phases);
        event.end();
        if (event.shouldCommit()) {
            event.commit();
        }
    }

    @Override
    public void serverReceived(SofaRequest request) {
        if (!SERVER_EVENT_TYPE.isEnabled()) {
            return;
        }

        RpcInternalContext internalContext = RpcInternalContext.getContext();
        SofaRpcServerEvent event = new SofaRpcServerEvent();
        event.begin();
        event.profileId = profileId(request);
        fillServerRequest(event, request, internalContext);
        updateSize(event, request, null, internalContext, internalContext);
        ServerState state = new ServerState(event, phaseDurations(internalContext), internalContext, request);
        internalContext.setAttachment(SERVER_STATE_KEY, state);
        if (request != null) {
            // 异步回包可能切换RpcInternalContext，请求对象会随回调保留，可作为本地关联载体。
            request.addRequestProp(SERVER_STATE_KEY, state);
        }
    }

    @Override
    public void serverSend(SofaRequest request, SofaResponse response, Throwable throwable) {
        RpcInternalContext context = RpcInternalContext.getContext();
        ServerState state = (ServerState) context.removeAttachment(SERVER_STATE_KEY);
        if (state == null && request != null) {
            Object requestState = request.getRequestProp(SERVER_STATE_KEY);
            state = requestState instanceof ServerState ? (ServerState) requestState : null;
        }
        if (request != null) {
            request.removeRequestProp(SERVER_STATE_KEY);
        }
        if (state == null) {
            return;
        }
        removeServerState(state);

        SofaRpcServerEvent event = state.event;
        // Triple在ServerReceiveEvent之后才补齐方法名，因此结束时再次填充请求元数据。
        fillServerRequestMetadata(event, request);
        fillServerPhases(event, state.phases);
        fillResult(event, response, throwable);
        updateSize(event, request, response, context, state.internalContext);
        removePhaseDurations(state.internalContext, state.phases);
        event.end();
        if (event.shouldCommit()) {
            event.commit();
        }
    }

    @Override
    public void checkState() {
        RpcInternalContext context = RpcInternalContext.peekContext();
        if (context == null) {
            return;
        }

        ClientState clientState = (ClientState) context.removeAttachment(CLIENT_STATE_KEY);
        if (clientState != null) {
            removePhaseDurations(clientState.internalContext, clientState.phases);
            finishIncomplete(clientState.event, clientState.phases);
        }
        ServerState serverState = (ServerState) context.removeAttachment(SERVER_STATE_KEY);
        if (serverState != null) {
            removeServerState(serverState);
            removePhaseDurations(serverState.internalContext, serverState.phases);
            finishIncomplete(serverState.event, serverState.phases);
        }
    }

    @Override
    public void recordPhase(RpcInternalContext context, String phase, long elapsedNanos) {
        if (context == null || phase == null || elapsedNanos < 0 || !isEnabled()) {
            return;
        }
        phaseDurations(context).record(phase, elapsedNanos);
    }

    private static ClientState clientState() {
        return (ClientState) RpcInternalContext.getContext().getAttachment(CLIENT_STATE_KEY);
    }

    private static void finishIncomplete(SofaRpcClientEvent event, PhaseDurations phases) {
        event.result = "INCOMPLETE";
        fillClientPhases(event, phases);
        event.end();
        if (event.shouldCommit()) {
            event.commit();
        }
    }

    private static void finishIncomplete(SofaRpcServerEvent event, PhaseDurations phases) {
        event.result = "INCOMPLETE";
        fillServerPhases(event, phases);
        event.end();
        if (event.shouldCommit()) {
            event.commit();
        }
    }

    private static void fillClientRequest(SofaRpcClientEvent event, SofaRequest request,
                                          RpcInternalContext context) {
        if (request != null) {
            event.service = value(request.getTargetServiceUniqueName());
            event.method = value(request.getMethodName());
            event.sourceApp = requestValue(request, RemotingConstants.HEAD_APP_NAME);
            event.targetApp = value(request.getTargetAppName());
            event.protocol = requestValue(request, RemotingConstants.HEAD_PROTOCOL);
            event.invokeType = value(request.getInvokeType());
        }
        fillAddresses(event, context);
        Object invokeCount = context.getAttachment(RpcConstants.INTERNAL_KEY_INVOKE_TIMES);
        if (invokeCount instanceof Number) {
            event.invokeCount = ((Number) invokeCount).intValue();
        }
    }

    private static void fillServerRequest(SofaRpcServerEvent event, SofaRequest request,
                                          RpcInternalContext context) {
        fillServerRequestMetadata(event, request);
        fillAddresses(event, context);
    }

    private static void fillServerRequestMetadata(SofaRpcServerEvent event, SofaRequest request) {
        if (request != null) {
            event.service = value(request.getTargetServiceUniqueName());
            event.method = value(request.getMethodName());
            event.sourceApp = requestValue(request, RemotingConstants.HEAD_APP_NAME);
            event.targetApp = value(request.getTargetAppName());
            event.protocol = requestValue(request, RemotingConstants.HEAD_PROTOCOL);
            event.invokeType = requestValue(request, RemotingConstants.HEAD_INVOKE_TYPE);
        }
    }

    private static void fillAddresses(SofaRpcClientEvent event, RpcInternalContext context) {
        event.localAddress = NetUtils.toAddressString(context.getLocalAddress());
        event.remoteAddress = NetUtils.toAddressString(context.getRemoteAddress());
    }

    private static void fillAddresses(SofaRpcServerEvent event, RpcInternalContext context) {
        event.localAddress = NetUtils.toAddressString(context.getLocalAddress());
        event.remoteAddress = NetUtils.toAddressString(context.getRemoteAddress());
    }

    private static void fillClientPhases(SofaRpcClientEvent event, PhaseDurations phases) {
        event.firstResponseTime = phases.get(
            RpcConstants.INTERNAL_KEY_CLIENT_FIRST_STREAM_RESP_NANO);
        event.routerTime = phases.get(RpcConstants.INTERNAL_KEY_CLIENT_ROUTER_TIME_NANO);
        event.connectionTime = phases.get(RpcConstants.INTERNAL_KEY_CONN_CREATE_TIME_NANO);
        event.clientFilterTime = phases.get(RpcConstants.INTERNAL_KEY_CLIENT_FILTER_TIME_NANO);
        event.loadBalancerTime = phases.get(RpcConstants.INTERNAL_KEY_CLIENT_BALANCER_TIME_NANO);
        event.requestSerializationTime = phases.get(
            RpcConstants.INTERNAL_KEY_REQ_SERIALIZE_TIME_NANO);
        event.responseDeserializationTime = phases.get(
            RpcConstants.INTERNAL_KEY_RESP_DESERIALIZE_TIME_NANO);
    }

    private static void fillServerPhases(SofaRpcServerEvent event, PhaseDurations phases) {
        event.requestDeserializationTime = phases.get(
            RpcConstants.INTERNAL_KEY_REQ_DESERIALIZE_TIME_NANO);
        event.responseSerializationTime = phases.get(
            RpcConstants.INTERNAL_KEY_RESP_SERIALIZE_TIME_NANO);
        event.threadPoolWaitTime = phases.get(RpcConstants.INTERNAL_KEY_PROCESS_WAIT_TIME_NANO);
        event.businessTime = phases.get(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE_NANO);
        event.serverFilterTime = phases.get(RpcConstants.INTERNAL_KEY_SERVER_FILTER_TIME_NANO);
        event.serverNetworkWaitTime = phases.get(RpcConstants.INTERNAL_KEY_SERVER_NET_WAIT_NANO);
    }

    private static PhaseDurations replacePhaseDurations(RpcInternalContext context) {
        PhaseDurations phases = new PhaseDurations();
        context.setAttachment(PHASE_STATE_KEY, phases);
        return phases;
    }

    private static PhaseDurations phaseDurations(RpcInternalContext context) {
        Object existing = context.getAttachment(PHASE_STATE_KEY);
        if (existing instanceof PhaseDurations) {
            return (PhaseDurations) existing;
        }
        synchronized (context) {
            existing = context.getAttachment(PHASE_STATE_KEY);
            if (existing instanceof PhaseDurations) {
                return (PhaseDurations) existing;
            }
            PhaseDurations phases = new PhaseDurations();
            context.setAttachment(PHASE_STATE_KEY, phases);
            return phases;
        }
    }

    private static void removePhaseDurations(RpcInternalContext context, PhaseDurations phases) {
        if (context != null && context.getAttachment(PHASE_STATE_KEY) == phases) {
            context.removeAttachment(PHASE_STATE_KEY);
        }
    }

    private static void fillResult(SofaRpcClientEvent event, SofaResponse response, Throwable throwable) {
        Result result = result(response, throwable);
        event.result = result.name;
        event.errorClass = result.errorClass;
        event.errorCode = result.errorCode;
    }

    private static void fillResult(SofaRpcServerEvent event, SofaResponse response, Throwable throwable) {
        Result result = result(response, throwable);
        event.result = result.name;
        event.errorClass = result.errorClass;
        event.errorCode = result.errorCode;
    }

    private static Result result(SofaResponse response, Throwable throwable) {
        Throwable error = throwable;
        if (error == null && response != null && response.getAppResponse() instanceof Throwable) {
            error = (Throwable) response.getAppResponse();
        }
        if (error != null) {
            int errorCode = error instanceof SofaRpcException ? ((SofaRpcException) error).getErrorType() : -1;
            String result = response != null && response.getAppResponse() == error ? "BUSINESS_ERROR" : "EXCEPTION";
            return new Result(result, error.getClass().getName(), errorCode);
        }
        if (response != null && response.isError()) {
            return new Result("RPC_ERROR", "", -1);
        }
        return new Result("SUCCESS", "", -1);
    }

    private static void updateSize(SofaRpcClientEvent event, SofaRequest request, SofaResponse response,
                                   RpcInternalContext current, RpcInternalContext captured) {
        long requestSize = size(current, captured, RpcConstants.INTERNAL_KEY_REQ_SIZE,
            request == null ? null : request.getData());
        long responseSize = size(current, captured, RpcConstants.INTERNAL_KEY_RESP_SIZE,
            response == null ? null : response.getData());
        if (requestSize >= 0) {
            event.requestSize = requestSize;
        }
        if (responseSize >= 0) {
            event.responseSize = responseSize;
        }
    }

    private static void updateSize(SofaRpcServerEvent event, SofaRequest request, SofaResponse response,
                                   RpcInternalContext current, RpcInternalContext captured) {
        // 服务端请求大小优先取接收请求时的上下文，异步响应大小优先取当前回包上下文。
        long requestSize = size(captured, current, RpcConstants.INTERNAL_KEY_REQ_SIZE,
            request == null ? null : request.getData());
        long responseSize = size(current, captured, RpcConstants.INTERNAL_KEY_RESP_SIZE,
            response == null ? null : response.getData());
        if (requestSize >= 0) {
            event.requestSize = requestSize;
        }
        if (responseSize >= 0) {
            event.responseSize = responseSize;
        }
    }

    private static long size(RpcInternalContext preferred, RpcInternalContext fallback, String key,
                             AbstractByteBuf data) {
        Long value = contextLong(preferred, key);
        if (value == null && fallback != preferred) {
            value = contextLong(fallback, key);
        }
        return value == null ? dataSize(data) : value;
    }

    private static Long contextLong(RpcInternalContext context, String key) {
        if (context == null) {
            return null;
        }
        Object value = context.getAttachment(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private static long dataSize(AbstractByteBuf data) {
        if (data == null) {
            return -1L;
        }
        try {
            return data.readableBytes();
        } catch (RuntimeException e) {
            return -1L;
        }
    }

    private static String profileId(SofaRequest request) {
        Object profileId = request == null ? null : request.getRequestProp(ProfileConstants.PROFILE_ID_KEY);
        if (profileId instanceof String) {
            String value = (String) profileId;
            if (value.length() == 36) {
                try {
                    String normalized = UUID.fromString(value).toString();
                    if (normalized.equalsIgnoreCase(value)) {
                        return normalized;
                    }
                } catch (IllegalArgumentException ignored) { // NOPMD
                    // 外部请求可能携带非法标识，重新生成即可，避免污染JFR记录。
                }
            }
        }
        return newProfileId();
    }

    private static String newProfileId() {
        return UUID.randomUUID().toString();
    }

    private static String requestValue(SofaRequest request, String key) {
        Object value = request.getRequestProp(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static void removeServerState(ServerState state) {
        SofaRequest request = state.request;
        if (request != null) {
            request.removeRequestProp(SERVER_STATE_KEY);
        }
    }

    private static final class ClientState {

        private final SofaRpcClientEvent event;

        private final PhaseDurations     phases;

        private final RpcInternalContext internalContext;

        private ClientState(SofaRpcClientEvent event, PhaseDurations phases,
                            RpcInternalContext internalContext) {
            this.event = event;
            this.phases = phases;
            this.internalContext = internalContext;
        }
    }

    private static final class ServerState {

        private final SofaRpcServerEvent event;

        private final PhaseDurations     phases;

        private final RpcInternalContext internalContext;

        private final SofaRequest        request;

        private ServerState(SofaRpcServerEvent event, PhaseDurations phases,
                            RpcInternalContext internalContext, SofaRequest request) {
            this.event = event;
            this.phases = phases;
            this.internalContext = internalContext;
            this.request = request;
        }
    }

    private static final class PhaseDurations {

        private final Map<String, Long> values = new ConcurrentHashMap<String, Long>();

        private void record(String phase, long elapsedNanos) {
            values.put(phase, elapsedNanos);
        }

        private long get(String phase) {
            Long value = values.get(phase);
            return value == null ? -1L : value;
        }
    }

    private static final class Result {

        private final String name;

        private final String errorClass;

        private final int    errorCode;

        private Result(String name, String errorClass, int errorCode) {
            this.name = name;
            this.errorClass = errorClass;
            this.errorCode = errorCode;
        }
    }
}
