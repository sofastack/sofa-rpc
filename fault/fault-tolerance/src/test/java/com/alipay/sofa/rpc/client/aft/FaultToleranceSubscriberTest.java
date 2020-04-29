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

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.core.exception.SofaTimeOutException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.event.ClientAsyncReceiveEvent;
import com.alipay.sofa.rpc.event.ClientSyncReceiveEvent;
import com.alipay.sofa.rpc.event.FaultToleranceSubscriber;
import com.alipay.sofa.rpc.event.ProviderInfoRemoveEvent;
import com.alipay.sofa.rpc.event.ProviderInfoUpdateAllEvent;
import com.alipay.sofa.rpc.event.ProviderInfoUpdateEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class FaultToleranceSubscriberTest extends FaultBaseTest {
    @Test
    public void onEvent() throws Exception {

        ProviderInfo providerInfo = ProviderHelper.toProviderInfo("127.0.0.1");
        FaultToleranceSubscriber subscriber = new FaultToleranceSubscriber();

        subscriber.onEvent(new ClientSyncReceiveEvent(consumerConfig, providerInfo,
            new SofaRequest(), new SofaResponse(), null));
        InvocationStat stat = InvocationStatFactory.getInvocationStat(consumerConfig, providerInfo);
        Assert.assertNull(stat);

        FaultToleranceConfig config = new FaultToleranceConfig();
        config.setRegulationEffective(true);
        FaultToleranceConfigManager.putAppConfig(APP_NAME1, config);

        subscriber.onEvent(new ClientSyncReceiveEvent(consumerConfig, providerInfo,
            new SofaRequest(), new SofaResponse(), null));
        stat = InvocationStatFactory.getInvocationStat(consumerConfig, providerInfo);
        Assert.assertTrue(stat.getInvokeCount() == 1);

        subscriber.onEvent(new ClientAsyncReceiveEvent(consumerConfig, providerInfo,
            new SofaRequest(), new SofaResponse(), null));
        Assert.assertTrue(stat.getInvokeCount() == 2);

        subscriber.onEvent(new ClientSyncReceiveEvent(consumerConfig, providerInfo,
            new SofaRequest(), null, new SofaTimeOutException("")));
        Assert.assertTrue(stat.getExceptionCount() == 1);

        subscriber.onEvent(new ClientAsyncReceiveEvent(consumerConfig, providerInfo,
            new SofaRequest(), null, new SofaTimeOutException("")));
        Assert.assertTrue(stat.getExceptionCount() == 2);

        Assert.assertTrue(stat.getExceptionRate() == 0.5d);
    }

    @Test
    public void onProviderEvent() {
        FaultToleranceConfig config = new FaultToleranceConfig();
        config.setRegulationEffective(true);
        FaultToleranceConfigManager.putAppConfig(APP_NAME1, config);

        ProviderInfo providerInfo1 = ProviderHelper.toProviderInfo("127.0.0.1");
        ProviderInfo providerInfo2 = ProviderHelper.toProviderInfo("127.0.0.2");
        ProviderInfo providerInfo3 = ProviderHelper.toProviderInfo("127.0.0.3");
        ProviderInfo providerInfo4 = ProviderHelper.toProviderInfo("127.0.0.4");
        ProviderInfo providerInfo5 = ProviderHelper.toProviderInfo("127.0.0.5");
        FaultToleranceSubscriber subscriber = new FaultToleranceSubscriber();

        subscriber.onEvent(new ClientSyncReceiveEvent(consumerConfig, providerInfo1,
            new SofaRequest(), new SofaResponse(), null));
        subscriber.onEvent(new ClientSyncReceiveEvent(consumerConfig, providerInfo2,
            new SofaRequest(), new SofaResponse(), null));
        subscriber.onEvent(new ClientSyncReceiveEvent(consumerConfig, providerInfo3,
            new SofaRequest(), new SofaResponse(), null));
        subscriber.onEvent(new ClientSyncReceiveEvent(consumerConfig, providerInfo4,
            new SofaRequest(), new SofaResponse(), null));

        Assert.assertTrue(InvocationStatFactory.ALL_STATS.size() == 4);

        subscriber.onEvent(new ProviderInfoRemoveEvent(consumerConfig,
            new ProviderGroup("x", Arrays.asList(ProviderHelper.toProviderInfo("127.0.0.1")))));
        Assert.assertTrue(InvocationStatFactory.ALL_STATS.size() == 3);

        subscriber.onEvent(new ProviderInfoUpdateEvent(consumerConfig,
            new ProviderGroup("x", Arrays.asList(
                ProviderHelper.toProviderInfo("127.0.0.2"),
                ProviderHelper.toProviderInfo("127.0.0.3"),
                ProviderHelper.toProviderInfo("127.0.0.4"))),
            new ProviderGroup("x", Arrays.asList(
                ProviderHelper.toProviderInfo("127.0.0.2"),
                ProviderHelper.toProviderInfo("127.0.0.4"),
                ProviderHelper.toProviderInfo("127.0.0.5")))
            ));
        Assert.assertTrue(InvocationStatFactory.ALL_STATS.size() == 2);

        subscriber.onEvent(new ClientSyncReceiveEvent(consumerConfig, providerInfo5,
            new SofaRequest(), new SofaResponse(), null));
        Assert.assertTrue(InvocationStatFactory.ALL_STATS.size() == 3);

        subscriber.onEvent(new ProviderInfoUpdateAllEvent(consumerConfig,
            Arrays.asList(new ProviderGroup("x", Arrays.asList(
                ProviderHelper.toProviderInfo("127.0.0.2"),
                ProviderHelper.toProviderInfo("127.0.0.4"),
                ProviderHelper.toProviderInfo("127.0.0.5")))),
            Arrays.asList(new ProviderGroup("x", Arrays.asList(
                ProviderHelper.toProviderInfo("127.0.0.1"),
                ProviderHelper.toProviderInfo("127.0.0.4"))))
            ));
        Assert.assertTrue(InvocationStatFactory.ALL_STATS.size() == 1);
    }

}