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
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.HashUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    /**
     * 选择器
     */
    private static class Selector {

        /**
         * The Hashcode.
         */
        private final int                         hashcode;

        /**
         * The Interface id.
         */
        private final String                      interfaceId;

        /**
         * The Method name.
         */
        private final String                      method;

        /**
         * 虚拟节点
         */
        private final TreeMap<Long, ProviderInfo> virtualNodes;

        /**
         * Instantiates a new Selector .
         *
         * @param interfaceId the interface id
         * @param method      the method
         * @param actualNodes the actual nodes
         */
        public Selector(String interfaceId, String method, List<ProviderInfo> actualNodes) {
            this(interfaceId, method, actualNodes, actualNodes.hashCode());
        }

        /**
         * Instantiates a new Selector.
         *
         * @param interfaceId the interface id
         * @param method      the method
         * @param actualNodes the actual nodes
         * @param hashcode    the hashcode
         */
        public Selector(String interfaceId, String method, List<ProviderInfo> actualNodes, int hashcode) {
            this.interfaceId = interfaceId;
            this.method = method;
            this.hashcode = hashcode;
            // 创建虚拟节点环 （provider创建虚拟节点数 =  真实节点权重 * 32）
            this.virtualNodes = new TreeMap<Long, ProviderInfo>();
            // 设置越大越慢，精度越高
            int num = 32;
            for (ProviderInfo providerInfo : actualNodes) {
                for (int i = 0; i < num * providerInfo.getWeight() / 4; i++) {
                    byte[] digest = HashUtils.messageDigest(providerInfo.getHost() + providerInfo.getPort() + i);
                    for (int h = 0; h < 4; h++) {
                        long m = HashUtils.hash(digest, h);
                        virtualNodes.put(m, providerInfo);
                    }
                }
            }
        }

        /**
         * Select provider.
         *
         * @param request the request
         * @return the provider
         */
        public ProviderInfo select(SofaRequest request) {
            String key = buildKeyOfHash(request.getMethodArgs());
            byte[] digest = HashUtils.messageDigest(key);
            return selectForKey(HashUtils.hash(digest, 0));
        }

        /**
         * 获取第一参数作为hash的key
         *
         * @param args the args
         * @return the string
         */
        private String buildKeyOfHash(Object[] args) {
            if (CommonUtils.isEmpty(args)) {
                return StringUtils.EMPTY;
            } else {
                return StringUtils.toString(args[0]);
            }
        }

        /**
         * Select for key.
         *
         * @param hash the hash
         * @return the provider
         */
        private ProviderInfo selectForKey(long hash) {
            Map.Entry<Long, ProviderInfo> entry = virtualNodes.ceilingEntry(hash);
            if (entry == null) {
                entry = virtualNodes.firstEntry();
            }
            return entry.getValue();
        }

        /**
         * Gets hash code.
         *
         * @return the hash code
         */
        public int getHashCode() {
            return hashcode;
        }
    }

}
