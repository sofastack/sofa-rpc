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
package com.alipay.sofa.rpc.filter.sofatracer;

import com.alipay.common.tracer.core.context.trace.SofaTraceContext;
import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.filter.AutoActive;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import com.alipay.sofa.rpc.module.SofaTracerModule;
import com.alipay.sofa.rpc.tracer.sofatracer.log.tags.RpcSpanTags;

import static com.alipay.sofa.rpc.common.RemotingConstants.HEAD_APP_NAME;
import static com.alipay.sofa.rpc.common.RemotingConstants.HEAD_INVOKE_TYPE;
import static com.alipay.sofa.rpc.common.RemotingConstants.HEAD_PROTOCOL;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
@Extension(value = "providerTracer", order = -10000)
@AutoActive(providerSide = true)
public class ProviderTracerFilter extends Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        return SofaTracerModule.isEnable();
    }

    @Override
    public SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException {
        SofaTracerSpan serverSpan = null;
        try {
            SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
            serverSpan = sofaTraceContext.getCurrentSpan();
            if (serverSpan != null) {
                RpcInternalContext context = RpcInternalContext.getContext();
                serverSpan.setTag(RpcSpanTags.SERVICE, request.getTargetServiceUniqueName());
                serverSpan.setTag(RpcSpanTags.METHOD, request.getMethodName());
                serverSpan.setTag(RpcSpanTags.REMOTE_IP, context.getRemoteHostName()); // 客户端地址

                // 从请求里获取ConsumerTracerFilter额外传递的信息
                serverSpan.setTag(RpcSpanTags.REMOTE_APP, (String) request.getRequestProp(HEAD_APP_NAME));
                serverSpan.setTag(RpcSpanTags.PROTOCOL, (String) request.getRequestProp(HEAD_PROTOCOL));
                serverSpan.setTag(RpcSpanTags.INVOKE_TYPE, (String) request.getRequestProp(HEAD_INVOKE_TYPE));

                ProviderConfig providerConfig = (ProviderConfig) invoker.getConfig();
                serverSpan.setTag(RpcSpanTags.LOCAL_APP, providerConfig.getAppName());

                serverSpan.setTag(RpcSpanTags.SERVER_THREAD_POOL_WAIT_TIME,
                    (Number) context.getAttachment(RpcConstants.INTERNAL_KEY_PROCESS_WAIT_TIME));
            }
            return invoker.invoke(request);
        } finally {
            if (serverSpan != null) {
                serverSpan.setTag(RpcSpanTags.SERVER_BIZ_TIME,
                    (Number) RpcInternalContext.getContext().getAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE));
            }
        }
    }

}
