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
package com.alipay.sofa.rpc.context;

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Map;

/**
 * 透传数据解析器
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BaggageResolver {

    /**
     * logger for this class
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(BaggageResolver.class);

    /**
     * 通过请求透传数据
     *
     * @param context RpcInvokeContext
     * @param request 请求
     */
    public static void carryWithRequest(RpcInvokeContext context, SofaRequest request) {
        if (context != null) {
            Map<String, String> requestBaggage = context.getAllRequestBaggage();
            if (CommonUtils.isNotEmpty(requestBaggage)) { // 需要透传
                request.addRequestProp(RemotingConstants.RPC_REQUEST_BAGGAGE, requestBaggage);
            }
        }
    }

    /**
     * 从请求里获取透传数据
     *
     * @param context RpcInvokeContext
     * @param request 请求
     * @param init    传入上下文为空时，是否初始化
     */
    public static void pickupFromRequest(RpcInvokeContext context, SofaRequest request, boolean init) {
        if (context == null && !init) {
            return;
        }
        // 解析请求 
        Map<String, String> requestBaggage = (Map<String, String>) request
            .getRequestProp(RemotingConstants.RPC_REQUEST_BAGGAGE);
        if (CommonUtils.isNotEmpty(requestBaggage)) {
            if (context == null) {
                context = RpcInvokeContext.getContext();
            }
            context.putAllRequestBaggage(requestBaggage);
        }
    }

    /**
     * 从请求里获取透传数据
     *
     * @param context RpcInvokeContext
     * @param request 请求
     */
    public static void pickupFromRequest(RpcInvokeContext context, SofaRequest request) {
        pickupFromRequest(context, request, false);
    }

    /**
     * 通过响应透传数据
     *
     * @param context  RpcInvokeContext
     * @param response 响应
     */
    public static void carryWithResponse(RpcInvokeContext context, SofaResponse response) {
        if (context != null) {
            Map<String, String> responseBaggage = context.getAllResponseBaggage();
            if (CommonUtils.isNotEmpty(responseBaggage)) {
                String prefix = RemotingConstants.RPC_RESPONSE_BAGGAGE + ".";
                for (Map.Entry<String, String> entry : responseBaggage.entrySet()) {
                    response.addResponseProp(prefix + entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * 从响应里获取透传数据
     *
     * @param context  RpcInvokeContext
     * @param response 响应
     * @param init     传入上下文为空时，是否初始化
     */
    public static void pickupFromResponse(RpcInvokeContext context, SofaResponse response, boolean init) {
        if (context == null && !init) {
            return;
        }
        Map<String, String> responseBaggage = response.getResponseProps();
        if (CommonUtils.isNotEmpty(responseBaggage)) {
            String prefix = RemotingConstants.RPC_RESPONSE_BAGGAGE + ".";
            for (Map.Entry<String, String> entry : responseBaggage.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    if (context == null) {
                        context = RpcInvokeContext.getContext();
                    }
                    context.putResponseBaggage(entry.getKey().substring(prefix.length()),
                        entry.getValue());
                }
            }
        }
    }

    /**
     * 从响应里获取透传数据
     *
     * @param context  RpcInvokeContext
     * @param response 响应
     */
    public static void pickupFromResponse(RpcInvokeContext context, SofaResponse response) {
        pickupFromResponse(context, response, false);
    }
}