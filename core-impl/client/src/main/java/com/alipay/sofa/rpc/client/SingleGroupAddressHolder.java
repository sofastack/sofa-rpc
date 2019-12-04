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
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.struct.ConcurrentHashSet;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 只支持单个分组的地址选择器（额外存一个直连分组）
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("singleGroup")
public class SingleGroupAddressHolder extends AddressHolder {

    /**
     * 配置的直连地址列表
     */
    protected ProviderGroup        directUrlGroup;
    /**
     * 注册中心来的地址列表
     */
    protected ProviderGroup        registryGroup;

    /**
     * 地址变化的锁
     */
    private ReentrantReadWriteLock lock  = new ReentrantReadWriteLock();
    // 读锁，允许并发读
    private Lock                   rLock = lock.readLock();
    // 写锁，写的时候不允许读
    private Lock                   wLock = lock.writeLock();

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    protected SingleGroupAddressHolder(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
        directUrlGroup = new ProviderGroup(RpcConstants.ADDRESS_DIRECT_GROUP);
        registryGroup = new ProviderGroup();
    }

    @Override
    public List<ProviderInfo> getProviderInfos(String groupName) {
        rLock.lock();
        try {
            // 复制一份
            return new ArrayList<ProviderInfo>(getProviderGroup(groupName).getProviderInfos());
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public ProviderGroup getProviderGroup(String groupName) {
        rLock.lock();
        try {
            return RpcConstants.ADDRESS_DIRECT_GROUP.equals(groupName) ? directUrlGroup
                : registryGroup;
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public List<ProviderGroup> getProviderGroups() {
        rLock.lock();
        try {
            List<ProviderGroup> list = new ArrayList<ProviderGroup>();
            list.add(registryGroup);
            list.add(directUrlGroup);
            return list;
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public int getAllProviderSize() {
        rLock.lock();
        try {
            return directUrlGroup.size() + registryGroup.size();
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public void addProvider(ProviderGroup providerGroup) {
        if (ProviderHelper.isEmpty(providerGroup)) {
            return;
        }
        wLock.lock();
        try {
            getProviderGroup(providerGroup.getName()).addAll(providerGroup.getProviderInfos());
        } finally {
            wLock.unlock();
        }
    }

    @Override
    public void removeProvider(ProviderGroup providerGroup) {
        if (ProviderHelper.isEmpty(providerGroup)) {
            return;
        }
        wLock.lock();
        try {
            getProviderGroup(providerGroup.getName()).removeAll(providerGroup.getProviderInfos());
        } finally {
            wLock.unlock();
        }
    }

    @Override
    public void updateProviders(ProviderGroup providerGroup) {
        wLock.lock();
        try {
            getProviderGroup(providerGroup.getName())
                .setProviderInfos(new ArrayList<ProviderInfo>(providerGroup.getProviderInfos()));
        } finally {
            wLock.unlock();
        }
    }

    @Override
    public void updateAllProviders(List<ProviderGroup> providerGroups) {
        ConcurrentHashSet<ProviderInfo> tmpDirectUrl = new ConcurrentHashSet<ProviderInfo>();
        ConcurrentHashSet<ProviderInfo> tmpRegistry = new ConcurrentHashSet<ProviderInfo>();
        for (ProviderGroup providerGroup : providerGroups) {
            if (!ProviderHelper.isEmpty(providerGroup)) {
                if (RpcConstants.ADDRESS_DIRECT_GROUP.equals(providerGroup.getName())) {
                    tmpDirectUrl.addAll(providerGroup.getProviderInfos());
                } else {
                    tmpRegistry.addAll(providerGroup.getProviderInfos());
                }
            }
        }
        wLock.lock();
        try {
            this.directUrlGroup.setProviderInfos(new ArrayList<ProviderInfo>(tmpDirectUrl));
            this.registryGroup.setProviderInfos(new ArrayList<ProviderInfo>(tmpRegistry));
        } finally {
            wLock.unlock();
        }
    }
}