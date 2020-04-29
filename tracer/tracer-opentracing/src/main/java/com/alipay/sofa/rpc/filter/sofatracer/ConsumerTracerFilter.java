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
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.common.RpcConstants;
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

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
@Extension(value = "consumerTracer", order = -10000)
@AutoActive(consumerSide = true)
public class ConsumerTracerFilter extends Filter {

    @Override
    public boolean needToLoad(FilterInvoker invoker) {
        return SofaTracerModule.isEnable();
    }

    @Override
    public SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException {

        SofaTraceContext sofaTraceContext = SofaTraceContextHolder.getSofaTraceContext();
        SofaTracerSpan clientSpan = sofaTraceContext.getCurrentSpan();

        clientSpan.setTag(RpcSpanTags.INVOKE_TYPE, request.getInvokeType());

        RpcInternalContext context = RpcInternalContext.getContext();
        clientSpan.setTag(RpcSpanTags.ROUTE_RECORD,
            (String) context.getAttachment(RpcConstants.INTERNAL_KEY_ROUTER_RECORD));

        ProviderInfo providerInfo = context.getProviderInfo();
        if (providerInfo != null) {
            clientSpan.setTag(RpcSpanTags.REMOTE_APP, providerInfo.getStaticAttr(ProviderInfoAttrs.ATTR_APP_NAME));
            clientSpan.setTag(RpcSpanTags.REMOTE_IP, providerInfo.getHost() + ":" + providerInfo.getPort());
        }

        return invoker.invoke(request);
        // 因为异步的场景，所以received不写在这里
    }
}
