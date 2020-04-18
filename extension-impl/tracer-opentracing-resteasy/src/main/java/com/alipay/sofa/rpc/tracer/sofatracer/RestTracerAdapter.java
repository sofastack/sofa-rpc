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

import com.alipay.common.tracer.core.context.trace.SofaTraceContext;
import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.context.BaggageResolver;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.rest.SofaResourceFactory;
import com.alipay.sofa.rpc.server.rest.SofaResourceMethodInvoker;
import com.alipay.sofa.rpc.tracer.Tracers;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;
import org.jboss.resteasy.core.interception.PostMatchContainerRequestContext;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.jboss.resteasy.plugins.server.netty.NettyHttpResponse;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.alipay.sofa.rpc.common.RpcConstants.INTERNAL_KEY_APP_NAME;

/**
 * 客户端：startRpc ——&gt; filter --&gt; beforeSend --&gt; 存入tracer信息 --&gt; clientReceived
 * 服务端：serverReceived --&gt; filter --&gt; serverSend
 *
 * @author <a href="mailto:lw111072@antfin.com">LiWei.Liangen</a>
 * @author <a href="mailto:chpengzh@foxmail.com">Chen.Pengzhi</a>
 */
public class RestTracerAdapter {

    /**
     * slf4j for this class
     */
    private static final Logger LOGGER             = LoggerFactory.getLogger(RestTracerAdapter.class);

    private static final String METHOD_TYPE_STRING = "_method_type_string";

    /**
     * 存入tracer信息
     *
     * @param requestContext ClientRequestContext
     */
    public static void beforeSend(ClientRequestContext requestContext) {

        // tracer信息放入request 发到服务端
        SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
        SofaTracerSpan clientSpan = sofaTraceContext.getCurrentSpan();
        RpcInternalContext context = RpcInternalContext.getContext();
        if (clientSpan != null) {
            requestContext.getHeaders().add(RemotingConstants.NEW_RPC_TRACE_NAME,
                clientSpan.getSofaTracerSpanContext().serializeSpanContext());
        }
        // 客户端发送自己的应用名
        String appName = (String) context.getAttachment(INTERNAL_KEY_APP_NAME);
        if (appName != null) {
            requestContext.getHeaders().add(RemotingConstants.HEAD_APP_NAME, appName);
        }

        RestBaggageItemsHandler.encodeBaggageItemToRequest(requestContext.getHeaders());
    }

