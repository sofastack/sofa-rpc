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
package com.alipay.sofa.rpc.module;

import com.alipay.sofa.rpc.client.aft.Regulator;
import com.alipay.sofa.rpc.client.aft.impl.TimeWindowRegulator;
import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;

import com.alipay.sofa.rpc.event.ClientAsyncReceiveEvent;
import com.alipay.sofa.rpc.event.ClientSyncReceiveEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.FaultToleranceSubscriber;
import com.alipay.sofa.rpc.event.ProviderInfoRemoveEvent;
import com.alipay.sofa.rpc.event.ProviderInfoUpdateAllEvent;
import com.alipay.sofa.rpc.event.ProviderInfoUpdateEvent;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;

/**
 * FaultToleranceModule
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("fault-tolerance")
public class FaultToleranceModule implements Module {

    /**
     * 事件订阅
     */
    private FaultToleranceSubscriber subscriber;

    /**
     * Regulator
     */
    private Regulator                regulator = new TimeWindowRegulator();

    @Override
    public boolean needLoad() {
        return true;
    }

    @Override
    public void install() {
        subscriber = new FaultToleranceSubscriber();
        EventBus.register(ClientSyncReceiveEvent.class, subscriber);
        EventBus.register(ClientAsyncReceiveEvent.class, subscriber);
        EventBus.register(ProviderInfoRemoveEvent.class, subscriber);
        EventBus.register(ProviderInfoUpdateEvent.class, subscriber);
        EventBus.register(ProviderInfoUpdateAllEvent.class, subscriber);

        String regulatorAlias = RpcConfigs.getOrDefaultValue(RpcOptions.AFT_REGULATOR, "timeWindow");
        regulator = ExtensionLoaderFactory.getExtensionLoader(Regulator.class).getExtension(regulatorAlias);
        regulator.init();
    }

    @Override
    public void uninstall() {
        if (subscriber != null) {
            EventBus.unRegister(ClientSyncReceiveEvent.class, subscriber);
            EventBus.unRegister(ClientAsyncReceiveEvent.class, subscriber);
            EventBus.unRegister(ProviderInfoRemoveEvent.class, subscriber);
            EventBus.unRegister(ProviderInfoUpdateEvent.class, subscriber);
            EventBus.unRegister(ProviderInfoUpdateAllEvent.class, subscriber);
        }
        regulator.destroy();
    }

    /**
     * Get regulator
     *
     * @return Regulator
     */
    public Regulator getRegulator() {
        return regulator;
    }
}
