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

import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.aft.impl.ServiceHorizontalRegulationStrategy;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class RegulationStrategyTest extends FaultBaseTest {

    @Test
    public void testIsDegradeEffective() {
        RegulationStrategy regulationStrategy = new ServiceHorizontalRegulationStrategy();
        FaultToleranceConfig config = new FaultToleranceConfig();
        config.setDegradeEffective(true);
        FaultToleranceConfigManager.putAppConfig(APP_NAME1, config);
        InvocationStatDimension invocationA = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip"),
            consumerConfig);
        MeasureResultDetail measureResultDetailA = new MeasureResultDetail(invocationA, MeasureState.ABNORMAL);
        Assert.assertTrue(regulationStrategy.isDegradeEffective(measureResultDetailA));
    }

    @Test
    public void testIsReachMaxDegradeIpCount() {
        FaultToleranceConfig configA = new FaultToleranceConfig();
        configA.setDegradeMaxIpCount(2);
        FaultToleranceConfigManager.putAppConfig(APP_NAME1, configA);

        FaultToleranceConfig configB = new FaultToleranceConfig();
        configB.setDegradeMaxIpCount(2);
        FaultToleranceConfigManager.putAppConfig(APP_NAME2, configB);

        RegulationStrategy regulationStrategy = new ServiceHorizontalRegulationStrategy();

        InvocationStatDimension invocation1 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig);
        MeasureResultDetail measureResultDetail1 = new MeasureResultDetail(invocation1, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail1));

        InvocationStatDimension invocation2 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip2"),
            consumerConfig);
        MeasureResultDetail measureResultDetail2 = new MeasureResultDetail(invocation2, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail2));

        /**同一应用，同一服务，不同ip数已达最大*/
        InvocationStatDimension invocation3 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip3"),
            consumerConfig);
        MeasureResultDetail measureResultDetail3 = new MeasureResultDetail(invocation3, MeasureState.ABNORMAL);
        Assert.assertTrue(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail3));

        /**同一应用，不同服务，还可以增加*/
        InvocationStatDimension invocation4 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig);
        MeasureResultDetail measureResultDetail4 = new MeasureResultDetail(invocation4, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail4));

        /**同一应用，同一服务，相同ip，还可以增加*/
        InvocationStatDimension invocation6 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig);
        MeasureResultDetail measureResultDetail6 = new MeasureResultDetail(invocation6, MeasureState.HEALTH);
        Assert.assertFalse(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail6));
    }

    @Test
    public void testIsExistDegradeList() {
        RegulationStrategy regulationStrategy = new ServiceHorizontalRegulationStrategy();

        /**降级列表为空，不存在*/
        InvocationStatDimension invocation1 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig);
        MeasureResultDetail measureResultDetail1 = new MeasureResultDetail(invocation1, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isExistInTheDegradeList(measureResultDetail1));

        /**加入到降级列表中*/
        InvocationStatDimension invocation2 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig);
        MeasureResultDetail measureResultDetail2 = new MeasureResultDetail(invocation2, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail2));

        /**相同应用，相同服务，相同ip，存在于降级列表中*/
        InvocationStatDimension invocation3 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig);
        MeasureResultDetail measureResultDetail3 = new MeasureResultDetail(invocation3, MeasureState.ABNORMAL);
        Assert.assertTrue(regulationStrategy.isExistInTheDegradeList(measureResultDetail3));

        /**相同应用，相同服务，不同ip，不存在于降级列表中*/
        InvocationStatDimension invocation4 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip2"),
            consumerConfig);
        MeasureResultDetail measureResultDetail4 = new MeasureResultDetail(invocation4, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isExistInTheDegradeList(measureResultDetail4));

        /**相同应用，不同服务，相同ip，存在于降级列表中*/
        InvocationStatDimension invocation5 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig2);
        MeasureResultDetail measureResultDetail5 = new MeasureResultDetail(invocation5, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isExistInTheDegradeList(measureResultDetail5));
    }

    @Test
    public void testRecoverIpCount() {
        RegulationStrategy regulationStrategy = new ServiceHorizontalRegulationStrategy();

        FaultToleranceConfig configA = new FaultToleranceConfig();
        configA.setDegradeMaxIpCount(2);
        FaultToleranceConfigManager.putAppConfig(APP_NAME1, configA);

        InvocationStatDimension invocation1 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig);
        MeasureResultDetail measureResultDetail1 = new MeasureResultDetail(invocation1, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail1));

        InvocationStatDimension invocation2 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip2"),
            consumerConfig);
        MeasureResultDetail measureResultDetail2 = new MeasureResultDetail(invocation2, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail2));

        /**已达上限*/
        InvocationStatDimension invocation3 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip3"),
            consumerConfig);
        MeasureResultDetail measureResultDetail3 = new MeasureResultDetail(invocation3, MeasureState.ABNORMAL);
        Assert.assertTrue(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail3));

        /**进行一次恢复*/
        InvocationStatDimension invocation4 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig);
        MeasureResultDetail measureResultDetail4 = new MeasureResultDetail(invocation4, MeasureState.ABNORMAL);
        measureResultDetail4.setRecoveredOriginWeight(true);
        regulationStrategy.removeFromDegradeList(measureResultDetail4);

        /**未达上限*/
        InvocationStatDimension invocation5 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip3"),
            consumerConfig);
        MeasureResultDetail measureResultDetail5 = new MeasureResultDetail(invocation5, MeasureState.ABNORMAL);
        Assert.assertFalse(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail5));

        /**已达上限*/
        InvocationStatDimension invocation6 = new InvocationStatDimension(ProviderHelper.toProviderInfo("ip1"),
            consumerConfig);
        MeasureResultDetail measureResultDetail6 = new MeasureResultDetail(invocation6, MeasureState.ABNORMAL);
        Assert.assertTrue(regulationStrategy.isReachMaxDegradeIpCount(measureResultDetail6));
    }
}