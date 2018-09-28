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
import com.alipay.sofa.rpc.ext.Extensible;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.transport.ClientTransport;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * ConnectionHolder SPI
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
@ThreadSafe
public abstract class ConnectionHolder implements Initializable, Destroyable, ProviderInfoListener {

    /**
     * 服务消费者配置
     */
    protected ConsumerBootstrap consumerBootstrap;

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    protected ConnectionHolder(ConsumerBootstrap consumerBootstrap) {
        this.consumerBootstrap = consumerBootstrap;
    }

    /**
     * 关闭所有长连接
     *
     * @param destroyHook DestroyHook
     */
    public abstract void closeAllClientTransports(DestroyHook destroyHook);

    /**
     * 存活的连接
     *
     * @return the alive connections
     */
    @Deprecated
    public abstract ConcurrentMap<ProviderInfo, ClientTransport> getAvailableConnections();

    /**
     * 存活的全部provider
     *
     * @return all alive providers
     */
    @Deprecated
    public abstract List<ProviderInfo> getAvailableProviders();

    /**
     * 根据provider查找存活的ClientTransport
     *
     * @param providerInfo the provider
     * @return the client transport
     */
    public abstract ClientTransport getAvailableClientTransport(ProviderInfo providerInfo);

    /**
     * 是否没有存活的的provider
     *
     * @return all alive providers
     */
    public abstract boolean isAvailableEmpty();

    /**
     * 获取当前的Provider列表（包括连上和没连上的）
     *
     * @return 当前的Provider列表 set
     */
    @Deprecated
    public abstract Collection<ProviderInfo> currentProviderList();

    /**
     * 设置为不可用
     *
     * @param providerInfo Provider
     * @param transport    连接
     */
    public abstract void setUnavailable(ProviderInfo providerInfo, ClientTransport transport);

}
