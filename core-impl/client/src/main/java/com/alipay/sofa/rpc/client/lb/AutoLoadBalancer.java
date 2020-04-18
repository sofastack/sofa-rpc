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
package com.alipay.sofa.rpc.client.lb;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.client.AbstractLoadBalancer;
import com.alipay.sofa.rpc.client.LoadBalancer;
import com.alipay.sofa.rpc.client.LoadBalancerFactory;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.dynamic.DynamicConfigKeys;
import com.alipay.sofa.rpc.dynamic.DynamicConfigManager;
import com.alipay.sofa.rpc.dynamic.DynamicConfigManagerFactory;
import com.alipay.sofa.rpc.dynamic.DynamicHelper;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.List;

@Extension("auto")
public class AutoLoadBalancer extends AbstractLoadBalancer {

    /***
     * see com.alipay.sofa.rpc.config.ConsumerConfig#loadBalancer
     */
    protected static final String LOAD_BALANCER_KEY     = "loadBalancer";

    protected static final String DEFAULT_LOAD_BALANCER = "random";

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    public AutoLoadBalancer(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    protected ProviderInfo doSelect(SofaRequest request, List<ProviderInfo> providerInfos) {

        // 动态配置优先
        final String dynamicAlias = consumerConfig.getParameter(DynamicConfigKeys.DYNAMIC_ALIAS);
        if (StringUtils.isNotBlank(dynamicAlias)) {
            String dynamicLoadBalancer = null;
            DynamicConfigManager dynamicConfigManager = DynamicConfigManagerFactory.getDynamicManager(
                consumerConfig.getAppName(), dynamicAlias);
            if (dynamicConfigManager != null) {
                dynamicLoadBalancer = dynamicConfigManager.getConsumerServiceProperty(
                    request.getTargetServiceUniqueName(), LOAD_BALANCER_KEY);
                if (DynamicHelper.isNotDefault(dynamicLoadBalancer) && StringUtils.isNotBlank(dynamicLoadBalancer)) {
                    LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(consumerBootstrap,
                        dynamicLoadBalancer);
                    return loadBalancer.select(request, providerInfos);
                }
            }
        }
        LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(consumerBootstrap, DEFAULT_LOAD_BALANCER);
        return loadBalancer.select(request, providerInfos);
    }
}
