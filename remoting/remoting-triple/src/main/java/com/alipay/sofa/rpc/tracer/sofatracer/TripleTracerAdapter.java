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
package com.alipay.sofa.rpc.tracer.sofatracer;

import com.alipay.common.tracer.core.appender.self.SelfLog;
import com.alipay.common.tracer.core.context.span.SofaTracerSpanContext;
import com.alipay.common.tracer.core.context.trace.SofaTraceContext;
import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.TracerCompatibleConstants;
import com.alipay.sofa.rpc.common.utils.JSONUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ServerReceiveEvent;
import com.alipay.sofa.rpc.event.ServerSendEvent;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.triple.TripleContants;
import com.alipay.sofa.rpc.server.triple.TripleHeadKeys;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerServiceDefinition;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import static com.alipay.sofa.rpc.common.RemotingConstants.HEAD_APP_NAME;

/**
 * 客户端：startRpc ——&gt; filter --&gt; beforeSend --&gt; 存入tracer信息 --&gt; clientReceived
 * 服务端：serverReceived --&gt; filter --&gt; serverSend
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 * @author <a href="mailto:chpengzh@foxmail.com">Chen.Pengzhi</a>
 */
public class TripleTracerAdapter {

    /**
     * slf4j for this class
     */
    private static final Logger LOGGER     = LoggerFactory.getLogger(TripleTracerAdapter.class);
    private static final String USERID_KEY = "userid";

    /**
     * 存入tracer信息
     *
     * @param sofaRequest   SofaRequest
     * @param requestHeader Metadata
     */
    public static void beforeSend(SofaRequest sofaRequest, ConsumerConfig consumerConfig, Metadata requestHeader) {

        // 客户端设置请求服务端的Header
        // tracer信息放入request 发到服务端

        Map<String, String> header = new HashMap<String, String>();
        header.put(RemotingConstants.HEAD_METHOD_NAME, sofaRequest.getMethodName());
        header.put(RemotingConstants.HEAD_TARGET_SERVICE, sofaRequest.getTargetServiceUniqueName());
        header.put(RemotingConstants.HEAD_TARGET_APP, sofaRequest.getTargetAppName());
        //客户端的启动
        SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
        //获取并不弹出
        SofaTracerSpan clientSpan = sofaTraceContext.getCurrentSpan();
        if (clientSpan == null) {
            SelfLog.warn("ClientSpan is null.Before call interface=" + sofaRequest.getInterfaceName() + ",method=" +
                    sofaRequest.getMethodName());
        } else {
            SofaTracerSpanContext sofaTracerSpanContext = clientSpan.getSofaTracerSpanContext();
            header.put(TripleHeadKeys.HEAD_KEY_TRACE_ID.name(), sofaTracerSpanContext.getTraceId());
            header.put(TripleHeadKeys.HEAD_KEY_RPC_ID.name(), sofaTracerSpanContext.getSpanId());
            header.put(TripleHeadKeys.HEAD_KEY_OLD_TRACE_ID.name(), sofaTracerSpanContext.getTraceId());
            header.put(TripleHeadKeys.HEAD_KEY_OLD_RPC_ID.name(), sofaTracerSpanContext.getSpanId());

            header.put(TripleHeadKeys.HEAD_KEY_BIZ_BAGGAGE_TYPE.name(),
                    sofaTracerSpanContext.getBizSerializedBaggage());

            header.put(TripleHeadKeys.HEAD_KEY_SYS_BAGGAGE_TYPE.name(),
                    sofaTracerSpanContext.getSysSerializedBaggage());
        }
        //获取 RPC 上下文

        RpcInvokeContext internalContext = RpcInvokeContext.getContext();
        String route = (String) internalContext.get(USERID_KEY);

        if (StringUtils.isNotBlank(route)) {
            Map<String, String> map = new HashMap<>();
            map.put(USERID_KEY, route);
            header.put(TripleHeadKeys.HEAD_KEY_UNIT_INFO.name(), JSONUtils.toJSONString(map));
        }

        header.put(TripleHeadKeys.HEAD_KEY_SERVICE_VERSION.name(), "1.0");

        header.put(TripleHeadKeys.HEAD_KEY_META_TYPE.name(), "rpc");
        header.put(TripleHeadKeys.HEAD_KEY_CURRENT_APP.name(), (String) sofaRequest.getRequestProp(HEAD_APP_NAME));
        header.put(TripleHeadKeys.HEAD_KEY_CONSUMER_APP.name(), (String) sofaRequest.getRequestProp(HEAD_APP_NAME));

        header.put(TripleHeadKeys.HEAD_KEY_PROTOCOL_TYPE.name(),
                (String) sofaRequest.getRequestProp(RemotingConstants.HEAD_PROTOCOL));
        header.put(TripleHeadKeys.HEAD_KEY_INVOKE_TYPE.name(),
                (String) sofaRequest.getRequestProp(RemotingConstants.HEAD_INVOKE_TYPE));

        final String source = consumerConfig.getParameter("interworking.source");
        if (StringUtils.isNotBlank(source)) {
            header.put(TripleHeadKeys.HEAD_KEY_SOURCE_TENANTID.name(),
                    source);
        }

        final String target = consumerConfig.getParameter("interworking.target");
        if (StringUtils.isNotBlank(target)) {
            header.put(TripleHeadKeys.HEAD_KEY_TARGET_TENANTID.name(),
                    target);
        }
        for (Map.Entry<String, String> entry : header.entrySet()) {
            if (StringUtils.isNotBlank(entry.getValue())) {
                requestHeader.put(TripleHeadKeys.getKey(entry.getKey()), entry.getValue());
            }
        }
    }

