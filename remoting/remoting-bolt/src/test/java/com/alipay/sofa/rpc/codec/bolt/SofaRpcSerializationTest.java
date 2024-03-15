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
package com.alipay.sofa.rpc.codec.bolt;

import static org.mockito.ArgumentMatchers.anyByte;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.alipay.remoting.InvokeContext;
import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;
import com.alipay.sofa.rpc.codec.Serializer;
import com.alipay.sofa.rpc.codec.SerializerFactory;
import com.alipay.sofa.rpc.common.RemotingConstants;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.transport.AbstractByteBuf;
import com.google.protobuf.Empty;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

public class SofaRpcSerializationTest {
    static class SimpleResponse {
        public SimpleResponse() {
        }

        private Object realResponse;

        public Object getRealResponse() {
            return realResponse;
        }

        public void setRealResponse(Object realResponse) {
            this.realResponse = realResponse;
        }
    }

    static class SimpleSerializer implements Serializer {
        @Override
        public AbstractByteBuf encode(Object object, Map<String, String> context) throws SofaRpcException {
            throw new UnsupportedOperationException(object.getClass().getName());
        }

        @Override
        public Object decode(AbstractByteBuf data, Class clazz, Map<String, String> context) throws SofaRpcException {
            try {
                if (MessageLite.class.isAssignableFrom(clazz)) {
                    MessageLite defaultInstance = Internal.getDefaultInstance(clazz);
                    return defaultInstance.getParserForType().parseFrom(data.array());
                }
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public void decode(AbstractByteBuf data, Object template, Map<String, String> context) throws SofaRpcException {
            if (template instanceof SimpleResponse) {
                Object realResponse = decode(data, Empty.class, context);
                ((SimpleResponse) template).setRealResponse(realResponse);
                return;
            }
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testSerializeEmptyRequest() throws DeserializationException {
        SofaRpcSerialization serialization = new SofaRpcSerialization();

        try (MockedStatic<SerializerFactory> mocked = Mockito.mockStatic(SerializerFactory.class)) {
            mocked.when(() -> SerializerFactory.getSerializer(anyByte())).thenReturn(new SimpleSerializer());

            RpcResponseCommand response = new RpcResponseCommand();
            response.setResponseClass(SimpleResponse.class.getName());
            response.setContent(null);

            Assert.assertTrue(serialization.deserializeContent(response, new InvokeContext()));
            SimpleResponse simpleResponse = (SimpleResponse) response.getResponseObject();
            Assert.assertEquals(Empty.getDefaultInstance(), simpleResponse.getRealResponse());
        }
    }

    @Test
    public void serializeResponseContent() {
        String traceId = "traceId";
        String rpcId = "rpcId";
        RpcInternalContext.getContext().setAttachment("_trace_id", traceId);
        RpcInternalContext.getContext().setAttachment("_span_id", rpcId);

        RpcResponseCommand command = new RpcResponseCommand();

        SofaRpcSerialization sofaRpcSerialization = new SofaRpcSerialization();
        boolean exp = false;
        try {
            sofaRpcSerialization.serializeContent(command);
        } catch (SerializationException e) {
            exp = true;
            Assert.assertTrue(e.getMessage().contains("traceId=" + traceId + ", rpcId=" + rpcId));
        }
        Assert.assertTrue(exp);
    }

    @Test
    public void testSetRequestPropertiesWithHeaderInfo() {
        String service1 = "testService1";
        String service2 = "testService2";
        Map<String, String> headerMap = new HashMap();
        headerMap.put(RemotingConstants.HEAD_TARGET_SERVICE, service1);
        SofaRequest sofaRequest = new SofaRequest();
        SofaRpcSerialization sofaRpcSerialization = new SofaRpcSerialization();
        sofaRpcSerialization.setRequestPropertiesWithHeaderInfo(headerMap, sofaRequest);
        Assert.assertEquals(service1, sofaRequest.getTargetServiceUniqueName());
        headerMap.put(RemotingConstants.HEAD_SERVICE, service2);
        sofaRpcSerialization.setRequestPropertiesWithHeaderInfo(headerMap, sofaRequest);
        Assert.assertEquals(service2, sofaRequest.getTargetServiceUniqueName());
    }

    @Test
    public void testParseRequestHeader(){
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("testKey1","testValue1");
        headerMap.put("rpc_trace_context.sofaTraceId", "traceId");
        headerMap.put("rpc_trace_context.sofaRpcId", "rpcId");
        SofaRpcSerialization sofaRpcSerialization = new SofaRpcSerialization();
        SofaRequest sofaRequest = new SofaRequest();
        sofaRequest.addRequestProp("testKey1", "testValue11");
        sofaRequest.addRequestProp("testKey2", "testValue2");
        sofaRpcSerialization.parseRequestHeader(headerMap, sofaRequest);
        Assert.assertEquals("testValue1", sofaRequest.getRequestProp("testKey1"));
        Assert.assertEquals("testValue2", sofaRequest.getRequestProp("testKey2"));
        Object traceMap = sofaRequest.getRequestProp(RemotingConstants.RPC_TRACE_NAME);
        Assert.assertTrue(traceMap instanceof Map);
        Assert.assertEquals("traceId",((Map)traceMap).get("sofaTraceId"));
        Assert.assertEquals("rpcId",((Map)traceMap).get("sofaRpcId"));
    }
}