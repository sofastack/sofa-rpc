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
import com.alipay.sofa.rpc.ext.Extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 支持多个tag的地址选择器
 * <p>
 * Created by zhanggeng on 2017/7/11.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
@Extension("multipleGroup")
public class MultipleGroupAddressHolder extends AddressHolder {

    /**
     * 配置的直连地址列表
     */
    private final ProviderGroup              directUrlGroup   = new ProviderGroup(RpcConstants.ADDRESS_DIRECT_GROUP);

    /**
     * 地址列表,key为group,
     */
    private final Map<String, ProviderGroup> allProviderInfos = new ConcurrentHashMap<String, ProviderGroup>();
    /**
     * 地址变化的锁
     */
    private ReentrantReadWriteLock           lock             = new ReentrantReadWriteLock();
    // 读锁，允许并发读
    private Lock                             rLock            = lock.readLock();
    // 写锁，写的时候不允许读
    private Lock                             wLock            = lock.writeLock();

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    protected MultipleGroupAddressHolder(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public List<ProviderInfo> getProviderInfos(String groupName) {

        rLock.lock();
        try {
            // 复制一份
            ProviderGroup providerGroup = getProviderGroup(groupName);
            if (providerGroup != null) {
                return new ArrayList<ProviderInfo>(providerGroup.getProviderInfos());
            } else {
                return new ArrayList<ProviderInfo>();
            }
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public ProviderGroup getProviderGroup(String groupName) {
        rLock.lock();
        try {
            if (RpcConstants.ADDRESS_DIRECT_GROUP.equals(groupName)) {
                return directUrlGroup;
            } else {
                ProviderGroup providerGroup = allProviderInfos.get(groupName);
                //fix for address is null.
                if (providerGroup == null) {
                    providerGroup = new ProviderGroup(groupName);
                }
                return providerGroup;
            }

        } finally {
            rLock.unlock();
        }
    }

    @Override
    public List<ProviderGroup> getProviderGroups() {
        rLock.lock();
        try {
            List<ProviderGroup> providerGroups = new ArrayList<ProviderGroup>(allProviderInfos.values());
            providerGroups.add(directUrlGroup);
            return providerGroups;
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public int getAllProviderSize() {
        rLock.lock();
        try {
            List<String> groups;
            groups = new ArrayList<String>(allProviderInfos.keySet());

            int size = 0;
            for (String group : groups) {
                size += getProviderInfos(group).size();
            }

            size += directUrlGroup.size();
            return size;
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
            ProviderGroup originalProviderGroup = getProviderGroup(providerGroup.getName());

            if (originalProviderGroup != null) {
                originalProviderGroup.addAll(providerGroup.getProviderInfos());
            } else {
                if (RpcConstants.ADDRESS_DIRECT_GROUP.equals(providerGroup.getName())) {
                    directUrlGroup.setProviderInfos(providerGroup.getProviderInfos());
                } else {
                    allProviderInfos.put(providerGroup.getName(), providerGroup);
                }
            }
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
            ProviderGroup originalProviderGroup = getProviderGroup(providerGroup.getName());

            if (originalProviderGroup != null) {
                originalProviderGroup.removeAll(providerGroup.getProviderInfos());
            }
        } finally {
            wLock.unlock();
        }
    }

    @Override
    public void updateProviders(ProviderGroup providerGroup) {
        wLock.lock();
        try {
            if (RpcConstants.ADDRESS_DIRECT_GROUP.equals(providerGroup.getName())) {
                directUrlGroup.setProviderInfos(providerGroup.getProviderInfos());
            } else {
                allProviderInfos.put(providerGroup.getName(), providerGroup);
            }
        } finally {
            wLock.unlock();
        }
    }

    @Override
    public void updateAllProviders(List<ProviderGroup> providerGroups) {

        wLock.lock();
        try {

            //need to clear all without direct,keep zone info
            for (Map.Entry<String, ProviderGroup> entry : allProviderInfos.entrySet()) {
                entry.setValue(new ProviderGroup(entry.getKey()));
            }

            for (ProviderGroup providerGroup : providerGroups) {
                if (RpcConstants.ADDRESS_DIRECT_GROUP.equals(providerGroup.getName())) {
                    directUrlGroup.setProviderInfos(providerGroup.getProviderInfos());
                } else {
                    allProviderInfos.put(providerGroup.getName(), providerGroup);
                }
            }

        } finally {
            wLock.unlock();
        }
    }
}
