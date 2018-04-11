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

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class CodecUtilsTest {
    @Test
    public void intToBytes() throws Exception {
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
    public void bytesToInt() throws Exception {

        int i = CodecUtils.bytesToInt(new byte[] { 0, 0, 0, 0 });
        Assert.assertEquals(0, i);

        i = CodecUtils.bytesToInt(new byte[] { 0, 0, 3, -24 });
        Assert.assertEquals(1000, i);

        int s = CodecUtils.bytesToInt(new byte[] { 1, 0, 0, 2 });
        Assert.assertEquals(s, 16777218);
    }

    @Test
    public void short2bytes() throws Exception {
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
    public void copyOf() throws Exception {
        byte[] bs = new byte[] { 1, 2, 3, 5 };
        byte[] cp = CodecUtils.copyOf(bs, 3);
        Assert.assertArrayEquals(cp, new byte[] { 1, 2, 3 });

        cp = CodecUtils.copyOf(bs, 5);
        Assert.assertArrayEquals(cp, new byte[] { 1, 2, 3, 5, 0 });
    }

    @Test
    public void parseHigh4Low4Bytes() throws Exception {
        byte b = 117; // = 7*16+5
        byte[] bs = CodecUtils.parseHigh4Low4Bytes(b);
        Assert.assertEquals(bs[0], 7);
        Assert.assertEquals(bs[1], 5);
    }

    @Test
    public void buildHigh4Low4Bytes() throws Exception {
        byte bs = CodecUtils.buildHigh4Low4Bytes((byte) 7, (byte) 5);
        Assert.assertEquals(bs, (byte) 117);
    }

    @Test
    public void parseHigh2Low6Bytes() throws Exception {
        byte b = 117; // = 1*64 + 53
        byte[] bs = CodecUtils.parseHigh2Low6Bytes(b);
        Assert.assertEquals(bs[0], 1);
        Assert.assertEquals(bs[1], 53);
    }

    @Test
    public void buildHigh2Low6Bytes() throws Exception {
        byte bs = CodecUtils.buildHigh2Low6Bytes((byte) 1, (byte) 53);
        Assert.assertEquals(bs, (byte) 117);
    }

    @Test
    public void byteToBits() throws Exception {
        byte b = 0x35; // 0011 0101
        Assert.assertEquals(CodecUtils.byteToBits(b), "00110101");
    }

    @Test
    public void bitsToByte() throws Exception {
        String s = "00110101";
        Assert.assertEquals(CodecUtils.bitsToByte(s), 0x35);
        String s1 = "00111101";
        Assert.assertEquals(CodecUtils.bitsToByte(s1), 0x3d);
    }

    @Test
    public void startsWith() throws Exception {
    }

    @Test
    public void byte2Booleans() throws Exception {
    }

    @Test
    public void booleansToByte() throws Exception {
    }

    @Test
    public void getBooleanFromByte() throws Exception {
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
    public void setBooleanToByte() throws Exception {
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
}