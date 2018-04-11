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
import com.alipay.sofa.rpc.client.ProviderStatus;

/**
 * Weight manager of provider info.
 * 
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProviderInfoWeightManager {

    /**
     * Recover weight of provider info
     *
     * @param providerInfo ProviderInfo
     * @param weight       recovered weight
     * @return is recover success 
     */
    public static boolean recoverWeight(ProviderInfo providerInfo, int weight) {
        providerInfo.setStatus(ProviderStatus.RECOVERING);
        providerInfo.setWeight(weight);
        return true;
    }

    /**
     * Degrade weight of provider info
     *
     * @param providerInfo ProviderInfo
     * @param weight       degraded weight
     * @return is degrade success
     */
    public static boolean degradeWeight(ProviderInfo providerInfo, int weight) {
        providerInfo.setStatus(ProviderStatus.DEGRADED);
        providerInfo.setWeight(weight);
        return true;
    }

    /**
     * Recover weight of provider info, and set default status
     *
     * @param providerInfo ProviderInfo
     * @param originWeight origin weight
     */
    public static void recoverOriginWeight(ProviderInfo providerInfo, int originWeight) {
        providerInfo.setStatus(ProviderStatus.AVAILABLE);
        providerInfo.setWeight(originWeight);
    }

    /**
     *
     * @return weight
     */
    public static int getWeight(ProviderInfo providerInfo) {
        return providerInfo == null ? -1 : providerInfo.getWeight();
    }
}