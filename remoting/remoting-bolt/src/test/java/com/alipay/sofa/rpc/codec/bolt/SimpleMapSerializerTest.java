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

import com.alipay.sofa.rpc.common.struct.UnsafeByteArrayOutputStream;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.alipay.sofa.rpc.common.RemotingConstants.RPC_TRACE_NAME;
import static com.alipay.sofa.rpc.common.RemotingConstants.RPC_ID_KEY;
import static com.alipay.sofa.rpc.common.RemotingConstants.TRACE_ID_KEY;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SimpleMapSerializerTest {
    @Test
    public void encode() throws Exception {
        SimpleMapSerializer simpleMapSerializer = new SimpleMapSerializer();
        Map<String, String> map = null;
        byte[] bs = simpleMapSerializer.encode(map);
        Assert.assertEquals(bs, null);

        map = new HashMap<String, String>();
        bs = simpleMapSerializer.encode(map);
        Assert.assertEquals(bs, null);

        map.put("1", "2");
        map.put("", "x");
        map.put("a", "");
        map.put("b", null);
        bs = simpleMapSerializer.encode(map);
        Assert.assertEquals(28, bs.length);

        Map<String, String> map1 = simpleMapSerializer.decode(bs);
        Assert.assertNotNull(map1);
        Assert.assertEquals(3, map1.size());
        Assert.assertEquals("2", map1.get("1"));
        Assert.assertEquals("x", map1.get(""));
        Assert.assertEquals("", map1.get("a"));
        Assert.assertNull(map1.get("b"));

        map1 = simpleMapSerializer.decode(null);
        Assert.assertNotNull(map1);
        Assert.assertEquals(0, map1.size());

        map1 = simpleMapSerializer.decode(new byte[0]);
        Assert.assertNotNull(map1);
        Assert.assertEquals(0, map1.size());
    }

    @Test
    public void decode() throws Exception {
    }

    @Test
    public void writeString() throws Exception {
    }

    @Test
    public void readString() throws Exception {
    }

    @Test
    public void readInt() throws Exception {
    }

    @Test
    public void testUTF8() throws Exception {
        SimpleMapSerializer mapSerializer = new SimpleMapSerializer();
        String s = "test";
        // utf-8 和 gbk  英文是一样的
        Assert.assertArrayEquals(s.getBytes("UTF-8"), s.getBytes("GBK"));

        Map<String, String> map = new HashMap<String, String>();
        map.put("11", "22");
        map.put("222", "333");
        byte[] bs = mapSerializer.encode(map);
        Map newmap = mapSerializer.decode(bs);
        Assert.assertEquals(map, newmap);

        // 支持中文
        map.put("弄啥呢", "咋弄呢？");
        bs = mapSerializer.encode(map);
        newmap = mapSerializer.decode(bs);
        Assert.assertEquals(map, newmap);
    }

    //when read null
    @Test
    public void testCompatible() throws Exception {
        SimpleMapSerializer simpleMapSerializer = new SimpleMapSerializer();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("1", "2");
        map.put("", "x");
        map.put("b", null);
        map.put(null, null);
        byte[] bs = simpleMapSerializer.encode(map);
        Map<String, String> readMap = simpleMapSerializer.decode(bs);
        Assert.assertEquals(2, readMap.size());

        UnsafeByteArrayOutputStream out = new UnsafeByteArrayOutputStream(64);

        //key is null
        simpleMapSerializer.writeString(out, null);
        simpleMapSerializer.writeString(out, "value");

        //value is null
        simpleMapSerializer.writeString(out, "value");
        simpleMapSerializer.writeString(out, null);

        //value is null and key is null
        simpleMapSerializer.writeString(out, null);
        simpleMapSerializer.writeString(out, null);

        //value is not null and key is not null
        simpleMapSerializer.writeString(out, "key");
        simpleMapSerializer.writeString(out, "value");

        readMap = simpleMapSerializer.decode(out.toByteArray());
        Assert.assertEquals(1, readMap.size());
        Assert.assertEquals("value", readMap.get("key"));
    }

    @Test
    public void testParseRequestHeader() {
        String headerRpcId = RPC_TRACE_NAME + "." + RPC_ID_KEY;
        SofaRpcSerialization sofaRpcSerialization = new SofaRpcSerialization();
        SofaRequest sofaRequest = new SofaRequest();
        Map<String, String> headerMap = new HashMap<>();
        Map<String, String> tracerCtx = new HashMap<String, String>();
        tracerCtx.put(TRACE_ID_KEY, "MOCK_traceId");
        tracerCtx.put(RPC_ID_KEY, "MOCK_rpcId");
        sofaRequest.addRequestProp(RPC_TRACE_NAME, tracerCtx);
        sofaRequest.addRequestProp("exist", "exist");
        headerMap.put(headerRpcId, "changedRpcId");
        headerMap.put("X-Tldc-Target-Tenant", "fia");
        headerMap.put("exist", "");
        sofaRpcSerialization.parseRequestHeader(headerMap, sofaRequest);
        Map<String, Object> requestProps = sofaRequest.getRequestProps();
        Assert.assertEquals("MOCK_traceId", ((Map) requestProps.get(RPC_TRACE_NAME)).get(TRACE_ID_KEY));
        Assert.assertEquals("changedRpcId", ((Map) requestProps.get(RPC_TRACE_NAME)).get(RPC_ID_KEY));
        Assert.assertEquals("fia", requestProps.get("X-Tldc-Target-Tenant"));
        Assert.assertEquals("exist", requestProps.get("exist"));
        sofaRequest.getRequestProps().clear();
        headerMap.put(headerRpcId, "changedRpcId");
        headerMap.put("X-Tldc-Target-Tenant", "fia");
        headerMap.put("exist", "");
        sofaRpcSerialization.parseRequestHeader(headerMap, sofaRequest);
        Assert.assertNull(((Map) requestProps.get(RPC_TRACE_NAME)).get(TRACE_ID_KEY));
        Assert.assertEquals("changedRpcId", ((Map) requestProps.get(RPC_TRACE_NAME)).get(RPC_ID_KEY));
        Assert.assertEquals("fia", requestProps.get("X-Tldc-Target-Tenant"));
        Assert.assertEquals("", requestProps.get("exist"));
    }

    @Test
    public void testParseResponseHeader() {
        SofaRpcSerialization sofaRpcSerialization = new SofaRpcSerialization();
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("X-Tldc-Target-Tenant", "fia");
        SofaResponse sofaResponse = new SofaResponse();
        sofaRpcSerialization.parseResponseHeader(headerMap,sofaResponse);
        Assert.assertEquals("fia", sofaResponse.getResponseProps().get("X-Tldc-Target-Tenant"));
        sofaResponse.getResponseProps().clear();

        headerMap.put("exist", "");
        sofaResponse.addResponseProp("exist", "exist");
        sofaRpcSerialization.parseResponseHeader(headerMap, sofaResponse);
        Map<String, String> responseProps = sofaResponse.getResponseProps();
        Assert.assertEquals("fia", responseProps.get("X-Tldc-Target-Tenant"));
        Assert.assertEquals("exist", responseProps.get("exist"));

        sofaResponse.getResponseProps().clear();
        headerMap.put("X-Tldc-Target-Tenant", "fia");
        headerMap.put("exist", "");
        sofaRpcSerialization.parseResponseHeader(headerMap, sofaResponse);
        Assert.assertEquals("fia", responseProps.get("X-Tldc-Target-Tenant"));
        Assert.assertEquals("", responseProps.get("exist"));
    }
}