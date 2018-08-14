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
package com.alipay.sofa.rpc.client.aft;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.aft.impl.ServiceExceptionInvocationStat;
import com.alipay.sofa.rpc.common.struct.ConcurrentHashSet;
import com.alipay.sofa.rpc.config.ConsumerConfig;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 调控入口类
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class InvocationStatFactory {

    /**
     * 调控统计和结果的映射
     */
    static final ConcurrentMap<InvocationStatDimension, InvocationStat> ALL_STATS = new ConcurrentHashMap<InvocationStatDimension, InvocationStat>();

    /**
     * Listeners of InvocationStat
     */
    static final ConcurrentHashSet<InvocationStatListener>              LISTENERS = new ConcurrentHashSet<InvocationStatListener>();

    /**
     * 得到调用统计器
     *
     * @param consumerConfig 接口信息
     * @param providerInfo   请求信息
     * @return 调用统计器，如果不符合统计条件则返回空
     */
    public static InvocationStat getInvocationStat(ConsumerConfig consumerConfig, ProviderInfo providerInfo) {
        String appName = consumerConfig.getAppName();
        if (appName == null) {
            return null;
        }
        // 应用开启单机故障摘除功能
        if (FaultToleranceConfigManager.isRegulationEffective(appName)) {
            return getInvocationStat(new InvocationStatDimension(providerInfo, consumerConfig));
        }
        return null;
    }

    /**
     * 根据Invocation获取InvocationStat
     * 该Invocation对应的InvocationStat会在被第一次获取（也就是刚被创建的时候）时被放入到Regulation中进行能力的持续调控。
     *
     * @param statDimension InvocationStatDimension
     * @return InvocationStat
     */
    public static InvocationStat getInvocationStat(InvocationStatDimension statDimension) {
        InvocationStat invocationStat = ALL_STATS.get(statDimension);
        if (invocationStat == null) {
            invocationStat = new ServiceExceptionInvocationStat(statDimension);
            InvocationStat old = ALL_STATS.putIfAbsent(statDimension, invocationStat);
            if (old != null) {
                invocationStat = old;
            }
            for (InvocationStatListener listener : LISTENERS) {
                listener.onAddInvocationStat(invocationStat);
            }
        }
        return invocationStat;
    }

    /**
     * Remove dimension stat by dimension
     *
     * @param statDimension InvocationStatDimension
     */
    public static void removeInvocationStat(InvocationStatDimension statDimension) {
        InvocationStat invocationStat = ALL_STATS.remove(statDimension);
        if (invocationStat != null) {
            for (InvocationStatListener listener : LISTENERS) {
                listener.onRemoveInvocationStat(invocationStat);
            }
        }
    }

    /**
     * Remove dimension stat by stat
     *
     * @param stat InvocationStat
     */
    public static void removeInvocationStat(InvocationStat stat) {
        removeInvocationStat(stat.getDimension());
    }

    /**
     * Remove dimension stat by consumerConfig and providerInfo
     *
     * @param consumerConfig ConsumerConfig
     * @param providerInfo   ProviderInfo
     */
    public static void removeInvocationStat(ConsumerConfig consumerConfig, ProviderInfo providerInfo) {
        removeInvocationStat(new InvocationStatDimension(providerInfo, consumerConfig));
    }

    /**
     * 对批量InvocationStat快照进行一个更新
     *
     * @param snapshots InvocationStat
     */
    public static void updateInvocationStats(List<InvocationStat> snapshots) {
        for (InvocationStat snapshot : snapshots) {
            getInvocationStat(snapshot.getDimension()).update(snapshot);
        }
    }

    /**
     * Destroy 
     */
    public static void destroy() {
        ALL_STATS.clear();
        LISTENERS.clear();
    }

    /**
     * Listener of invocation stat
     */
    public interface InvocationStatListener {

        /**
         * do something when add invocation stat
         *
         * @param invocationStat InvocationStat
         */
        public void onAddInvocationStat(InvocationStat invocationStat);

        /**
         * do something when remove invocation stat
         *
         * @param invocationStat InvocationStat
         */
        public void onRemoveInvocationStat(InvocationStat invocationStat);
    }

    /**
     * Add InvocationStatListener implement
     *
     * @param listener InvocationStatListener
     */
    public static void addListener(InvocationStatListener listener) {
        LISTENERS.add(listener);
    }

    /**
     * Remove InvocationStatListener implement
     *
     * @param listener InvocationStatListener
     */
    public static void removeListener(InvocationStatListener listener) {
        LISTENERS.remove(listener);
    }
}