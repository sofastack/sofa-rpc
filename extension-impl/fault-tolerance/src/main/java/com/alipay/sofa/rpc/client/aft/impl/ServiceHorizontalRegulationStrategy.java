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
package com.alipay.sofa.rpc.client.aft.impl;

import com.alipay.sofa.rpc.client.aft.FaultToleranceConfigManager;
import com.alipay.sofa.rpc.client.aft.InvocationStatDimension;
import com.alipay.sofa.rpc.client.aft.MeasureResultDetail;
import com.alipay.sofa.rpc.client.aft.RegulationStrategy;
import com.alipay.sofa.rpc.common.struct.ConcurrentHashSet;
import com.alipay.sofa.rpc.ext.Extension;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
@Extension("serviceHorizontal")
public class ServiceHorizontalRegulationStrategy implements RegulationStrategy {

    @Override
    public boolean isDegradeEffective(MeasureResultDetail measureResultDetail) {
        InvocationStatDimension statDimension = measureResultDetail.getInvocationStatDimension();
        return FaultToleranceConfigManager.isDegradeEffective(statDimension.getAppName());
    }

    /**
     * Key（应用，服务）降级的不同ip列表
     */
    protected final ConcurrentMap<String, ConcurrentHashSet<String>> appServiceDegradeIps = new ConcurrentHashMap<String, ConcurrentHashSet<String>>();

    protected ConcurrentHashSet<String> getDegradeProviders(String key) {
        ConcurrentHashSet<String> ips = appServiceDegradeIps.get(key);
        if (ips == null) {
            ips = new ConcurrentHashSet<String>();
            ConcurrentHashSet<String> old = appServiceDegradeIps.putIfAbsent(key, ips);
            if (old != null) {
                ips = old;
            }
        }
        return ips;
    }

    private final Lock ipsLock = new ReentrantLock();

    @Override
    public boolean isReachMaxDegradeIpCount(MeasureResultDetail measureResultDetail) {
        InvocationStatDimension statDimension = measureResultDetail.getInvocationStatDimension();
        ConcurrentHashSet<String> ips = getDegradeProviders(statDimension.getDimensionKey());

        String ip = statDimension.getIp();
        if (ips.contains(ip)) {
            return false;
        } else {
            int degradeMaxIpCount = FaultToleranceConfigManager.getDegradeMaxIpCount(statDimension.getAppName());
            ipsLock.lock();
            try {
                if (ips.size() < degradeMaxIpCount) {
                    ips.add(ip);
                    return false;
                } else {
                    return true;
                }
            } finally {
                ipsLock.unlock();
            }
        }
    }

    @Override
    public boolean isExistInTheDegradeList(MeasureResultDetail measureResultDetail) {
        InvocationStatDimension statDimension = measureResultDetail.getInvocationStatDimension();
        ConcurrentHashSet<String> ips = getDegradeProviders(statDimension.getDimensionKey());
        return ips != null && ips.contains(statDimension.getIp());
    }

    @Override
    public void removeFromDegradeList(MeasureResultDetail measureResultDetail) {
        if (measureResultDetail.isRecoveredOriginWeight()) {
            InvocationStatDimension statDimension = measureResultDetail.getInvocationStatDimension();
            getDegradeProviders(statDimension.getDimensionKey()).remove(statDimension.getIp());
        }
    }
}