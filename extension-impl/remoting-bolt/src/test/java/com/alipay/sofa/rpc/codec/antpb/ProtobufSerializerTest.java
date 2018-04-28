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
package com.alipay.sofa.rpc.codec.antpb;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProtobufSerializerTest {

    @Test
    public void getInstance() throws Exception {
        ProtobufSerializer s1 = ProtobufSerializer.getInstance();
        ProtobufSerializer s2 = ProtobufSerializer.getInstance();
        Assert.assertTrue(s1 == s2);
    }

    @Test
    public void encode() throws Exception {
        EchoStrReq req = EchoStrReq.newBuilder().setS("xxxx").build();
        byte[] bs = req.toByteArray();
        byte[] bs2 = ProtobufSerializer.getInstance().encode(req);

        Assert.assertArrayEquals(bs, bs2);
    }

    @Test
    public void decode() throws Exception {
        EchoStrRes res = EchoStrRes.newBuilder().setS("xxxx").build();
        byte[] src = ProtobufSerializer.getInstance().encode(res);

        EchoStrRes res1 = (EchoStrRes) ProtobufSerializer.getInstance().decode(src,
            EchoStrRes.class);
        Assert.assertEquals(res1.getS(), res.getS());

        src = ProtobufSerializer.getInstance().encode("xxx");
        String s = (String) ProtobufSerializer.getInstance().decode(src, String.class);
        Assert.assertEquals("xxx", s);

    }

    @Test
    public void getReqClass() throws Exception {
        Class req = ProtobufSerializer.getInstance().getReqClass(
            "com.alipay.sofa.rpc.codec.antpb.ProtoService", "echoStr", Thread.currentThread()
                .getContextClassLoader());
        Assert.assertTrue(req == EchoStrReq.class);
    }

    @Test
    public void getResClass() throws Exception {
        Class res = ProtobufSerializer.getInstance().getResClass(
            "com.alipay.sofa.rpc.codec.antpb.ProtoService", "echoStr", Thread.currentThread()
                .getContextClassLoader());
        Assert.assertTrue(res == EchoStrRes.class);
    }

    @Test
    public void testJudgeProtoInterface() throws Exception {
        EchoStrRes res = EchoStrRes.newBuilder().setS("xxxx").build();
        boolean find = ProtobufSerializer.getInstance().isProtoBufMessageObject(res);
        Assert.assertTrue(find);

        find = ProtobufSerializer.getInstance().isProtoBufMessageObject(null);
        Assert.assertFalse(find);

        find = ProtobufSerializer.getInstance().isProtoBufMessageObject(new Object());
        Assert.assertFalse(find);

        find = ProtobufSerializer.getInstance().isProtoBufMessageClass(null);
        Assert.assertFalse(find);

        find = ProtobufSerializer.getInstance().isProtoBufMessageClass(EchoStrRes.class);
        Assert.assertTrue(find);

        find = ProtobufSerializer.getInstance().isProtoBufMessageClass(EchoStrRes.Builder.class);
        Assert.assertFalse(find);
    }
}