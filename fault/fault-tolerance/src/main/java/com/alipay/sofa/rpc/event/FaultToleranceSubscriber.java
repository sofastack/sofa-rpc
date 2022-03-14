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
package com.alipay.sofa.rpc.event;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.aft.FaultToleranceConfigManager;
import com.alipay.sofa.rpc.client.aft.InvocationStat;
import com.alipay.sofa.rpc.client.aft.InvocationStatFactory;
import com.alipay.sofa.rpc.config.ConsumerConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Subscriber client receive event for adaptive fault tolerance
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class FaultToleranceSubscriber extends Subscriber {

    /**
     * 事件订阅者
     */
    public FaultToleranceSubscriber() {
        super(false);
    }

    @Override
    public void onEvent(Event originEvent) {
        Class eventClass = originEvent.getClass();

        if (eventClass == ClientSyncReceiveEvent.class) {
            if (!FaultToleranceConfigManager.isEnable()) {
                return;
            }
            // 同步结果
            ClientSyncReceiveEvent event = (ClientSyncReceiveEvent) originEvent;
            ConsumerConfig consumerConfig = event.getConsumerConfig();
            ProviderInfo providerInfo = event.getProviderInfo();
            InvocationStat result = InvocationStatFactory.getInvocationStat(consumerConfig, providerInfo);
            if (result != null) {
                result.invoke();
                Throwable t = event.getThrowable();
                if (t != null) {
                    result.catchException(t);
                }
            }
        } else if (eventClass == ClientAsyncReceiveEvent.class) {
            if (!FaultToleranceConfigManager.isEnable()) {
                return;
            }
            // 异步结果
            ClientAsyncReceiveEvent event = (ClientAsyncReceiveEvent) originEvent;
            ConsumerConfig consumerConfig = event.getConsumerConfig();
            ProviderInfo providerInfo = event.getProviderInfo();
            InvocationStat result = InvocationStatFactory.getInvocationStat(consumerConfig, providerInfo);
            if (result != null) {
                result.invoke();
                Throwable t = event.getThrowable();
                if (t != null) {
                    result.catchException(t);
                }
            }
        } else if (eventClass == ProviderInfoRemoveEvent.class) {
            ProviderInfoRemoveEvent event = (ProviderInfoRemoveEvent) originEvent;
            ConsumerConfig consumerConfig = event.getConsumerConfig();
            ProviderGroup providerGroup = event.getProviderGroup();
            if (!ProviderHelper.isEmpty(providerGroup)) {
                for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                    InvocationStatFactory.removeInvocationStat(consumerConfig, providerInfo);
                }
            }
        } else if (eventClass == ProviderInfoUpdateEvent.class) {
            ProviderInfoUpdateEvent event = (ProviderInfoUpdateEvent) originEvent;
            ConsumerConfig consumerConfig = event.getConsumerConfig();
            List<ProviderInfo> add = new ArrayList<ProviderInfo>();
            List<ProviderInfo> remove = new ArrayList<ProviderInfo>();
            ProviderHelper.compareGroup(event.getOldProviderGroup(), event.getNewProviderGroup(), add, remove);
            for (ProviderInfo providerInfo : remove) {
                InvocationStatFactory.removeInvocationStat(consumerConfig, providerInfo);
            }
        } else if (eventClass == ProviderInfoUpdateAllEvent.class) {
            ProviderInfoUpdateAllEvent event = (ProviderInfoUpdateAllEvent) originEvent;
            ConsumerConfig consumerConfig = event.getConsumerConfig();
            List<ProviderInfo> add = new ArrayList<ProviderInfo>();
            List<ProviderInfo> remove = new ArrayList<ProviderInfo>();
            ProviderHelper.compareGroups(event.getOldProviderGroups(), event.getNewProviderGroups(), add, remove);
            for (ProviderInfo providerInfo : remove) {
                InvocationStatFactory.removeInvocationStat(consumerConfig, providerInfo);
            }
        }
    }

}
