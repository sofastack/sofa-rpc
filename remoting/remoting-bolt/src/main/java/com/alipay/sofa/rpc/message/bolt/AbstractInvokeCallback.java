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
package com.alipay.sofa.rpc.message.bolt;

import com.alipay.remoting.RejectedExecutionPolicy;
import com.alipay.remoting.RejectionProcessableInvokeCallback;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.BaggageResolver;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public abstract class AbstractInvokeCallback implements RejectionProcessableInvokeCallback {
    /**
     * 服务消费者配置
     */
    protected final ConsumerConfig    consumerConfig;
    /**
     * 服务提供者信息
     */
    protected final ProviderInfo      providerInfo;
    /**
     * 请求
     */
    protected final SofaRequest       request;
    /**
     * 请求运行时的ClassLoader
     */
    protected ClassLoader             classLoader;
    /**
     * 线程上下文
     */
    protected RpcInternalContext      context;
    /**
     * 线程繁忙时的拒绝策略
     */
    protected RejectedExecutionPolicy rejectedExecutionPolicy;

    protected AbstractInvokeCallback(ConsumerConfig consumerConfig, ProviderInfo providerInfo, SofaRequest request,
                                     RpcInternalContext context, ClassLoader classLoader) {
        this.consumerConfig = consumerConfig;
        this.providerInfo = providerInfo;
        this.request = request;
        this.context = context;
        this.classLoader = classLoader;
        this.setRejectedExecutionPolicy(consumerConfig);
    }

    private void setRejectedExecutionPolicy(ConsumerConfig consumerConfig) {
        if (null == consumerConfig || consumerConfig.getRejectedExecutionPolicy() == null) {
            this.rejectedExecutionPolicy = RejectedExecutionPolicy.DISCARD;
            return;
        }

        String policy = consumerConfig.getRejectedExecutionPolicy();
        this.rejectedExecutionPolicy = RejectedExecutionPolicy.valueOf(policy);
    }

    protected void recordClientElapseTime() {
        if (context != null) {
            Long startTime = (Long) context.removeAttachment(RpcConstants.INTERNAL_KEY_CLIENT_SEND_TIME);
            if (startTime != null) {
                context.setAttachment(RpcConstants.INTERNAL_KEY_CLIENT_ELAPSE, RpcRuntimeContext.now() - startTime);
            }
        }
    }

    protected void pickupBaggage(SofaResponse response) {
        if (RpcInvokeContext.isBaggageEnable()) {
            RpcInvokeContext old = null;
            RpcInvokeContext newContext = null;
            if (context != null) {
                old = (RpcInvokeContext) context.getAttachment(RpcConstants.HIDDEN_KEY_INVOKE_CONTEXT);
            }
            if (old == null) {
                newContext = RpcInvokeContext.getContext();
            } else {
                RpcInvokeContext.setContext(old);
                newContext = RpcInvokeContext.getContext();
            }
            BaggageResolver.pickupFromResponse(newContext, response);

            if (old != null) {
                old.getAllResponseBaggage().putAll(newContext.getAllResponseBaggage());
                old.getAllRequestBaggage().putAll(newContext.getAllRequestBaggage());
            }

        }
    }

    /**
     * @see RejectionProcessableInvokeCallback#rejectedExecutionPolicy() 
     */
    @Override
    public RejectedExecutionPolicy rejectedExecutionPolicy() {
        return rejectedExecutionPolicy;
    }
}
