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
package com.alipay.sofa.rpc.registry;

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.base.Initializable;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.ext.Extensible;

import java.util.List;

/**
 * Registry SPI
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
public abstract class Registry implements Initializable, Destroyable {

    /**
     * 注册中心服务配置
     */
    protected RegistryConfig registryConfig;

    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected Registry(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    /**
     * 启动
     *
     * @return is started
     */
    public abstract boolean start();

    /**
     * 注册服务提供者
     *
     * @param config Provider配置
     */
    public abstract void register(ProviderConfig config);

    /**
     * 反注册服务提供者
     *
     * @param config Provider配置
     */
    public abstract void unRegister(ProviderConfig config);

    /**
     * 反注册服务提供者
     *
     * @param configs Provider配置
     */
    public abstract void batchUnRegister(List<ProviderConfig> configs);

    /**
     * 订阅服务列表
     *
     * @param config Consumer配置
     * @return 当前Provider列表，返回null表示未同步获取到地址
     */
    public abstract List<ProviderGroup> subscribe(ConsumerConfig config);

    /**
     * 反订阅服务调用者相关配置
     *
     * @param config Consumer配置
     */
    public abstract void unSubscribe(ConsumerConfig config);

    /**
     * 反订阅服务调用者相关配置
     *
     * @param configs Consumer配置
     */
    public abstract void batchUnSubscribe(List<ConsumerConfig> configs);

    @Override
    public void destroy(DestroyHook hook) {
        if (hook != null) {
            hook.preDestroy();
        }
        destroy();
        if (hook != null) {
            hook.postDestroy();
        }
    }

}
