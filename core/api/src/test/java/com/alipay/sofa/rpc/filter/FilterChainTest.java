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
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class FilterChainTest {

    @Test
    public void buildProviderChain() {
        ConsumerConfig config = new ConsumerConfig();
        ArrayList<Filter> list = new ArrayList<Filter>();
        config.setFilter(Collections.singletonList("testChainFilter0"));
        list.add(new TestChainFilter1());
        list.add(new TestChainFilter2());
        list.add(new TestChainFilter3());
        list.add(new TestChainFilter4());
        list.add(new ExcludeFilter("-testChainFilter5"));
        config.setFilterRef(list);

        FilterChain chain = FilterChain.buildConsumerChain(config, new TestChainFilterInvoker(config));

        SofaRequest request = new SofaRequest();
        request.setMethodArgs(new String[] { "xxx" });
        request.setInvokeType("sync");
        String result = (String) chain.invoke(request).getAppResponse();
        Assert.assertEquals("xxx_q0_q2_q4_s4_s2_s0", result);

        request = new SofaRequest();
        request.setMethodArgs(new String[] { "xxx" });
        request.setInvokeType("callback");
        SofaResponse response = chain.invoke(request);
        chain.onAsyncResponse(config, request, response, null);
        result = (String) response.getAppResponse();
        Assert.assertEquals("xxx_q0_q2_q4_a4_a2_a0", result);
    }
}
