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

import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import org.junit.Assert;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public class SofaRpcSerializationTest {

    @Test
    public void deserializeRequestContent() {
        String traceId = "traceId";
        String rpcId = "rpcId";
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("rpc_trace_context.sofaTraceId", traceId);
        headerMap.put("rpc_trace_context.sofaRpcId", rpcId);

        RpcRequestCommand command = new RpcRequestCommand();
        command.setRequestHeader(headerMap);
        SofaRpcSerialization sofaRpcSerialization = new SofaRpcSerialization();
        boolean exp = false;
        try {
            sofaRpcSerialization.deserializeContent(command);
        } catch (DeserializationException e) {
            exp = true;
            Assert.assertEquals("Content of request is null, traceId=" + traceId + ", rpcId=" + rpcId, e.getMessage());
        }
        Assert.assertTrue(exp);
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
}