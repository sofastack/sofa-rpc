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

import java.util.Date;
import java.util.TimeZone;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class DateUtilsTest {
    @Test
    public void getDelayToNextMinute() throws Exception {
        long now = System.currentTimeMillis();
        int delay = DateUtils.getDelayToNextMinute(now);
        Assert.assertTrue(delay < 60000);
    }

    @Test
    public void getPreMinuteMills() throws Exception {
        long now = System.currentTimeMillis();
        long pre = DateUtils.getPreMinuteMills(now);
        Assert.assertTrue(now - pre < 60000);
    }

    @Test
    public void dateToStr() throws Exception {

        long s1 = 1501127802975l; // 2017-07-27 11:56:42:975 +8
        long s2 = 1501127835658l; // 2017-07-27 11:57:15:658 +8
        TimeZone timeZone = TimeZone.getDefault();

        Date date0 = new Date(0 - timeZone.getOffset(0));
        Date date1 = new Date(s1 - timeZone.getOffset(s1));
        Date date2 = new Date(s2 - timeZone.getOffset(s2));

        Assert.assertEquals(DateUtils.dateToStr(date0), "1970-01-01 00:00:00");
        Assert.assertEquals(DateUtils.dateToStr(date1), "2017-07-27 03:56:42");
        Assert.assertEquals(DateUtils.dateToStr(date2), "2017-07-27 03:57:15");
    }

    @Test
    public void dateToStr1() throws Exception {

        long d0 = 0l;
        long d1 = 1501127802975l; // 2017-07-27 11:56:42:975 +8
        long d2 = 1501127835658l; // 2017-07-27 11:57:15:658 +8
        TimeZone timeZone = TimeZone.getDefault();
        Date date0 = new Date(d0 - timeZone.getOffset(d0));
        Date date1 = new Date(d1 - timeZone.getOffset(d1));
        Date date2 = new Date(d2 - timeZone.getOffset(d2));

        Assert.assertEquals(DateUtils.dateToStr(date0, DateUtils.DATE_FORMAT_MILLS_TIME), "1970-01-01 00:00:00.000");
        Assert.assertEquals(DateUtils.dateToStr(date1, DateUtils.DATE_FORMAT_MILLS_TIME), "2017-07-27 03:56:42.975");
        Assert.assertEquals(DateUtils.dateToStr(date2, DateUtils.DATE_FORMAT_MILLS_TIME), "2017-07-27 03:57:15.658");
    }

    @Test
    public void strToDate() throws Exception {
        long d0 = 0l;
        long d1 = 1501127802000l; // 2017-07-27 11:56:42:975 +8
        long d2 = 1501127835000l; // 2017-07-27 11:57:15:658 +8
        TimeZone timeZone = TimeZone.getDefault();
        Date date0 = new Date(d0 - timeZone.getOffset(d0));
        Date date1 = new Date(d1 - timeZone.getOffset(d1));
        Date date2 = new Date(d2 - timeZone.getOffset(d2));

        String s0 = "1970-01-01 00:00:00";
        String s1 = "2017-07-27 03:56:42";
        String s2 = "2017-07-27 03:57:15";

        Assert.assertEquals(DateUtils.strToDate(s0).getTime(), date0.getTime());
        Assert.assertEquals(DateUtils.strToDate(s1).getTime(), date1.getTime());
        Assert.assertEquals(DateUtils.strToDate(s2).getTime(), date2.getTime());

    }

    @Test
    public void strToDate1() throws Exception {

        long d0 = 0l;
        long d1 = 1501127802975l; // 2017-07-27 11:56:42:975 +8
        long d2 = 1501127835658l; // 2017-07-27 11:57:15:658 +8
        TimeZone timeZone = TimeZone.getDefault();
        Date date0 = new Date(d0 - timeZone.getOffset(d0));
        Date date1 = new Date(d1 - timeZone.getOffset(d1));
        Date date2 = new Date(d2 - timeZone.getOffset(d2));

        String s0 = "1970-01-01 00:00:00.000";
        String s1 = "2017-07-27 03:56:42.975";
        String s2 = "2017-07-27 03:57:15.658";

        Assert.assertEquals(DateUtils.strToDate(s0, DateUtils.DATE_FORMAT_MILLS_TIME).getTime(), date0.getTime());
        Assert.assertEquals(DateUtils.strToDate(s1, DateUtils.DATE_FORMAT_MILLS_TIME).getTime(), date1.getTime());
        Assert.assertEquals(DateUtils.strToDate(s2, DateUtils.DATE_FORMAT_MILLS_TIME).getTime(), date2.getTime());
    }

    @Test
    public void dateToMillisStr() throws Exception {
        long d0 = 0l;
        long d1 = 1501127802975l; // 2017-07-27 11:56:42:975 +8
        long d2 = 1501127835658l; // 2017-07-27 11:57:15:658 +8
        TimeZone timeZone = TimeZone.getDefault();
        Date date0 = new Date(d0 - timeZone.getOffset(d0));
        Date date1 = new Date(d1 - timeZone.getOffset(d1));
        Date date2 = new Date(d2 - timeZone.getOffset(d2));

        Assert.assertEquals(DateUtils.dateToMillisStr(date0), "1970-01-01 00:00:00.000");
        Assert.assertEquals(DateUtils.dateToMillisStr(date1), "2017-07-27 03:56:42.975");
        Assert.assertEquals(DateUtils.dateToMillisStr(date2), "2017-07-27 03:57:15.658");
    }

    @Test
    public void millisStrToDate() throws Exception {
        long d0 = 0l;
        long d1 = 1501127802975l; // 2017-07-27 11:56:42:975 +8
        long d2 = 1501127835658l; // 2017-07-27 11:57:15:658 +8
        TimeZone timeZone = TimeZone.getDefault();
        Date date0 = new Date(d0 - timeZone.getOffset(d0));
        Date date1 = new Date(d1 - timeZone.getOffset(d1));
        Date date2 = new Date(d2 - timeZone.getOffset(d2));

        String s0 = "1970-01-01 00:00:00.000";
        String s1 = "2017-07-27 03:56:42.975";
        String s2 = "2017-07-27 03:57:15.658";

        Assert.assertEquals(DateUtils.millisStrToDate(s0).getTime(), date0.getTime());
        Assert.assertEquals(DateUtils.millisStrToDate(s1).getTime(), date1.getTime());
        Assert.assertEquals(DateUtils.millisStrToDate(s2).getTime(), date2.getTime());
    }

}