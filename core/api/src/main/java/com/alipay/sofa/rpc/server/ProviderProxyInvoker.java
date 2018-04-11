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
package com.alipay.sofa.rpc.server;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.filter.FilterChain;
import com.alipay.sofa.rpc.filter.ProviderInvoker;
import com.alipay.sofa.rpc.invoke.Invoker;

/**
 * 服务端调用链入口
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ProviderProxyInvoker implements Invoker {

    /**
     * 对应的客户端信息
     */
    private final ProviderConfig providerConfig;

    /**
     * 过滤器执行链
     */
    private final FilterChain    filterChain;

    /**
     * 构造执行链
     *
     * @param providerConfig 服务端配置
     */
    public ProviderProxyInvoker(ProviderConfig providerConfig) {
        this.providerConfig = providerConfig;
        // 最底层是调用过滤器
        this.filterChain = FilterChain.buildProviderChain(providerConfig,
            new ProviderInvoker(providerConfig));
    }

    /**
     * proxy拦截的调用
     *
     * @param request 请求消息
     * @return 调用结果
     * @throws SofaRpcException rpc异常
     */
    @Override
    public SofaResponse invoke(SofaRequest request) throws SofaRpcException {
        return filterChain.invoke(request);
    }

    /**
     * @return the providerConfig
     */
    public ProviderConfig getProviderConfig() {
        return providerConfig;
    }
}
