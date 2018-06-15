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

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.List;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("test")
public class TestRegistry extends Registry {
    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected TestRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public void register(ProviderConfig config) {

    }

    @Override
    public void unRegister(ProviderConfig config) {

    }

    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {

    }

    @Override
    public List<ProviderGroup> subscribe(ConsumerConfig config) {
        return null;
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {

    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void init() {

    }
}
