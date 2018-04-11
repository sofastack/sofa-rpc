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
import com.alipay.sofa.rpc.common.struct.PositiveAtomicCounter;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 负载均衡轮询算法，按方法级进行轮询，互不影响
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension("roundRobin")
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {

    private final ConcurrentMap<String, PositiveAtomicCounter> sequences = new ConcurrentHashMap<String, PositiveAtomicCounter>();

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    public RoundRobinLoadBalancer(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public ProviderInfo doSelect(SofaRequest request, List<ProviderInfo> providerInfos) {
        String key = getServiceKey(request); // 每个方法级自己轮询，互不影响
        int length = providerInfos.size(); // 总个数
        PositiveAtomicCounter sequence = sequences.get(key);
        if (sequence == null) {
            sequences.putIfAbsent(key, new PositiveAtomicCounter());
            sequence = sequences.get(key);
        }
        return providerInfos.get(sequence.getAndIncrement() % length);
    }

    private String getServiceKey(SofaRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getTargetAppName()).append("#")
            .append(request.getMethodName());
        return builder.toString();
    }

}