    /**
     * 适配服务端serverReceived
     */
    public static void serverReceived(ServerServiceDefinition serverServiceDefinition, final ServerCall call,
                                      Metadata requestHeaders) {
        try {
            SofaRequest sofaRequest = new SofaRequest();
            Map<String, String> traceMap = new HashMap<String, String>();

            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_TARGET_SERVICE)) {
                sofaRequest.setTargetServiceUniqueName(requestHeaders
                    .get(TripleHeadKeys.HEAD_KEY_TARGET_SERVICE));
                sofaRequest.setInterfaceName(requestHeaders
                    .get(TripleHeadKeys.HEAD_KEY_TARGET_SERVICE));
            } else {
                String serviceName = serverServiceDefinition.getServiceDescriptor().getName();
                sofaRequest.setTargetServiceUniqueName(serviceName);
                sofaRequest.setInterfaceName(serviceName);
            }
            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_TARGET_APP)) {
                sofaRequest.setTargetAppName(requestHeaders
                    .get(TripleHeadKeys.HEAD_KEY_TARGET_APP));
            }

            //先取兼容的
            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_OLD_TRACE_ID)) {
                traceMap.put(TracerCompatibleConstants.TRACE_ID_KEY,
                    requestHeaders.get(TripleHeadKeys.HEAD_KEY_OLD_TRACE_ID));
            }
            else if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_TRACE_ID)) {
                traceMap.put(TracerCompatibleConstants.TRACE_ID_KEY,
                    requestHeaders.get(TripleHeadKeys.HEAD_KEY_TRACE_ID));
            }
            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_OLD_RPC_ID)) {
                traceMap
                    .put(TracerCompatibleConstants.RPC_ID_KEY, requestHeaders.get(TripleHeadKeys.HEAD_KEY_OLD_RPC_ID));
            }
            else if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_RPC_ID)) {
                traceMap
                    .put(TracerCompatibleConstants.RPC_ID_KEY, requestHeaders.get(TripleHeadKeys.HEAD_KEY_RPC_ID));
            }

            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_SERVICE_VERSION)) {
                //   traceMap.put(TracerCompatibleConstants.RPC_ID_KEY, requestHeaders.get(GrpcHeadKeys.HEAD_KEY_RPC_ID));
            }

            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_SAMP_TYPE)) {
                traceMap.put(TracerCompatibleConstants.SAMPLING_MARK,
                    requestHeaders.get(TripleHeadKeys.HEAD_KEY_SAMP_TYPE));
            }

            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_CURRENT_APP)) {
                sofaRequest.addRequestProp(HEAD_APP_NAME, requestHeaders.get(TripleHeadKeys.HEAD_KEY_CURRENT_APP));
            }

            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_PROTOCOL_TYPE)) {
                sofaRequest.addRequestProp(RemotingConstants.HEAD_PROTOCOL,
                    requestHeaders.get(TripleHeadKeys.HEAD_KEY_PROTOCOL_TYPE));
            }
            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_INVOKE_TYPE)) {
                sofaRequest.addRequestProp(RemotingConstants.HEAD_INVOKE_TYPE,
                    requestHeaders.get(TripleHeadKeys.HEAD_KEY_INVOKE_TYPE));
            }

            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_BIZ_BAGGAGE_TYPE)) {
                traceMap.put(TracerCompatibleConstants.PEN_ATTRS_KEY,
                    requestHeaders.get(TripleHeadKeys.HEAD_KEY_BIZ_BAGGAGE_TYPE));
            }

            if (requestHeaders.containsKey(TripleHeadKeys.HEAD_KEY_SYS_BAGGAGE_TYPE)) {
                traceMap.put(TracerCompatibleConstants.PEN_SYS_ATTRS_KEY,
                    requestHeaders.get(TripleHeadKeys.HEAD_KEY_SYS_BAGGAGE_TYPE));
            }

            if (!traceMap.isEmpty()) {
                sofaRequest.addRequestProp(RemotingConstants.RPC_TRACE_NAME, traceMap);
            }

            final RpcInvokeContext context = RpcInvokeContext.getContext();
            context.put(TripleContants.SOFA_REQUEST_KEY, sofaRequest);

            SocketAddress socketAddress = call.getAttributes().get(
                Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
            context.put(TripleContants.SOFA_REMOTE_ADDR_KEY, socketAddress);

            RpcInternalContext internalContext = RpcInternalContext.getContext();
            internalContext.setProviderSide(true);
            if (socketAddress instanceof InetSocketAddress) {
                internalContext.setRemoteAddress((InetSocketAddress) socketAddress);
            }
            if (EventBus.isEnable(ServerReceiveEvent.class)) {
                EventBus.post(new ServerReceiveEvent(sofaRequest));
            }

            SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
            SofaTracerSpan serverSpan = sofaTraceContext.getCurrentSpan();
            if (serverSpan != null) {
                serverSpan.setTag("service", sofaRequest.getTargetServiceUniqueName());
                // 从请求里获取ConsumerTracerFilter额外传递的信息
                serverSpan.setTag("remote.app", (String) sofaRequest.getRequestProp(HEAD_APP_NAME));
                //serverSpan.setTag(RpcSpanTags.PROTOCOL, (String) request.getRequestProp(HEAD_PROTOCOL));
                //serverSpan.setTag(RpcSpanTags.INVOKE_TYPE, (String) request.getRequestProp(HEAD_INVOKE_TYPE));*/
            }
        } catch (Throwable e) {
        }
    }

    /**
     * 适配服务端serverSend
     */
    public static void serverSend(final Metadata requestHeaders, SofaResponse response, Throwable throwable) {
        if (EventBus.isEnable(ServerSendEvent.class)) {
            SofaRequest request = (SofaRequest) RpcInvokeContext.getContext()
                .get(TripleContants.SOFA_REQUEST_KEY);
            if (request == null) {
                request = new SofaRequest();
            }
            if (request.getTargetServiceUniqueName() == null) {
                request.setTargetServiceUniqueName(requestHeaders.get(TripleHeadKeys.HEAD_KEY_TARGET_SERVICE));
            }
            if (request.getMethodName() == null) {
                request.setMethodName(requestHeaders.get(TripleHeadKeys.HEAD_KEY_METHOD_NAME));
            }
            EventBus.post(new ServerSendEvent(request, response, throwable));
        }
    }
}