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

import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.server.triple.TripleHeadKeys;
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
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
        TripleTracerAdapter.beforeSend(sofaRequest, consumerConfig, metadata, null);
        Assert.assertEquals("targetService2", metadata.get(TripleHeadKeys.getKey(HEAD_TARGET_SERVICE)));
        Assert.assertEquals("triple.header.value", metadata.get(TripleHeadKeys.getKey("triple.header.key")));
        Assert.assertEquals("value1", metadata.get(TripleHeadKeys.getKey("triple.header.object.key1")));
        Assert.assertEquals("value2", metadata.get(TripleHeadKeys.getKey("triple.header.object.key2")));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testServerReceivedParseBaggage() throws Exception {
        // 通过反射开启 baggage
        Field baggageField = RpcInvokeContext.class.getDeclaredField("BAGGAGE_ENABLE");
        baggageField.setAccessible(true);
        boolean originEnable = (boolean) baggageField.get(null);
        try {
            baggageField.set(null, true);

            // 构造 gRPC Metadata，模拟客户端 flatCopyTo 后的展平 baggage header
            Metadata requestHeaders = new Metadata();
            String baggagePrefix = RemotingConstants.RPC_REQUEST_BAGGAGE + ".";
            requestHeaders.put(TripleHeadKeys.getKey(baggagePrefix + "key1"), "value1");
            requestHeaders.put(TripleHeadKeys.getKey(baggagePrefix + "key2"), "value2");
            requestHeaders.put(TripleHeadKeys.HEAD_KEY_TARGET_SERVICE, "com.test.TestService");

            // Mock ServerCall
            ServerCall<Object, Object> serverCall = Mockito.mock(ServerCall.class);
            MethodDescriptor<Object, Object> methodDescriptor = MethodDescriptor.newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("com.test.TestService/testMethod")
                .setRequestMarshaller(Mockito.mock(MethodDescriptor.Marshaller.class))
                .setResponseMarshaller(Mockito.mock(MethodDescriptor.Marshaller.class))
                .build();
            Mockito.when(serverCall.getMethodDescriptor()).thenReturn(methodDescriptor);
            Mockito.when(serverCall.getAttributes()).thenReturn(Attributes.EMPTY);

            // Mock ServerServiceDefinition
            ServiceDescriptor serviceDescriptor = ServiceDescriptor.newBuilder("com.test.TestService").build();
            ServerServiceDefinition serverServiceDefinition = ServerServiceDefinition.builder(serviceDescriptor)
                .build();

            SofaRequest sofaRequest = new SofaRequest();
            TripleTracerAdapter.serverReceived(sofaRequest, serverServiceDefinition, serverCall, requestHeaders);

            // 验证 baggage 已正确放入 SofaRequest.requestProps
            Map<String, String> baggage = (Map<String, String>) sofaRequest
                .getRequestProp(RemotingConstants.RPC_REQUEST_BAGGAGE);
            Assert.assertNotNull("baggage should be parsed from triple headers", baggage);
            Assert.assertEquals("value1", baggage.get("key1"));
            Assert.assertEquals("value2", baggage.get("key2"));
            Assert.assertEquals(2, baggage.size());
        } finally {
            baggageField.set(null, originEnable);
            RpcInvokeContext.removeContext();
        }
    }

    @Test
    public void testGetUserId() {
        Metadata metadata = new Metadata();
        Assert.assertNull(TripleTracerAdapter.getUserId(metadata));
        metadata.put(TripleHeadKeys.HEAD_KEY_UNIT_INFO, "test");
        Assert.assertNull(TripleTracerAdapter.getUserId(metadata));
        metadata.put(TripleHeadKeys.HEAD_KEY_UNIT_INFO, "{\"userid\":\"99\"}");
        Assert.assertEquals("99", TripleTracerAdapter.getUserId(metadata));
    }

}