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
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.Selector;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 景竹 2018/8/13 since 5.5.0
 */
@Extension("weightConsistentHash")
public class WeightConsistentHashLoadBalancer extends AbstractLoadBalancer {

    /**
     * {interface#method :  selector}
     */
    private final ConcurrentHashMap<String, Selector> selectorCache = new ConcurrentHashMap<String, Selector>();

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    public WeightConsistentHashLoadBalancer(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public ProviderInfo doSelect(SofaRequest request, List<ProviderInfo> providerInfos) {
        String interfaceId = request.getInterfaceName();
        String method = request.getMethodName();
        String key = interfaceId + "#" + method;
        // 判断是否同样的服务列表
        int hashcode = providerInfos.hashCode();
        Selector selector = selectorCache.get(key);
        // 原来没有
        if (selector == null ||
            // 或者服务列表已经变化
            selector.getHashCode() != hashcode) {
            selector = new Selector(interfaceId, method, providerInfos, hashcode);
            selectorCache.put(key, selector);
        }
        return selector.select(request);
    }

}
