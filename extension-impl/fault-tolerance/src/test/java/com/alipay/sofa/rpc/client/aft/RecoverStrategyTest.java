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
public class RecoverStrategyTest extends FaultBaseServiceTest {

    @Test
    public void test() throws InterruptedException {

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
        InvocationStatDimension statDimension = new InvocationStatDimension(providerInfo, consumerConfig);
        InvocationStat invocationStat = InvocationStatFactory.getInvocationStat(statDimension);
        Assert.assertEquals(10, delayGetCount(invocationStat, 10));
        Assert.assertEquals(2, invocationStat.getExceptionCount());
        Assert.assertTrue(0.2D == invocationStat.getExceptionRate());

        //第一个窗口结束
        Assert.assertEquals(50, delayGetWeight(providerInfo, 50, 42));

        /**test recover normal*/
        config.setLeastWindowCount(6L);
        config.setLeastWindowExceptionRateMultiple(2D);
        config.setWeightRecoverRate(1.8D);

        for (int i = 0; i < 8; i++) {
            try {
                helloService.sayHello("liangen");
            } catch (Exception e) {
                LOGGER.info("超时");
            }
        }

        Assert.assertEquals(8, delayGetCount(invocationStat, 8));
        Assert.assertTrue(invocationStat.getExceptionCount() == 1);
        Assert.assertTrue(invocationStat.getExceptionRate() == 0.13);

        //第二个窗口结束
        Assert.assertTrue(90 == delayGetWeight(providerInfo, 90, 62));

        /**test recover max*/
        for (int i = 0; i < 7; i++) {
            try {
                helloService.sayHello("liangen");
            } catch (Exception e) {
                LOGGER.info("超时");
            }
        }

        Assert.assertEquals(7, delayGetCount(invocationStat, 7));
        Assert.assertTrue(invocationStat.getExceptionCount() == 2);
        Assert.assertTrue(invocationStat.getExceptionRate() == 0.29);

        Thread.sleep(2100);//第三个窗口结束
        Assert.assertTrue(100 == delayGetWeight(providerInfo, 100, 42));
        InvocationStatFactory.removeInvocationStat(invocationStat);
    }
}