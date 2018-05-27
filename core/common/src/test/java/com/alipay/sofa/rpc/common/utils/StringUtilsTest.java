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

public class StringUtilsTest {

    @Test
    public void testSplit() throws Exception {
        String[] array = StringUtils.split(null, ",");
        Assert.assertTrue(CommonUtils.isEmpty(array));

        array = StringUtils.split("", ",");
        Assert.assertTrue(CommonUtils.isEmpty(array));

        String src = "1;2;3";
        array = StringUtils.split(src, null);
        Assert.assertTrue(array.length == 1);

        array = StringUtils.split(src, ",");
        Assert.assertTrue(array.length == 1);

        src = "1;2;3";
        array = StringUtils.split(src, ";");
        Assert.assertTrue(array.length == 3);
        Assert.assertArrayEquals(array, new String[] { "1", "2", "3" });

        src = ";1;2;3;";
        array = StringUtils.split(src, ";");
        Assert.assertTrue(array.length == 5);
        Assert.assertArrayEquals(array, new String[] { "", "1", "2", "3", "" });

        src = "; 1;2 ; 3 ;";
        array = StringUtils.split(src, ";");
        Assert.assertTrue(array.length == 5);
        Assert.assertArrayEquals(array, new String[] { "", " 1", "2 ", " 3 ", "" });

    }

    @Test
    public void testSplitWithCommaAndSemicolon() throws Exception {
        String src = "1;2;3";
        String[] array = StringUtils.splitWithCommaOrSemicolon(src);
        Assert.assertTrue(array.length == 3);
        Assert.assertArrayEquals(array, new String[] { "1", "2", "3" });

        src = " 1;2 ; 3 ";
        array = StringUtils.splitWithCommaOrSemicolon(src);
        Assert.assertTrue(array.length == 3);
        Assert.assertArrayEquals(array, new String[] { "1", "2", "3" });

        src = ";;;1;;;;2;;;3;;;;";
        array = StringUtils.splitWithCommaOrSemicolon(src);
        Assert.assertTrue(array.length == 3);
        Assert.assertArrayEquals(array, new String[] { "1", "2", "3" });

        src = "  ;1;2;3;   ";
        array = StringUtils.splitWithCommaOrSemicolon(src);
        Assert.assertTrue(array.length == 3);
        Assert.assertArrayEquals(array, new String[] { "1", "2", "3" });

        array = StringUtils.splitWithCommaOrSemicolon("");
        Assert.assertTrue(CommonUtils.isEmpty(array));

        array = StringUtils.splitWithCommaOrSemicolon(null);
        Assert.assertTrue(CommonUtils.isEmpty(array));
    }

    @Test
    public void testJoin() throws Exception {
        String[] src = new String[] { "1", "2", "3" };
        String arrayString = StringUtils.join(src, "");
        Assert.assertEquals(arrayString, "123");

        arrayString = StringUtils.join(src, ",");
        Assert.assertEquals(arrayString, "1,2,3");

        arrayString = StringUtils.join(new String[] {}, ",");
        Assert.assertEquals(arrayString, "");

        arrayString = StringUtils.join(null, "");
        Assert.assertEquals(arrayString, "");
    }

    @Test
    public void testJoinWithComma() throws Exception {
        String[] src = new String[] { "1", "2", "3" };
        String arrayString = StringUtils.joinWithComma(src);
        Assert.assertEquals(arrayString, "1,2,3");

        arrayString = StringUtils.joinWithComma(new String[] {});
        Assert.assertEquals(arrayString, "");

        arrayString = StringUtils.joinWithComma(null, null);
        Assert.assertEquals(arrayString, "");
    }

    @Test
    public void testSplitWithCommaOrSemicolon() throws Exception {
        String[] s = StringUtils.splitWithCommaOrSemicolon(null);
        Assert.assertTrue(CommonUtils.isEmpty(s));

        s = StringUtils.splitWithCommaOrSemicolon("");
        Assert.assertTrue(CommonUtils.isEmpty(s));

        s = StringUtils.splitWithCommaOrSemicolon("1");
        Assert.assertNotNull(s);
        Assert.assertEquals(s.length, 1);

        s = StringUtils.splitWithCommaOrSemicolon("1,");
        Assert.assertNotNull(s);
        Assert.assertEquals(s.length, 1);

        s = StringUtils.splitWithCommaOrSemicolon(" 1,");
        Assert.assertNotNull(s);
        Assert.assertEquals(s.length, 1);

        s = StringUtils.splitWithCommaOrSemicolon(" 1,2");
        Assert.assertNotNull(s);
        Assert.assertEquals(s.length, 2);

        s = StringUtils.splitWithCommaOrSemicolon(" 1;2");
        Assert.assertNotNull(s);
        Assert.assertEquals(s.length, 2);
    }

    @Test
    public void testSubstringAfter() {
        Assert.assertEquals(null, StringUtils.substringAfter(null, "*"));
        Assert.assertEquals("", StringUtils.substringAfter("", "*"));
        Assert.assertEquals("", StringUtils.substringAfter("*", null));
        Assert.assertEquals("bc", StringUtils.substringAfter("abc", "a"));
        Assert.assertEquals("cba", StringUtils.substringAfter("abcba", "b"));
        Assert.assertEquals("", StringUtils.substringAfter("abc", "c"));
        Assert.assertEquals("", StringUtils.substringAfter("abc", "d"));
        Assert.assertEquals("abc", StringUtils.substringAfter("abc", ""));
    }

    @Test
    public void testToString() {
        Assert.assertEquals(null, StringUtils.toString(null));
        Assert.assertEquals("Bean:11", StringUtils.toString(new Bean("11")));

        Assert.assertEquals(null, StringUtils.toString((Object) null, null));
        Assert.assertEquals("1", StringUtils.toString((Object) null, "1"));
        Assert.assertEquals("Bean:11", StringUtils.toString(new Bean("11"), null));

        Assert.assertEquals(null, StringUtils.objectsToString(null));
        Assert.assertEquals("[]", StringUtils.objectsToString(new Object[0]));
        Assert.assertEquals("[1,22]", StringUtils.objectsToString(new Object[] { 1, "22" }));
        Assert.assertEquals("[1,Bean:11]", StringUtils.objectsToString(new Object[] { 1, new Bean("11") }));
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(StringUtils.equals(null, null));
        Assert.assertFalse(StringUtils.equals(null, ""));
        Assert.assertFalse(StringUtils.equals("", null));
        Assert.assertTrue(StringUtils.equals("", ""));
        Assert.assertFalse(StringUtils.equals("1", "2"));
        Assert.assertTrue(StringUtils.equals("1", "1"));
    }

    class Bean {
        private String s;

        public Bean() {

        }

        public Bean(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return "Bean:" + s;
        }
    }
}