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
package com.alipay.sofa.rpc.transport;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ByteArrayWrapperByteBufTest {

    @Test
    public void array() {
        AbstractByteBuf byteBuf = new ByteArrayWrapperByteBuf(null);
        Assert.assertNull(byteBuf.array());

        byte[] bs = new byte[] { 1, 2, 3 };
        byteBuf = new ByteArrayWrapperByteBuf(bs);
        Assert.assertNotNull(byteBuf.array());
        Assert.assertTrue(byteBuf.array().length == 3);
        Assert.assertTrue(byteBuf.readableBytes() == 3);
        Assert.assertTrue(byteBuf.release());
    }
}