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
package com.alipay.sofa.rpc.test.bootstrap.triple;

import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.bootstrap.Bootstraps;
import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.bootstrap.triple.TripleClientProxyInvoker;
import com.alipay.sofa.rpc.bootstrap.triple.TripleConsumerBootstrap;
import com.alipay.sofa.rpc.common.MockMode;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Even
 * @date 2022/11/22 2:32 PM
 */
public class TripleConsumerBootstrapTest {

    @Test
    public void test() {
        ConsumerConfig<GenericService> consumerConfig = new ConsumerConfig();
        consumerConfig.setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE);
        consumerConfig.setInterfaceId("testInterfaceId");
        consumerConfig.setUniqueId("uniqueId");
        consumerConfig.setGeneric(true);
        consumerConfig.setMock(true);
        consumerConfig.setMockMode(MockMode.LOCAL);
        ConsumerBootstrap consumerBootstrap = Bootstraps.from(consumerConfig);
        consumerBootstrap.refer();
        Assert.assertTrue(consumerBootstrap instanceof TripleConsumerBootstrap);

        TripleClientProxyInvoker tripleClientProxyInvoker = new TripleClientProxyInvoker(consumerBootstrap);
        SofaRequest sofaRequest = new SofaRequest();
        tripleClientProxyInvoker.invoke(sofaRequest);
        Assert.assertEquals("testInterfaceId:1.0:uniqueId", sofaRequest.getTargetServiceUniqueName());
    }

}
