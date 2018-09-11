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
import java.util.concurrent.ConcurrentMap;

/**
 * 一致性hash算法，同样的请求（第一参数）会打到同样的节点
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extension("consistentHash")
public class ConsistentHashLoadBalancer extends AbstractLoadBalancer {

    /**
     * {interface#method : selector}
     */
    private ConcurrentMap<String, Selector> selectorCache = new ConcurrentHashMap<String, Selector>();

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    public ConsistentHashLoadBalancer(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public ProviderInfo doSelect(SofaRequest request, List<ProviderInfo> providerInfos) {
        String interfaceId = request.getInterfaceName();
        String method = request.getMethodName();
        String key = interfaceId + "#" + method;
        int hashcode = providerInfos.hashCode(); // 判断是否同样的服务列表
        Selector selector = selectorCache.get(key);
        if (selector == null // 原来没有
            ||
            selector.getHashCode() != hashcode) { // 或者服务列表已经变化
            selector = new Selector(interfaceId, method, providerInfos, hashcode);
            selectorCache.put(key, selector);
        }
        return selector.select(request);
    }
}
