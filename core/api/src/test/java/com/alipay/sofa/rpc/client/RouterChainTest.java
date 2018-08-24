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

import com.alipay.sofa.rpc.bootstrap.Bootstraps;
import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.client.router.TestChainRouter1;
import com.alipay.sofa.rpc.client.router.TestChainRouter2;
import com.alipay.sofa.rpc.client.router.TestChainRouter3;
import com.alipay.sofa.rpc.client.router.TestChainRouter4;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RouterChainTest {

    @Test
    public void buildProviderChain() {
        ConsumerConfig config = new ConsumerConfig();
        config.setBootstrap("test");
        ArrayList<Router> list = new ArrayList<Router>();
        config.setRouter(Arrays.asList("testChainRouter0", "-testChainRouter8"));
        list.add(new TestChainRouter1());
        list.add(new TestChainRouter2());
        list.add(new TestChainRouter3());
        list.add(new TestChainRouter4());
        list.add(new ExcludeRouter("-testChainRouter5"));
        config.setRouterRef(list);

        ConsumerBootstrap consumerBootstrap = Bootstraps.from(config);
        RouterChain chain = RouterChain.buildConsumerChain(consumerBootstrap);

        // build test data
        SofaRequest request = new SofaRequest();
        request.setMethodArgs(new String[] { "xxx" });
        request.setInvokeType("sync");
        List<ProviderInfo> providerInfos = new ArrayList<ProviderInfo>();
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.setHost("127.0.0.1");
        providerInfo.setPort(12200);
        providerInfos.add(providerInfo);

        chain.route(request, providerInfos);
        Assert.assertEquals("r0>r7>r2>r4",
            RpcInternalContext.getContext().getAttachment(RpcConstants.INTERNAL_KEY_ROUTER_RECORD));
    }
}