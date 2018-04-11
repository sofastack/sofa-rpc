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

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.base.Initializable;
import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extensible;
import com.alipay.sofa.rpc.filter.FilterChain;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 客户端，封装了集群模式、长连接管理、服务路由、负载均衡等抽象类
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
@ThreadSafe
public abstract class Cluster implements Invoker, ProviderInfoListener, Initializable, Destroyable {

    /**
     * 服务端消费者启动器
     */
    protected final ConsumerBootstrap consumerBootstrap;

    /**
     * 配置
     */
    protected final ConsumerConfig    consumerConfig;

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务端消费者启动器
     */
    public Cluster(ConsumerBootstrap consumerBootstrap) {
        this.consumerBootstrap = consumerBootstrap;
        this.consumerConfig = consumerBootstrap.getConsumerConfig();
    }

    /**
     * 调用远程地址发送消息
     *
     * @param providerInfo 服务提供者信息
     * @param request      请求
     * @return 状态
     * @throws SofaRpcException RPC异常
     */
    public abstract SofaResponse sendMsg(ProviderInfo providerInfo, SofaRequest request) throws SofaRpcException;

    /**
     * 是否可用
     *
     * @return Is cluster available
     */
    public abstract boolean isAvailable();

    /**
     * 状态变化通知
     *
     * @param originalState Origin state
     */
    public abstract void checkStateChange(boolean originalState);

    /**
     * 地址管理器
     *
     * @return Current AddressHolder
     */
    public abstract AddressHolder getAddressHolder();

    /**
     * 连接管理器
     *
     * @return Current ConnectionHolder
     */
    public abstract ConnectionHolder getConnectionHolder();

    /**
     * 过滤器链
     *
     * @return Current FilterChain
     */
    public abstract FilterChain getFilterChain();

    /**
     * 路由器链
     *
     * @return Current RouterChain
     */
    public abstract RouterChain getRouterChain();
}