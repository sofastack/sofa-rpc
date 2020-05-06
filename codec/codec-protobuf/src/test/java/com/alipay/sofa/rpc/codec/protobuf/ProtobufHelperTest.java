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
package com.alipay.sofa.rpc.codec.protobuf;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProtobufHelperTest {

    private ProtobufHelper protobufHelper = new ProtobufHelper();

    @Test
    public void getReqClass() {
        Class req = protobufHelper.getReqClass(
            "com.alipay.sofa.rpc.codec.protobuf.ProtoService", "echoStr");
        Assert.assertTrue(req == EchoStrReq.class);
    }

    @Test
    public void getResClass() {
        Class res = protobufHelper.getResClass(
            "com.alipay.sofa.rpc.codec.protobuf.ProtoService", "echoStr");
        Assert.assertTrue(res == EchoStrRes.class);
    }

    @Test
    public void testJudgeProtoInterface() {
        EchoStrRes res = EchoStrRes.newBuilder().setS("xxxx").build();
        boolean find = protobufHelper.isProtoBufMessageObject(res);
        Assert.assertTrue(find);

        find = protobufHelper.isProtoBufMessageObject(null);
        Assert.assertFalse(find);

        find = protobufHelper.isProtoBufMessageObject(new Object());
        Assert.assertFalse(find);

        find = protobufHelper.isProtoBufMessageClass(null);
        Assert.assertFalse(find);

        find = protobufHelper.isProtoBufMessageClass(EchoStrRes.class);
        Assert.assertTrue(find);

        find = protobufHelper.isProtoBufMessageClass(EchoStrRes.Builder.class);
        Assert.assertFalse(find);
    }
}