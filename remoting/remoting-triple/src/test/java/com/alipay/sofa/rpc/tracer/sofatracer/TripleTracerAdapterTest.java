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
package com.alipay.sofa.rpc.tracer.sofatracer;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.server.triple.TripleHeadKeys;
import io.grpc.Metadata;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.alipay.sofa.rpc.common.RemotingConstants.HEAD_TARGET_SERVICE;

/**
 * @author Even
 * @date 2022/12/29 1:53 PM
 */
public class TripleTracerAdapterTest {

    @Test
    public void testBeforeSend() {
        SofaRequest sofaRequest = new SofaRequest();
        sofaRequest.setTargetServiceUniqueName("targetService1");
        sofaRequest.addRequestProp("triple.header.key", "triple.header.value");
        Map map = new HashMap<String, String>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        sofaRequest.addRequestProp("triple.header.object", map);
        sofaRequest.addRequestProp(HEAD_TARGET_SERVICE, "targetService2");
        ConsumerConfig consumerConfig = new ConsumerConfig();
        Metadata metadata = new Metadata();
        TripleTracerAdapter.beforeSend(sofaRequest, consumerConfig, metadata);
        Assert.assertEquals("targetService2", metadata.get(TripleHeadKeys.getKey(HEAD_TARGET_SERVICE)));
        Assert.assertEquals("triple.header.value", metadata.get(TripleHeadKeys.getKey("triple.header.key")));
        Assert.assertEquals("value1", metadata.get(TripleHeadKeys.getKey("triple.header.object.key1")));
        Assert.assertEquals("value2", metadata.get(TripleHeadKeys.getKey("triple.header.object.key2")));
    }

}