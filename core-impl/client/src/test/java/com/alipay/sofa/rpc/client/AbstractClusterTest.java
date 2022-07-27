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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zdyu 2022/2/10
 */
public class AbstractClusterTest {

    private static AbstractCluster abstractCluster;

    @BeforeClass
    public static void beforeClass() {
        ConsumerConfig consumerConfig = new ConsumerConfig().setProtocol("test")
            .setBootstrap("test");
        ConsumerBootstrap consumerBootstrap = new ConsumerBootstrap(consumerConfig) {
            @Override
            public Object refer() {
                return null;
            }

            @Override
            public void unRefer() {

            }

            @Override
            public Object getProxyIns() {
                return null;
            }

            @Override
            public Cluster getCluster() {
                return null;
            }

            @Override
            public List<ProviderGroup> subscribe() {
                return null;
            }

            @Override
            public boolean isSubscribed() {
                return false;
            }
        };
        abstractCluster = new AbstractCluster(consumerBootstrap) {
            @Override
            protected SofaResponse doInvoke(SofaRequest msg) throws SofaRpcException {
                return null;
            }
        };
    }

    @Test
    public void testDestroyWithDestroyHook() {
        List<String> hookActionResult = new ArrayList<>(2);
        Destroyable.DestroyHook destroyHook = new Destroyable.DestroyHook() {
            @Override
            public void preDestroy() {
                hookActionResult.add("preDestroy");
            }

            @Override
            public void postDestroy() {
                hookActionResult.add("postDestroy");
            }
        };
        abstractCluster.destroy(destroyHook);
        Assert.assertEquals(2, hookActionResult.size());
        Assert.assertEquals("preDestroy", hookActionResult.get(0));
        Assert.assertEquals("postDestroy", hookActionResult.get(1));
    }

    @Test
    public void testResolveTimeout() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method resolveTimeoutMethod = AbstractCluster.class.getDeclaredMethod("resolveTimeout", SofaRequest.class,
            ConsumerConfig.class, ProviderInfo.class);
        resolveTimeoutMethod.setAccessible(true);

        SofaRequest sofaRequest = new SofaRequest();
        ConsumerConfig consumerConfig = new ConsumerConfig();
        ProviderInfo providerInfo = new ProviderInfo();
        Integer defaultTimeout = (Integer) resolveTimeoutMethod.invoke(abstractCluster, sofaRequest, consumerConfig,
            providerInfo);
        Assert.assertTrue(defaultTimeout == 3000);

        providerInfo.setStaticAttr(ProviderInfoAttrs.ATTR_TIMEOUT, "5000");
        Integer providerTimeout = (Integer) resolveTimeoutMethod.invoke(abstractCluster, sofaRequest, consumerConfig,
            providerInfo);
        Assert.assertTrue(providerTimeout == 5000);

        consumerConfig.setTimeout(2000);
        Integer consumerTimeout = (Integer) resolveTimeoutMethod.invoke(abstractCluster, sofaRequest, consumerConfig,
            providerInfo);
        Assert.assertTrue(consumerTimeout == 2000);

        sofaRequest.setTimeout(1000);
        Integer invokeTimeout = (Integer) resolveTimeoutMethod.invoke(abstractCluster, sofaRequest, consumerConfig,
            providerInfo);
        Assert.assertTrue(invokeTimeout == 1000);

    }
}