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
import com.alipay.sofa.rpc.ext.Extension;

import java.util.List;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("test")
public class TestAddressHolder extends AddressHolder {
    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    protected TestAddressHolder(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public List<ProviderInfo> getProviderInfos(String groupName) {
        return null;
    }

    @Override
    public ProviderGroup getProviderGroup(String groupName) {
        return null;
    }

    @Override
    public List<ProviderGroup> getProviderGroups() {
        return null;
    }

    @Override
    public int getAllProviderSize() {
        return 0;
    }

    @Override
    public void addProvider(ProviderGroup providerGroup) {

    }

    @Override
    public void removeProvider(ProviderGroup providerGroup) {

    }

    @Override
    public void updateProviders(ProviderGroup providerGroup) {

    }

    @Override
    public void updateAllProviders(List<ProviderGroup> providerGroups) {

    }
}
