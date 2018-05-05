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
package com.alipay.sofa.rpc.bootstrap;

import com.alipay.sofa.rpc.client.Cluster;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.ext.Extensible;

import java.util.List;

/**
 * 引用服务的包装类，包括具体的启动后的对象
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
public abstract class ConsumerBootstrap<T> {

    /**
     * 服务消费者配置
     */
    protected final ConsumerConfig<T> consumerConfig;

    /**
     * 构造函数
     *
     * @param consumerConfig 服务消费者配置
     */
    protected ConsumerBootstrap(ConsumerConfig<T> consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    /**
     * 得到服务消费者配置
     *
     * @return 服务消费者配置
     */
    public ConsumerConfig<T> getConsumerConfig() {
        return consumerConfig;
    }

    /**
     * 调用一个服务
     *
     * @return 代理类
     */
    public abstract T refer();

    /**
     * 取消调用一个服务
     */
    public abstract void unRefer();

    /**
     * 拿到代理类
     *
     * @return 代理类
     */
    public abstract T getProxyIns();

    /**
     * 得到调用集群
     *
     * @return 服务端集群
     */
    public abstract Cluster getCluster();

    /**
     * 订阅服务列表
     *
     * @return 服务列表
     */
    public abstract List<ProviderGroup> subscribe();

    /**
     * 是否已经订阅完毕
     *
     * @return 是否订阅完毕
     */
    public abstract boolean isSubscribed();
}
