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
package com.alipay.sofa.rpc.codec.snappy;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author bystander
 * @version $Id: BufferRecyclerTest.java, v 0.1 2018年12月10日 20:24 bystander Exp $
 */
public class BufferRecyclerTest {

    @Test
    public void encodingBuffer() {
        byte[] b = BufferRecycler.instance().allocEncodingBuffer(10);
        Assert.assertEquals(4000, b.length);

        BufferRecycler.instance().releaseEncodeBuffer(b);

    }

    @Test
    public void inputBuffer() {

        byte[] b = BufferRecycler.instance().allocInputBuffer(10);
        Assert.assertEquals(8000, b.length);

        BufferRecycler.instance().releaseInputBuffer(b);
    }

    @Test
    public void allocDecodeBuffer() {

        byte[] b = BufferRecycler.instance().allocDecodeBuffer(10);
        Assert.assertEquals(10, b.length);

        BufferRecycler.instance().releaseDecodeBuffer(b);
    }
}