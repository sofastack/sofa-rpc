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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.log.LogCodes;

import java.util.List;

/**
 * Abstract load balancer.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public abstract class AbstractLoadBalancer extends LoadBalancer {

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    public AbstractLoadBalancer(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public ProviderInfo select(SofaRequest request, List<ProviderInfo> providerInfos) throws SofaRpcException {
        if (providerInfos.size() == 0) {
            throw noAvailableProviderException(request.getTargetServiceUniqueName());
        }
        if (providerInfos.size() == 1) {
            return providerInfos.get(0);
        } else {
            return doSelect(request, providerInfos);
        }
    }

    /**
     * 找不到可用的服务列表的异常
     *
     * @param serviceKey 服务关键字
     * @return 服务端
     */
    protected SofaRouteException noAvailableProviderException(String serviceKey) {
        return new SofaRouteException(LogCodes.getLog(LogCodes.ERROR_NO_AVAILABLE_PROVIDER, serviceKey));
    }

    /**
     * 根据负载均衡筛选
     *
     * @param invocation    请求
     * @param providerInfos 全部服务端连接
     * @return 服务端连接 provider
     */
    protected abstract ProviderInfo doSelect(SofaRequest invocation, List<ProviderInfo> providerInfos);

    /**
     * Gets weight.
     *
     * @param providerInfo the provider
     * @return the weight
     */
    protected int getWeight(ProviderInfo providerInfo) {
        // 从provider中或得到相关权重,默认值100
        return providerInfo.getWeight() < 0 ? 0 : providerInfo.getWeight();
    }
}
