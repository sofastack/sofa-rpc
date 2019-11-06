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
import com.alipay.sofa.rpc.ext.Extensible;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;

import java.util.List;

/**
 * 地址管理器
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extensible(singleton = false)
public abstract class AddressHolder implements ProviderInfoListener {

    /**
     * 服务消费者配置
     */
    protected ConsumerBootstrap consumerBootstrap;

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    protected AddressHolder(ConsumerBootstrap consumerBootstrap) {
        this.consumerBootstrap = consumerBootstrap;
    }

    /**
     * 得到某分组的服务列表，注意获取的地址列表最好是只读，不要随便修改
     *
     * @param groupName 服务列表的标签
     * @return 当前分组下的服务列表
     */
    public abstract List<ProviderInfo> getProviderInfos(String groupName);

    /**
     * 得到某服务分组
     *
     * @param groupName 服务列表的标签
     * @return 当前分组下的服务列表
     */
    public abstract ProviderGroup getProviderGroup(String groupName);

    /**
     * 得到全部服务端列表分组
     *
     * @return 全部服务列表分组
     */
    public abstract List<ProviderGroup> getProviderGroups();

    /**
     * 得到全部服务端大小
     *
     * @return 全部服务列表
     */
    public abstract int getAllProviderSize();
}