    /**
     * 适配服务端serverReceived
     */
    public static void serverReceived(NettyHttpRequest request) {
        try {
            SofaRequest sofaRequest = new SofaRequest();

            HttpHeaders headers = request.getHttpHeaders();
            String rpcTraceContext = headers.getHeaderString(RemotingConstants.NEW_RPC_TRACE_NAME);
            if (StringUtils.isNotBlank(rpcTraceContext)) {
                // 新格式
                sofaRequest.addRequestProp(RemotingConstants.NEW_RPC_TRACE_NAME, rpcTraceContext);
            } else {
                String traceIdKey = headers.getHeaderString(RemotingConstants.HTTP_HEADER_TRACE_ID_KEY);
                String rpcIdKey = headers.getHeaderString(RemotingConstants.HTTP_HEADER_RPC_ID_KEY);
                if (StringUtils.isEmpty(rpcIdKey)) {
                    rpcIdKey = request.getUri().getQueryParameters().getFirst(RemotingConstants.RPC_ID_KEY);
                }
                if (StringUtils.isEmpty(traceIdKey)) {
                    traceIdKey = request.getUri().getQueryParameters().getFirst(RemotingConstants.TRACE_ID_KEY);
                }

                if (StringUtils.isNotEmpty(traceIdKey) && StringUtils.isNotEmpty(rpcIdKey)) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put(RemotingConstants.TRACE_ID_KEY, traceIdKey);
                    map.put(RemotingConstants.RPC_ID_KEY, rpcIdKey);
                    String penAttrs = headers.getHeaderString(RemotingConstants.PEN_ATTRS_KEY);
                    map.put(RemotingConstants.PEN_ATTRS_KEY, penAttrs);
                    sofaRequest.addRequestProp(RemotingConstants.RPC_TRACE_NAME, map);
                }
            }
            Tracers.serverReceived(sofaRequest);

            RestBaggageItemsHandler.decodeBaggageItemsFromRequest(request, sofaRequest);
        } catch (Throwable t) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(LogCodes.getLog(LogCodes.ERROR_TRACER_UNKNOWN_EXP, "receive", "rest", "server"), t);
            }
        }
    }

    /**
     * 适配服务端filter
     *
     * @param requestContext ContainerRequestContext
     */
    public static void serverFilter(ContainerRequestContext requestContext) {
        try {
            SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
            SofaTracerSpan serverSpan = sofaTraceContext.getCurrentSpan();
            if (serverSpan != null) {
                RpcInternalContext context = RpcInternalContext.getContext();

                context.setAttachment(RpcConstants.INTERNAL_KEY_SERVER_RECEIVE_TIME, RpcRuntimeContext.now());

                SofaResourceMethodInvoker resourceMethodInvoker = (SofaResourceMethodInvoker)
                        ((PostMatchContainerRequestContext) requestContext)
                            .getResourceMethod();

                SofaResourceFactory factory = resourceMethodInvoker.getResource();
                String serviceName = factory.getServiceName();
                String appName = factory.getAppName();

                if (serviceName == null) {
                    serviceName = resourceMethodInvoker.getResourceClass().getName();
                }
                serverSpan.setTag(RpcSpanTags.SERVICE, serviceName);
                if (resourceMethodInvoker.getMethod() != null) {
                    serverSpan.setTag(RpcSpanTags.METHOD, resourceMethodInvoker.getMethod().getName());
                    //serverSend需要
                    context.setAttachment(METHOD_TYPE_STRING, resourceMethodInvoker.getMethod());
                }

                serverSpan.setTag(RpcSpanTags.REMOTE_IP, context.getRemoteHostName()); // 客户端地址

                String remoteAppName = requestContext.getHeaderString(RemotingConstants.HEAD_APP_NAME);
                if (StringUtils.isNotBlank(remoteAppName)) {
                    serverSpan.setTag(RpcSpanTags.REMOTE_APP, remoteAppName);
                }
                serverSpan.setTag(RpcSpanTags.PROTOCOL, RpcConstants.PROTOCOL_TYPE_REST);
                serverSpan.setTag(RpcSpanTags.INVOKE_TYPE, RpcConstants.INVOKER_TYPE_SYNC);
                if (appName == null) {
                    appName = (String) RpcRuntimeContext.get(RpcRuntimeContext.KEY_APPNAME);
                }
                serverSpan.setTag(RpcSpanTags.LOCAL_APP, appName);
            }
        } catch (Throwable t) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("the process of rest tracer server filter occur error ", t);
            }
        }
    }

    /**
     * 适配服务端serverSend
     */
    public static void serverSend(NettyHttpResponse response, Throwable throwable) {
        try {
            SofaRequest sofaRequest = new SofaRequest();
            SofaResponse sofaResponse = new SofaResponse();

            if (response == null) {
                sofaResponse.setErrorMsg("rest path ends with /favicon.ico");
            } else if (throwable != null) {
                if (response.getStatus() == 500) {
                    sofaResponse.setAppResponse(throwable);
                } else {
                    sofaResponse.setErrorMsg(throwable.getMessage());
                }

                Object method = RpcInternalContext.getContext().getAttachment(METHOD_TYPE_STRING);
                if (method != null) {
                    Class[] parameterTypes = ((Method) method).getParameterTypes();
                    String[] methodTypeString = new String[parameterTypes.length];
                    for (int i = 0; i < methodTypeString.length; i++) {
                        methodTypeString[i] = (parameterTypes[i].getName());
                    }
                    sofaRequest.setMethodArgSigs(methodTypeString);
                }
            }

            SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
            SofaTracerSpan serverSpan = sofaTraceContext.getCurrentSpan();

            RpcInternalContext context = RpcInternalContext.getContext();

            if (serverSpan != null) {
                serverSpan.setTag(RpcSpanTags.SERVER_BIZ_TIME,
                    (Number) context.getAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE));
            }

            RestBaggageItemsHandler.encodeBaggageItemsToResponse(response, sofaResponse);
            Tracers.serverSend(sofaRequest, sofaResponse, null);
        } catch (Throwable t) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("the process of rest tracer server send occur error ", t);
            }
        }
    }

    /**
     * Handle response callback from server handler
     *
     * @param responseContext   response instance
     */
    public static void clientReceived(ClientResponseContext responseContext) {
        RestBaggageItemsHandler.decodeBaggageItemsFromResponse(responseContext.getHeaders());
    }

    private static class RestBaggageItemsHandler {

        private static final String RPC_REQUEST_BAGGAGE_PREFIX      = RemotingConstants.RPC_REQUEST_BAGGAGE + ".";

        private static final int    RPC_REQUEST_BAGGAGE_PREFIX_LEN  = RPC_REQUEST_BAGGAGE_PREFIX.length();

        private static final String RPC_RESPONSE_BAGGAGE_PREFIX     = RemotingConstants.RPC_RESPONSE_BAGGAGE + ".";

        private static final int    RPC_RESPONSE_BAGGAGE_PREFIX_LEN = RPC_RESPONSE_BAGGAGE_PREFIX.length();

        /**
         * Encode baggage items from invoke context to request headers
         *
         * @param headers request headers
         */
        private static void encodeBaggageItemToRequest(MultivaluedMap<String, Object> headers) {
            // Attach baggage items
            RpcInvokeContext rpcInvokeContext = RpcInvokeContext.peekContext();
            if (rpcInvokeContext == null || !RpcInvokeContext.isBaggageEnable() ||
                rpcInvokeContext.getAllRequestBaggage() == null ||
                rpcInvokeContext.getAllRequestBaggage().isEmpty()) {
                return;
            }

            Map<String, String> baggageItems = rpcInvokeContext.getAllRequestBaggage();
            for (Map.Entry<String, String> entry : baggageItems.entrySet()) {
                String baggageKey = RPC_REQUEST_BAGGAGE_PREFIX + entry.getKey();
                headers.putSingle(baggageKey, entry.getValue());
            }
        }

        /**
         * Decode baggage items from netty request to sofa request context
         *
         * @param request       netty http request
         * @param sofaRequest   rpc request holder
         */
        private static void decodeBaggageItemsFromRequest(NettyHttpRequest request,
                                                          SofaRequest sofaRequest) {
            HttpHeaders headers = request.getHttpHeaders();
            // Decode baggage items
            MultivaluedMap<String, String> headerMaps = headers.getRequestHeaders();
            if (!RpcInvokeContext.isBaggageEnable() || headerMaps == null || headerMaps.isEmpty()) {
                return;
            }

            Map<String, String> baggageItems = new HashMap<String, String>();
            for (Map.Entry<String, List<String>> entry : headerMaps.entrySet()) {
                if (!entry.getKey().startsWith(RPC_REQUEST_BAGGAGE_PREFIX) ||
                    entry.getValue() == null ||
                    entry.getValue().isEmpty()) {
                    continue;
                }

                String value = entry.getValue().get(0);
                String key = entry.getKey().substring(RPC_REQUEST_BAGGAGE_PREFIX_LEN);
                baggageItems.put(key, value);
            }
            sofaRequest.addRequestProp(RemotingConstants.RPC_REQUEST_BAGGAGE, baggageItems);

            BaggageResolver.pickupFromRequest(RpcInvokeContext.peekContext(), sofaRequest, true);
        }

        /**
         * Encode baggage items from sofa response to http response context
         *
         * @param response     netty response
         * @param sofaResponse rpc response holder
         */
        private static void encodeBaggageItemsToResponse(NettyHttpResponse response,
                                                         SofaResponse sofaResponse) {
            RpcInvokeContext context = RpcInvokeContext.peekContext();
            if (response == null || context == null ||
                context.getAllResponseBaggage() == null ||
                context.getAllResponseBaggage().isEmpty()) {
                return;
            }

            for (Map.Entry<String, String> entry : context.getAllResponseBaggage().entrySet()) {
                String key = RPC_RESPONSE_BAGGAGE_PREFIX + entry.getKey();
                String value = entry.getValue();
                response.getOutputHeaders().add(key, value);
            }

            BaggageResolver.carryWithResponse(context, sofaResponse);
        }

        /**
         * Decode baggage items from response headers to invoke context
         *
         * @param headers response headers
         */
        private static void decodeBaggageItemsFromResponse(MultivaluedMap<String, String> headers) {
            if (!RpcInvokeContext.isBaggageEnable() || headers == null || headers.isEmpty()) {
                return;
            }

            Map<String, String> baggageItems = new LinkedHashMap<String, String>();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (!entry.getKey().startsWith(RPC_RESPONSE_BAGGAGE_PREFIX) ||
                    entry.getValue() == null ||
                    entry.getValue().isEmpty()) {
                    continue;
                }

                String key = entry.getKey().substring(RPC_RESPONSE_BAGGAGE_PREFIX_LEN);
                String value = entry.getValue().get(0);
                baggageItems.put(key, value);
            }

            RpcInvokeContext.getContext().putAllResponseBaggage(baggageItems);
        }
    }
}