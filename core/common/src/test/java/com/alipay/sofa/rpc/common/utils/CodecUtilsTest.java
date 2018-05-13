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
package com.alipay.sofa.rpc.common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class CodecUtilsTest {
    @Test
    public void intToBytes() {
        int i = 1000;
        byte[] bs = CodecUtils.intToBytes(i);
        Assert.assertArrayEquals(bs, new byte[] { 0, 0, 3, -24 });

        i = 0;
        bs = CodecUtils.intToBytes(i);
        Assert.assertArrayEquals(bs, new byte[] { 0, 0, 0, 0 });

        int s = 16777218; // =1*256*256*256+ 0*256*256 +  0*256 + 2
        bs = CodecUtils.intToBytes(s);
        Assert.assertEquals(bs[0], 1);
        Assert.assertEquals(bs[1], 0);
        Assert.assertEquals(bs[2], 0);
        Assert.assertEquals(bs[3], 2);
    }

    @Test
    public void bytesToInt() {

        int i = CodecUtils.bytesToInt(new byte[] { 0, 0, 0, 0 });
        Assert.assertEquals(0, i);

        i = CodecUtils.bytesToInt(new byte[] { 0, 0, 3, -24 });
        Assert.assertEquals(1000, i);

        int s = CodecUtils.bytesToInt(new byte[] { 1, 0, 0, 2 });
        Assert.assertEquals(s, 16777218);
    }

    @Test
    public void short2bytes() {
        short i = 0;
        byte[] bs = CodecUtils.short2bytes(i);
        Assert.assertArrayEquals(bs, new byte[] { 0, 0 });

        i = 1000;
        bs = CodecUtils.short2bytes(i);
        Assert.assertArrayEquals(bs, new byte[] { 3, -24 });

        short s = 258; // =1*256+2
        bs = CodecUtils.short2bytes(s);
        Assert.assertEquals(bs[0], 1);
        Assert.assertEquals(bs[1], 2);
    }

    @Test
    public void copyOf() {
        byte[] bs = new byte[] { 1, 2, 3, 5 };
        byte[] cp = CodecUtils.copyOf(bs, 3);
        Assert.assertArrayEquals(cp, new byte[] { 1, 2, 3 });

        cp = CodecUtils.copyOf(bs, 5);
        Assert.assertArrayEquals(cp, new byte[] { 1, 2, 3, 5, 0 });
    }

    @Test
    public void parseHigh4Low4Bytes() {
        byte b = 117; // = 7*16+5
        byte[] bs = CodecUtils.parseHigh4Low4Bytes(b);
        Assert.assertEquals(bs[0], 7);
        Assert.assertEquals(bs[1], 5);
    }

    @Test
    public void buildHigh4Low4Bytes() {
        byte bs = CodecUtils.buildHigh4Low4Bytes((byte) 7, (byte) 5);
        Assert.assertEquals(bs, (byte) 117);
    }

    @Test
    public void parseHigh2Low6Bytes() {
        byte b = 117; // = 1*64 + 53
        byte[] bs = CodecUtils.parseHigh2Low6Bytes(b);
        Assert.assertEquals(bs[0], 1);
        Assert.assertEquals(bs[1], 53);
    }

    @Test
    public void buildHigh2Low6Bytes() {
        byte bs = CodecUtils.buildHigh2Low6Bytes((byte) 1, (byte) 53);
        Assert.assertEquals(bs, (byte) 117);
    }

    @Test
    public void byteToBits() {
        byte b = 0x35; // 0011 0101
        Assert.assertEquals(CodecUtils.byteToBits(b), "00110101");
    }

    @Test
    public void bitsToByte() {
        String s = "00110101";
        Assert.assertEquals(CodecUtils.bitsToByte(s), 0x35);
        String s1 = "00111101";
        Assert.assertEquals(CodecUtils.bitsToByte(s1), 0x3d);
    }

    @Test
    public void startsWith() {
    }

    @Test
    public void byte2Booleans() {
    }

    @Test
    public void booleansToByte() {
    }

    @Test
    public void getBooleanFromByte() {
        byte b = 0x35; // 0011 0101
        Assert.assertTrue(CodecUtils.getBooleanFromByte(b, 0));
        Assert.assertFalse(CodecUtils.getBooleanFromByte(b, 1));
        Assert.assertTrue(CodecUtils.getBooleanFromByte(b, 2));
        Assert.assertFalse(CodecUtils.getBooleanFromByte(b, 3));
        Assert.assertTrue(CodecUtils.getBooleanFromByte(b, 4));
        Assert.assertTrue(CodecUtils.getBooleanFromByte(b, 5));
        Assert.assertFalse(CodecUtils.getBooleanFromByte(b, 6));
        Assert.assertFalse(CodecUtils.getBooleanFromByte(b, 7));
        boolean ok = true;
        try {
            Assert.assertFalse(CodecUtils.getBooleanFromByte(b, -1));
        } catch (Exception e) {
            ok = false;
        }
        Assert.assertFalse(ok);
        ok = true;
        try {
            Assert.assertFalse(CodecUtils.getBooleanFromByte(b, 8));
        } catch (Exception e) {
            ok = false;
        }
        Assert.assertFalse(ok);
    }

    @Test
    public void setBooleanToByte() {
        byte b = 0x35; // 0011 0101
        byte b1 = CodecUtils.setBooleanToByte(b, 0, true);
        Assert.assertEquals(b, b1);
        byte b2 = CodecUtils.setBooleanToByte(b, 1, false);
        Assert.assertEquals(b, b2);

        byte b3 = CodecUtils.setBooleanToByte(b, 3, true);
        Assert.assertFalse(b == b3);
        Assert.assertTrue(CodecUtils.getBooleanFromByte(b3, 3));
        byte b4 = CodecUtils.setBooleanToByte(b, 4, false);
        Assert.assertFalse(b == b4);
        Assert.assertFalse(CodecUtils.getBooleanFromByte(b4, 4));
    }

    @Test
    public void flatCopyTo() {
        Map<String, Object> requestProps = new HashMap<String, Object>();
        requestProps.put("xx", "xxxxxxx");
        requestProps.put("yyy", new String[] { "yyyy" }); // string数组无法传递
        requestProps.put("zzzz", 333);

        Map<String, String> header = new HashMap<String, String>();
        Map<String, String> context = new HashMap<String, String>();
        context.put("sofaCallerApp", "test");
        context.put("sofaCallerIp", "10.15.233.63");
        context.put("sofaPenAttrs", "");
        context.put("sofaRpcId", "0");
        context.put("sofaTraceId", "0a0fe93f1488349732342100153695");
        context.put("sysPenAttrs", "");
        context.put("penAttrs", "Hello=world&");
        String rpcTraceContext = "rpc_trace_context";
        requestProps.put(rpcTraceContext, context);

        Map<String, String> requestBaggage = new HashMap<String, String>();
        requestBaggage.put("aaa", "reqasdhjaksdhaksdyiasdhasdhaskdhaskd");
        requestBaggage.put("bbb", "req10.15.233.63");
        requestBaggage.put("ccc", "reqwhat 's wrong");
        String rpcReqBaggage = "rpc_req_baggage";
        requestProps.put(rpcReqBaggage, requestBaggage);

        Map<String, String> responseBaggage = new HashMap<String, String>();
        responseBaggage.put("xxx", "respasdhjaksdhaksdyiasdhasdhaskdhaskd");
        responseBaggage.put("yyy", "resp10.15.233.63");
        responseBaggage.put("zzz", "resphehehe");
        String rpcRespBaggage = "rpc_resp_baggage";
        requestProps.put(rpcRespBaggage, responseBaggage);

        //        rpcSerialization.
        CodecUtils.flatCopyTo("", requestProps, header);
        Assert.assertTrue(header.size() == 15);

        for (Map.Entry<String, String> entry : header.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("");

        Map<String, Object> newRequestProps = new HashMap<String, Object>();

        Map<String, String> newContext = new HashMap<String, String>();
        CodecUtils.treeCopyTo(rpcTraceContext + ".", header, newContext, true);
        newRequestProps.put(rpcTraceContext, newContext);

        newContext = new HashMap<String, String>();
        CodecUtils.treeCopyTo(rpcReqBaggage + ".", header, newContext,
            true);
        newRequestProps.put(rpcReqBaggage, newContext);

        newContext = new HashMap<String, String>();
        CodecUtils.treeCopyTo(rpcRespBaggage + ".", header,
            newContext, true);
        newRequestProps.put(rpcRespBaggage, newContext);

        for (Map.Entry<String, Object> entry : newRequestProps.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        newRequestProps.putAll(header);

        Assert.assertTrue(newRequestProps.size() == 5);
    }

}