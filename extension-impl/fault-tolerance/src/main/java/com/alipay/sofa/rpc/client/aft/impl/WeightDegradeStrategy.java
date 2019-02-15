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

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderStatus;
import com.alipay.sofa.rpc.client.aft.FaultToleranceConfigManager;
import com.alipay.sofa.rpc.client.aft.InvocationStatDimension;
import com.alipay.sofa.rpc.client.aft.MeasureResultDetail;
import com.alipay.sofa.rpc.client.aft.ProviderInfoWeightManager;
import com.alipay.sofa.rpc.common.utils.CalculateUtils;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
@Extension("weight")
public class WeightDegradeStrategy extends LogPrintDegradeStrategy {

    /** Logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(WeightDegradeStrategy.class);

    @Override
    public void degrade(MeasureResultDetail measureResultDetail) {

        super.degrade(measureResultDetail);

        if (measureResultDetail.isLogOnly()) {
            return;
        }

        InvocationStatDimension statDimension = measureResultDetail.getInvocationStatDimension();
        String appName = statDimension.getAppName();

        ProviderInfo providerInfo = statDimension.getProviderInfo();
        // if provider is removed or provider is warming up
        if (providerInfo == null || providerInfo.getStatus() == ProviderStatus.WARMING_UP) {
            return;
        }
        int currentWeight = ProviderInfoWeightManager.getWeight(providerInfo);
        double weightDegradeRate = FaultToleranceConfigManager.getWeightDegradeRate(appName);
        int degradeLeastWeight = FaultToleranceConfigManager.getDegradeLeastWeight(appName);

        int degradeWeight = CalculateUtils.multiply(currentWeight, weightDegradeRate);
        degradeWeight = degradeWeight < degradeLeastWeight ? degradeLeastWeight : degradeWeight;

        // degrade weight of this provider info
        boolean success = ProviderInfoWeightManager.degradeWeight(providerInfo, degradeWeight);
        if (success && LOGGER.isInfoEnabled(appName)) {
            LOGGER.infoWithApp(appName, "the weight was degraded. serviceUniqueName:["
                + statDimension.getService() + "],ip:["
                + statDimension.getIp() + "],origin weight:["
                + currentWeight + "],degraded weight:["
                + degradeWeight + "].");
        }
    }
}
