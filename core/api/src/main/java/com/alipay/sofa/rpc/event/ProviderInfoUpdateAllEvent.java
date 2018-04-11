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
import com.alipay.sofa.rpc.config.ConsumerConfig;

import java.util.List;

/**
 * ProviderInfoUpdateAllEvent
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProviderInfoUpdateAllEvent implements Event {
    private final ConsumerConfig      consumerConfig;
    private final List<ProviderGroup> oldProviderGroups;
    private final List<ProviderGroup> newProviderGroups;

    public ProviderInfoUpdateAllEvent(ConsumerConfig consumerConfig, List<ProviderGroup> oldProviderGroups,
                                      List<ProviderGroup> newProviderGroups) {
        this.consumerConfig = consumerConfig;
        this.oldProviderGroups = oldProviderGroups;
        this.newProviderGroups = newProviderGroups;
    }

    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    public List<ProviderGroup> getOldProviderGroups() {
        return oldProviderGroups;
    }

    public List<ProviderGroup> getNewProviderGroups() {
        return newProviderGroups;
    }
}
