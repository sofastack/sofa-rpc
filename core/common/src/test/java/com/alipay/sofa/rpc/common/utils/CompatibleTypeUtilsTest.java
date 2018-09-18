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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class CompatibleTypeUtilsTest {
    @Test
    public void convert() throws Exception {
        Assert.assertEquals('x', CompatibleTypeUtils.convert("x", char.class));
        Assert.assertEquals('x', CompatibleTypeUtils.convert("x", Character.class));
        boolean error = false;
        try {
            CompatibleTypeUtils.convert("xx", Character.class);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        Assert.assertEquals(TestEnum.A, CompatibleTypeUtils.convert("A", TestEnum.class));
        Assert.assertEquals(new BigInteger("123"), CompatibleTypeUtils.convert("123", BigInteger.class));
        Assert.assertEquals(new BigDecimal("123.12"), CompatibleTypeUtils.convert("123.12", BigDecimal.class));
        Assert.assertEquals(new Short("123"), CompatibleTypeUtils.convert("123", Short.class));
        Assert.assertEquals(new Short("123"), CompatibleTypeUtils.convert("123", short.class));
        Assert.assertEquals(new Integer("123"), CompatibleTypeUtils.convert("123", Integer.class));
        Assert.assertEquals(new Integer("123"), CompatibleTypeUtils.convert("123", int.class));
        Assert.assertEquals(new Long("123"), CompatibleTypeUtils.convert("123", Long.class));
        Assert.assertEquals(new Long("123"), CompatibleTypeUtils.convert("123", long.class));
        Assert.assertEquals(new Double("123.1"), CompatibleTypeUtils.convert("123.1", Double.class));
        Assert.assertEquals(new Double("123.1"), CompatibleTypeUtils.convert("123.1", double.class));
        Assert.assertEquals(new Byte("123"), CompatibleTypeUtils.convert("123", Byte.class));
        Assert.assertEquals(new Byte("123"), CompatibleTypeUtils.convert("123", byte.class));
        Assert.assertEquals(new Float("123.1"), CompatibleTypeUtils.convert("123.1", Float.class));
        Assert.assertEquals(new Float("123.1"), CompatibleTypeUtils.convert("123.1", float.class));
        Assert.assertEquals(Boolean.TRUE, CompatibleTypeUtils.convert("true", Boolean.class));
        Assert.assertEquals(Boolean.TRUE, CompatibleTypeUtils.convert("true", boolean.class));

        Date dataTime = DateUtils.strToDate("2018-1-1 11:22:33");
        Assert.assertEquals(dataTime, CompatibleTypeUtils.convert("2018-1-1 11:22:33", Date.class));
        Long timeLong = DateUtils.strToLong("2018-1-1 11:22:33");
        java.sql.Date sqlDate = new java.sql.Date(timeLong);
        Object timeResult = CompatibleTypeUtils.convert("2018-1-1 11:22:33", java.sql.Date.class);
        Assert.assertEquals(sqlDate, timeResult);
        timeResult = CompatibleTypeUtils.convert(timeLong, java.sql.Date.class);
        Assert.assertEquals(sqlDate, timeResult);
        timeResult = CompatibleTypeUtils.convert("2018-1-1 11:22:33", java.sql.Timestamp.class);
        java.sql.Timestamp timestamp = new java.sql.Timestamp(timeLong);
        Assert.assertEquals(timestamp, timeResult);
        timeResult = CompatibleTypeUtils.convert("2018-1-1 11:22:33", java.sql.Time.class);
        java.sql.Time time = new java.sql.Time(timeLong);
        Assert.assertEquals(time, timeResult);

        Assert.assertEquals(new Short("123"), CompatibleTypeUtils.convert(123, Short.class));
        Assert.assertEquals(new Short("123"), CompatibleTypeUtils.convert(123, short.class));
        Assert.assertEquals(new Integer("123"), CompatibleTypeUtils.convert(123, Integer.class));
        Assert.assertEquals(new Integer("123"), CompatibleTypeUtils.convert(123, int.class));
        Assert.assertEquals(new Long("123"), CompatibleTypeUtils.convert(123, Long.class));
        Assert.assertEquals(new Long("123"), CompatibleTypeUtils.convert(123, long.class));
        Assert.assertEquals(new Double("123.1"), CompatibleTypeUtils.convert(123.1, Double.class));
        Assert.assertEquals(new Double("123.1"), CompatibleTypeUtils.convert(123.1, double.class));
        Assert.assertEquals(new Byte("123"), CompatibleTypeUtils.convert(123, Byte.class));
        Assert.assertEquals(new Byte("123"), CompatibleTypeUtils.convert(123, byte.class));
        Assert.assertEquals(new Float("123.1"), CompatibleTypeUtils.convert(123.1, Float.class));
        Assert.assertEquals(new Float("123.1"), CompatibleTypeUtils.convert(123.1, float.class));
        Assert.assertEquals(Boolean.TRUE, CompatibleTypeUtils.convert("true", Boolean.class));
        Assert.assertEquals(Boolean.TRUE, CompatibleTypeUtils.convert("true", boolean.class));

        String[] ss = (String[]) CompatibleTypeUtils.convert(Collections.singletonList("x"), String[].class);
        Assert.assertEquals("x", ss[0]);
        List list = (List) CompatibleTypeUtils.convert(Collections.singleton("x"), List.class);
        Assert.assertEquals("x", list.get(0));
        list = (List) CompatibleTypeUtils.convert(Collections.singletonList("x"), ArrayList.class);
        Assert.assertEquals("x", list.get(0));
        Set set = (Set) CompatibleTypeUtils.convert(Collections.singletonList("x"), Set.class);
        Assert.assertEquals("x", set.iterator().next());

        list = (List) CompatibleTypeUtils.convert(new String[] { "x" }, List.class);
        Assert.assertEquals("x", list.get(0));
        list = (List) CompatibleTypeUtils.convert(new String[] { "x" }, ArrayList.class);
        Assert.assertEquals("x", list.get(0));
        set = (Set) CompatibleTypeUtils.convert(new String[] { "x" }, Set.class);
        Assert.assertEquals("x", set.iterator().next());
    }

}