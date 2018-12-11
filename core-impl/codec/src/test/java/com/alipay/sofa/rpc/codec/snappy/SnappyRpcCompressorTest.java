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

import java.io.UnsupportedEncodingException;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SnappyRpcCompressorTest {

    @Test
    public void testCompress() throws UnsupportedEncodingException {
        SnappyRpcCompressor compressor = new SnappyRpcCompressor();
        String s = "xxxxasdasdasd0as8d0asdkmasldjalsd";
        byte[] bs = compressor.compress(s.getBytes("utf-8"));
        Assert.assertNotNull(s);

        String s1 = new String(compressor.deCompress(bs), "utf-8");
        Assert.assertEquals(s, s1);
    }

    @Test
    public void testUnCompressSpecifySize() throws UnsupportedEncodingException {
        SnappyRpcCompressor compressor = new SnappyRpcCompressor();
        String s = "xxxxasdasdasd0as8d0asdkmasldjalsd";
        byte[] bs = compressor.compress(s.getBytes("utf-8"));
        Assert.assertNotNull(s);

        String s1 = new String(compressor.uncompress(bs, 0, bs.length), "utf-8");
        Assert.assertEquals(s, s1);
    }

    @Test
    public void testPartCompressSpecifySize() throws UnsupportedEncodingException {
        SnappyRpcCompressor compressor = new SnappyRpcCompressor();
        String s = "xxxxasdasdasd0as8d0asdkmasldjalsd";
        byte[] bs = compressor.compress(s.getBytes("utf-8"));
        Assert.assertNotNull(s);

        String s1 = null;
        try {
            s1 = new String(compressor.uncompress(bs, 0, 1), "utf-8");
            Assert.fail();
        } catch (CorruptionException e) {
            Assert.assertTrue(true);

        }
    }

    @Test
    public void testBadUnCompressSpecifySize() throws UnsupportedEncodingException {
        SnappyRpcCompressor compressor = new SnappyRpcCompressor();
        String s = "xxxxasdasdasd0as8d0asdkmasldjalsd";
        byte[] bs = compressor.compress(s.getBytes("utf-8"));
        Assert.assertNotNull(s);

        try {
            String s1 = new String(compressor.uncompress(bs, 0, bs.length + 1), "utf-8");
            Assert.fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
    }
}