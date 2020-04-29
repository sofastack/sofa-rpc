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
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class DegradeStrategyTest extends FaultBaseServiceTest {

    @Test
    public void testAll() throws InterruptedException {
        FaultToleranceConfig config = new FaultToleranceConfig();

        config.setDegradeEffective(true);
        config.setRegulationEffective(true);
        config.setTimeWindow(3);
        config.setLeastWindowCount(10);
        config.setWeightDegradeRate(0.5D);
        config.setDegradeLeastWeight(30);
        config.setLeastWindowExceptionRateMultiple(1D);

        FaultToleranceConfigManager.putAppConfig(APP_NAME1, config);

        /**test degrade normal*/
        for (int i = 0; i < 10; i++) {
            try {
                helloService.sayHello("liangen");
            } catch (Exception e) {
                LOGGER.info("超时");
            }
        }
        final ProviderInfo providerInfo = getProviderInfoByHost(consumerConfig, "127.0.0.1");
        InvocationStatDimension invocation = new InvocationStatDimension(providerInfo, consumerConfig);
        InvocationStat invocationStat = InvocationStatFactory.getInvocationStat(invocation);
        Assert.assertEquals(10, delayGetCount(invocationStat, 10));
        Assert.assertTrue(invocationStat.getExceptionCount() == 2);
        Assert.assertTrue(invocationStat.getExceptionRate() == 0.2);

        // 第一个窗口结束
        Assert.assertTrue(50 == delayGetWeight(providerInfo, 50, 40));

        /**test degrade at lest*/
        FaultToleranceConfigManager.getConfig(APP_NAME1).setLeastWindowCount(5L);
        for (int i = 0; i < 5; i++) {
            try {
                helloService.sayHello("liangen");
            } catch (Exception e) {
                LOGGER.info("超时");
            }
        }

        Assert.assertEquals(5, delayGetCount(invocationStat, 5));
        Assert.assertTrue(invocationStat.getExceptionCount() == 1);
        Assert.assertTrue(invocationStat.getExceptionRate() == 0.2);

        // 第二个窗口结束
        Assert.assertTrue(30 == delayGetWeight(providerInfo, 30, 60));

        InvocationStatFactory.removeInvocationStat(invocationStat);
    }

    @Test
    public void testLeastWindowCount() throws InterruptedException {

        FaultToleranceConfig config = new FaultToleranceConfig();

        config.setDegradeEffective(true);
        config.setRegulationEffective(true);
        config.setTimeWindow(3);
        config.setLeastWindowCount(5);
        config.setWeightDegradeRate(0.5D);
        config.setDegradeLeastWeight(30);
        config.setLeastWindowExceptionRateMultiple(1D);

        FaultToleranceConfigManager.putAppConfig(APP_NAME1, config);

        /**test degrade normal*/
        for (int i = 0; i < 10; i++) {
            try {
                helloService.sayHello("liangen");
            } catch (Exception e) {
                LOGGER.info("超时");
            }
        }
        final ProviderInfo providerInfo = getProviderInfoByHost(consumerConfig, "127.0.0.1");
        InvocationStatDimension invocation = new InvocationStatDimension(providerInfo, consumerConfig);
        InvocationStat invocationStat = InvocationStatFactory.getInvocationStat(invocation);
        Assert.assertEquals(10, delayGetCount(invocationStat, 10));
        Assert.assertTrue(invocationStat.getExceptionCount() == 2);
        Assert.assertTrue(invocationStat.getExceptionRate() == 0.2);
        // 第一个窗口结束
        Assert.assertTrue(50 == delayGetWeight(providerInfo, 50, 40));

        /**test InvocationLeastWindowCount => 5*/
        FaultToleranceConfigManager.getConfig(APP_NAME1).setWeightDegradeRate(0.7D);
        for (int i = 0; i < 5; i++) {
            try {
                helloService.sayHello("liangen");
            } catch (Exception e) {
                LOGGER.info("超时");
            }
        }
        Assert.assertEquals(5, delayGetCount(invocationStat, 5));
        Assert.assertTrue(invocationStat.getExceptionCount() == 1);
        Assert.assertTrue(invocationStat.getExceptionRate() == 0.2);

        // 第二个窗口结束
        Assert.assertEquals(35, delayGetWeight(providerInfo, 35, 100));

        InvocationStatFactory.removeInvocationStat(invocationStat);
    }

    @Test
    public void testDegradeEffective() throws InterruptedException {

        FaultToleranceConfig config = new FaultToleranceConfig();

        config.setDegradeEffective(true);
        config.setRegulationEffective(true);
        config.setTimeWindow(3);
        config.setLeastWindowCount(1);
        config.setWeightDegradeRate(0.5D);
        config.setDegradeLeastWeight(30);
        config.setLeastWindowExceptionRateMultiple(1D);

        FaultToleranceConfigManager.putAppConfig(APP_NAME1, config);

        /**降级开关开启，权重降级*/
        for (int i = 0; i < 9; i++) {
            try {
                helloService.sayHello("liangen");
            } catch (Exception e) {
                LOGGER.info("超时");
            }
        }
        final ProviderInfo providerInfo = getProviderInfoByHost(consumerConfig, "127.0.0.1");
        InvocationStatDimension invocation = new InvocationStatDimension(providerInfo, consumerConfig);
        final InvocationStat invocationStat = InvocationStatFactory.ALL_STATS.get(invocation);
        Assert.assertEquals(9, delayGetCount(invocationStat, 9));
        Assert.assertEquals(1, invocationStat.getExceptionCount());
        Assert.assertEquals(invocationStat.getExceptionRate(), 0.11, 0);

        // 第一个窗口结束
        Assert.assertTrue(50 == delayGetWeight(providerInfo, 50, 50));

        /**降级开关关闭，权重不再降级*/
        FaultToleranceConfigManager.getConfig(APP_NAME1).setDegradeEffective(false);
        for (int i = 0; i < 1; i++) {
            try {
                helloService.sayHello("liangen");
            } catch (Exception e) {
                LOGGER.info("超时");
            }
        }
        Assert.assertEquals(1, delayGetCount(invocationStat, 1));
        Assert.assertTrue(invocationStat.getExceptionCount() == 1);
        Assert.assertTrue(invocationStat.getExceptionRate() == 1);

        //第二个窗口结束
        Thread.sleep(3100);
        int appWeight2 = ProviderInfoWeightManager.getWeight(providerInfo);
        Assert.assertEquals(50, appWeight2);

        InvocationStatFactory.removeInvocationStat(invocationStat);
    }
}