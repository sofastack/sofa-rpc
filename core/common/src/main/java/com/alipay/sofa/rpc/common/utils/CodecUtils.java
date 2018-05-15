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

import java.util.Iterator;
import java.util.Map;

/**
 * Codec工具类
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public final class CodecUtils {

    /**
     * 空的Object数组，无参方法
     */
    public static final Object[]   EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * 空的Class数组，无参方法
     */
    public static final Class<?>[] EMPTY_CLASS_ARRAY  = new Class<?>[0];

    /**
     * int 转 byte数组
     *
     * @param num int值
     * @return byte[4]
     */
    public static byte[] intToBytes(int num) {
        byte[] result = new byte[4];
        result[0] = (byte) (num >>> 24);//取最高8位放到0下标
        result[1] = (byte) (num >>> 16);//取次高8为放到1下标
        result[2] = (byte) (num >>> 8); //取次低8位放到2下标
        result[3] = (byte) (num); //取最低8位放到3下标
        return result;
    }

    /**
     * byte数组转int
     *
     * @param ary byte[4]
     * @return int值
     */
    public static int bytesToInt(byte[] ary) {
        return (ary[3] & 0xFF)
            | ((ary[2] << 8) & 0xFF00)
            | ((ary[1] << 16) & 0xFF0000)
            | ((ary[0] << 24) & 0xFF000000);
    }

    /**
     * short 转 byte数组
     *
     * @param num short值
     * @return byte[2]
     */
    public static byte[] short2bytes(short num) {
        byte[] result = new byte[2];
        result[0] = (byte) (num >>> 8); //取次低8位放到0下标
        result[1] = (byte) (num); //取最低8位放到1下标
        return result;
    }

    /**
     * byte array copy.
     *
     * @param src    src.
     * @param length new length.
     * @return new byte array.
     */
    public static byte[] copyOf(byte[] src, int length) {
        byte[] dest = new byte[length];
        System.arraycopy(src, 0, dest, 0, Math.min(src.length, length));
        return dest;
    }

    /**
     * 一个byte存两个4bit的信息
     *
     * @param b 原始byte
     * @return byte数组 [&lt;16,&lt;16]
     */
    public static byte[] parseHigh4Low4Bytes(byte b) {
        return new byte[] {
                (byte) ((b >> 4)), // 右移4位，只取前4bit的值
                (byte) ((b & 0x0f)) // 只取后面4bit的值，前面两位补0
        };
    }

    /**
     * 一个byte存两个4bit的信息
     *
     * @param high4 高4位 &lt;16
     * @param low4  低4位 &lt;16
     * @return 一个byte存两个4bit的信息
     */
    public static byte buildHigh4Low4Bytes(byte high4, byte low4) {
        return (byte) ((high4 << 4) + low4);
    }

    /**
     * 一个byte存一个2bit和6bit的信息
     *
     * @param b 原始byte
     * @return byte数组{&lt;4,&lt;64}
     */
    public static byte[] parseHigh2Low6Bytes(byte b) {
        return new byte[] {
                (byte) ((b >> 6)), // 右移6位，只取前2bit的值
                (byte) ((b & 0x3f)) // 只取后面6bit的值，前面两位补0
        };
    }

    /**
     * 一个byte存一个2bit和6bit的信息
     *
     * @param high2 高2位 &lt;4
     * @param low6  低6位 &lt;64
     * @return byte数组{&lt;4,&lt;64}
     */
    public static byte buildHigh2Low6Bytes(byte high2, byte low6) {
        return (byte) ((high2 << 6) + low6);
    }

    /**
     * 把byte转为字符串的bit
     * @param b byte
     * @return bit字符串
     */
    public static String byteToBits(byte b) {
        return ""
            + (byte) ((b >> 7) & 0x01) + (byte) ((b >> 6) & 0x1)
            + (byte) ((b >> 5) & 0x01) + (byte) ((b >> 4) & 0x1)
            + (byte) ((b >> 3) & 0x01) + (byte) ((b >> 2) & 0x1)
            + (byte) ((b >> 1) & 0x01) + (byte) ((b >> 0) & 0x1);
    }

    /**
     * 把字符串的bit转为byte
     * @param bits bits
     * @return byte
     */
    public static byte bitsToByte(String bits) {
        byte b = 0;
        for (int i = bits.length() - 1, j = 0; i >= 0; i--, j++) {
            char c = bits.charAt(i);
            if (c == '1') {
                b += (1 << j);
            }
        }
        return b;
    }

    /**
     * byte数组比较，是否命中前面几位
     *
     * @param bs 字符数组
     * @param head 匹配头部数组
     * @return 是否匹配
     */
    public static boolean startsWith(byte[] bs, byte[] head) {
        if (bs.length < head.length) {
            return false;
        }
        for (int i = 0; i < head.length; i++) {
            if (head[i] != bs[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将byte转换为一个长度为8的boolean数组（每bit代表一个boolean值）
     *
     * @param b byte
     * @return boolean数组
     */
    public static boolean[] byte2Booleans(byte b) {
        boolean[] array = new boolean[8];
        for (int i = 7; i >= 0; i--) { //对于byte的每bit进行判定
            array[i] = (b & 1) == 1; //判定byte的最后一位是否为1，若为1，则是true；否则是false
            b = (byte) (b >> 1); //将byte右移一位
        }
        return array;
    }

    /**
     * 将一个长度为8的boolean数组（每bit代表一个boolean值）转换为byte
     *
     * @param array boolean数组
     * @return byte
     */
    public static byte booleansToByte(boolean[] array) {
        if (array != null && array.length > 0) {
            byte b = 0;
            for (int i = 0; i <= 7; i++) {
                if (array[i]) {
                    int nn = (1 << (7 - i));
                    b += nn;
                }
            }
            return b;
        }
        return 0;
    }

    /**
     * 一个byte可以存8个boolean，可以按位获取
     *
     * @param modifiers 描述符
     * @param i         索引 0-7
     * @return 该索引bit对应的boolean（0false1true）
     */
    public static boolean getBooleanFromByte(byte modifiers, int i) {
        if (i > 7 || i < 0) {
            throw new IllegalArgumentException("Index must between 0-7!");
        }
        return ((modifiers >> i) & 0x01) == 1;
    }

    /**
     * 一个byte可以存8个boolean，可以按位设置
     *
     * @param modifiers 描述符
     * @param i         索引 0-7
     * @param bool      要设置的值
     * @return 新的描述符
     */
    public static byte setBooleanToByte(byte modifiers, int i, boolean bool) {
        boolean old = getBooleanFromByte(modifiers, i);
        if (old && !bool) { // true-->false
            return (byte) (modifiers - (1 << i));
        } else if (!old && bool) { // false-->true
            return (byte) (modifiers + (1 << i));
        }
        return modifiers;
    }

    /**
     * 扁平化复制
     * @param prefix 前缀
     * @param sourceMap 原始map
     * @param dstMap 目标map
     */
    public static void flatCopyTo(String prefix, Map<String, Object> sourceMap,
                                  Map<String, String> dstMap) {
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = prefix + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                dstMap.put(key, (String) value);
            } else if (value instanceof Number) {
                dstMap.put(key, value.toString());
            } else if (value instanceof Map) {
                flatCopyTo(key + ".", (Map<String, Object>) value, dstMap);
            }
        }
    }

    /**
     * 树状恢复
     * @param prefix 前缀
     * @param sourceMap  原始map
     * @param dstMap 目标map
     * @param remove 命中遍历后是否删除
     */
    public static void treeCopyTo(String prefix, Map<String, String> sourceMap,
                                  Map<String, String> dstMap, boolean remove) {
        Iterator<Map.Entry<String, String>> it = sourceMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getKey().startsWith(prefix)) {
                dstMap.put(entry.getKey().substring(prefix.length()), entry.getValue());
                if (remove) {
                    it.remove();
                }
            }
        }
    }
}
