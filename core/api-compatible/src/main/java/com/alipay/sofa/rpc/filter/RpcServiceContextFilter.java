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

import com.alipay.sofa.rpc.api.context.RpcServiceContext;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;

/**
 * Filter for build RpcServiceContextFilter
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "serviceContext", order = -19500)
@AutoActive(providerSide = true)
public class RpcServiceContextFilter extends Filter {

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
        RpcServiceContext serviceCtx = new RpcServiceContext();
        RpcInternalContext internalCtx = RpcInternalContext.getContext();
        serviceCtx.setServiceName(request.getTargetServiceUniqueName());
        serviceCtx.setMethodName(request.getMethodName());
        serviceCtx.setTraceId((String) internalCtx.getAttachment(RpcConstants.INTERNAL_KEY_TRACE_ID));
        serviceCtx.setRpcId((String) internalCtx.getAttachment(RpcConstants.INTERNAL_KEY_SPAN_ID));
        serviceCtx.setCallerAppName((String) request.getRequestProp(RemotingConstants.HEAD_APP_NAME));
        serviceCtx.setCallerUrl(internalCtx.getRemoteHostName());

        RpcInvokeContext.getContext().put(RemotingConstants.INVOKE_CTX_RPC_SER_CTX, serviceCtx);

        return invoker.invoke(request);
    }
}
