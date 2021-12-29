package com.alipay.sofa.rpc.codec.bolt;

import com.alipay.remoting.exception.DeserializationException;
import com.alipay.remoting.exception.SerializationException;
import com.alipay.remoting.rpc.protocol.RpcRequestCommand;
import com.alipay.remoting.rpc.protocol.RpcResponseCommand;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import javafx.collections.ObservableMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SofaRpcSerializationTest {

    @Test
    public void deserializeContent() {
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
    public void serializeContent() {
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
            Assert.assertEquals("RPC-020050008: 未找到 Serializer,type:[1]. , traceId=" + traceId + ", rpcId=" + rpcId, e.getMessage());
        }
        Assert.assertTrue(exp);
    }
}