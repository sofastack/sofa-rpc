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
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.rpc.api.context.RpcReferenceContext;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;

import java.net.InetSocketAddress;

/**
 * Filter for build RpcReferenceContext
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "referenceContext", order = -19500)
@AutoActive(consumerSide = true)
public class RpcReferenceContextFilter extends Filter {

    /**
     * 是否自动加载
     *
     * @param invoker 调用器
     * @return 是否加载本过滤器
     */
    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        return RpcInternalContext.isAttachmentEnable();
    }

    @Override
    public SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException {

        ConsumerConfig config = (ConsumerConfig) invoker.getConfig();
        RpcReferenceContext referenceCtx = new RpcReferenceContext();
        referenceCtx.setGeneric(config.isGeneric());
        referenceCtx.setInterfaceName(config.getInterfaceId());
        referenceCtx.setUniqueId(config.getUniqueId());
        referenceCtx.setServiceName(request.getTargetServiceUniqueName());
        referenceCtx.setMethodName(request.getMethodName());

        RpcInternalContext context = RpcInternalContext.getContext();

        ProviderInfo providerInfo = context.getProviderInfo();
        if (providerInfo != null) {
            referenceCtx.setTargetAppName(providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_APP_NAME));
            referenceCtx.setTargetUrl(providerInfo.getHost() + ":" + providerInfo.getPort());
        }

        referenceCtx.setProtocol(config.getProtocol());
        referenceCtx.setInvokeType(request.getInvokeType());
        referenceCtx.setRouteRecord((String) context.getAttachment(RpcConstants.INTERNAL_KEY_ROUTER_RECORD));

        RpcInvokeContext.getContext().put(RemotingConstants.INVOKE_CTX_RPC_REF_CTX, referenceCtx);

        SofaResponse response = invoker.invoke(request);

        // 调用后
        InetSocketAddress local = context.getLocalAddress();
        if (local != null) {
            referenceCtx.setClientIP(NetUtils.toIpString(local));
            referenceCtx.setClientPort(local.getPort());
        }
        Long ct = (Long) context.getAttachment(RpcConstants.INTERNAL_KEY_CONN_CREATE_TIME);
        if (ct != null) {
            referenceCtx.setConnEstablishedSpan(ct);
        }
        Integer qs = (Integer) context.getAttachment(RpcConstants.INTERNAL_KEY_REQ_SIZE);
        if (qs != null) {
            referenceCtx.setRequestSize(qs.longValue());
        }
        Integer ps = (Integer) context.getAttachment(RpcConstants.INTERNAL_KEY_RESP_SIZE);
        if (ps != null) {
            referenceCtx.setResponseSize(ps.longValue());
        }

        referenceCtx.setTraceId((String) context.getAttachment(RpcConstants.INTERNAL_KEY_TRACE_ID));
        referenceCtx.setRpcId((String) context.getAttachment(RpcConstants.INTERNAL_KEY_SPAN_ID));
        Long ce = (Long) context.getAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE);
        if (ce != null) {
            referenceCtx.setCostTime(ce);
        }

        return response;
    }
}
