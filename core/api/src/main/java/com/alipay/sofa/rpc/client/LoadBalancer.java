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
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.ext.Extensible;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

/**
 * 负载均衡器：从一堆Provider列表里选出一个
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
@ThreadSafe
public abstract class LoadBalancer {

    /**
     * 服务消费者配置
     */
    protected final ConsumerBootstrap consumerBootstrap;

    /**
     * 服务消费者配置
     */
    protected final ConsumerConfig    consumerConfig;

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    public LoadBalancer(ConsumerBootstrap consumerBootstrap) {
        this.consumerBootstrap = consumerBootstrap;
        this.consumerConfig = consumerBootstrap != null ? consumerBootstrap.getConsumerConfig() : null;
    }

    /**
     * 得到服务消费者配置
     *
     * @return the consumer config
     */
    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    /**
     * 选择服务
     *
     * @param request       本次调用（可以得到类名，方法名，方法参数，参数值等）
     * @param providerInfos <b>当前可用</b>的服务Provider列表
     * @return 选择其中一个Provider
     * @throws SofaRpcException rpc异常
     */
    public abstract ProviderInfo select(SofaRequest request, List<ProviderInfo> providerInfos)
        throws SofaRpcException;
}
