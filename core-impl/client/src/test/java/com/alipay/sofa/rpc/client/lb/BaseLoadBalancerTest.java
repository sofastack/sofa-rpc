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
package com.alipay.sofa.rpc.client.lb;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public abstract class BaseLoadBalancerTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseLoadBalancerTest.class);

    protected List<ProviderInfo> buildSameWeightProviderList(int size) {
        List<ProviderInfo> aliveConnections = new ArrayList<ProviderInfo>();
        for (int i = 0; i < size; i++) {
            ProviderInfo provider = new ProviderInfo();
            provider.setHost("127.0.0.2");
            provider.setPort(9000 + i);

            aliveConnections.add(provider);
        }

        return aliveConnections;
    }

    protected List<ProviderInfo> buildDiffWeightProviderList(int size) {
        List<ProviderInfo> aliveConnections = new ArrayList<ProviderInfo>();
        for (int i = 0; i < size; i++) {
            ProviderInfo provider = new ProviderInfo();
            provider.setHost("127.0.0.2");
            provider.setPort(9000 + i);
            provider.setWeight(i * 100); // 权重异常乘以100

            aliveConnections.add(provider);
        }

        return aliveConnections;
    }
}
