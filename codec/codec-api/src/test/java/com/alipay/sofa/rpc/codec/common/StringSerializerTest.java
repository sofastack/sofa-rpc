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
package com.alipay.sofa.rpc.codec.common;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class StringSerializerTest {

    @Test
    public void encode() {
        Assert.assertTrue(StringSerializer.encode("11").length == 2);
        Assert.assertTrue(StringSerializer.encode("").length == 0);
        Assert.assertTrue(StringSerializer.encode(null).length == 0);
    }

    @Test
    public void decode() {
        Assert.assertNull(StringSerializer.decode(null));
        Assert.assertEquals("", StringSerializer.decode(new byte[0]));
        Assert.assertEquals("11", StringSerializer.decode(StringSerializer.encode("11")));
    }
}