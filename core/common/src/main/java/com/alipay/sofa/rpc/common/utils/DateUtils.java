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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TODO 不要用 simpleDateFormat，有更快的
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class DateUtils {

    /**
     * 每秒毫秒数
     */
    public static final int    MILLISECONDS_PER_SECONDE = 1000;
    /**
     * 每分毫秒数 60*1000
     */
    public static final int    MILLISECONDS_PER_MINUTE  = 60000;
    /**
     * 每小时毫秒数 36*60*1000
     */
    public static final int    MILLISECONDS_PER_HOUR    = 3600000;
    /**
     * 每天毫秒数 24*60*60*1000;
     */
    public static final long   MILLISECONDS_PER_DAY     = 86400000;

    /**
     * 普通时间的格式
     */
    public static final String DATE_FORMAT_TIME         = "yyyy-MM-dd HH:mm:ss";
    /**
     * 毫秒级时间的格式
     */
    public static final String DATE_FORMAT_MILLS_TIME   = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 到下一分钟0秒的毫秒数
     *
     * @param rightnow 当前时间
     * @return the int 到下一分钟的毫秒数
     */
    public static int getDelayToNextMinute(long rightnow) {
        return (int) (MILLISECONDS_PER_MINUTE - (rightnow % MILLISECONDS_PER_MINUTE));
    }

    /**
     * 上一分钟的最后一毫秒
     *
     * @param rightnow 当前时间
     * @return 上一分钟的最后一毫秒
     */
    public static long getPreMinuteMills(long rightnow) {
        return rightnow - (rightnow % MILLISECONDS_PER_MINUTE) - 1;
    }

    /**
     * 得到时间字符串
     *
     * @param date 时间
     * @return 时间字符串
     */
    public static String dateToStr(Date date) {
        return dateToStr(date, DATE_FORMAT_TIME);
    }

    /**
     * 时间转字符串
     *
     * @param date   时间
     * @param format 格式化格式
     * @return 时间字符串
     */
    public static String dateToStr(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 字符串转时间
     *
     * @param dateStr 时间字符串
     * @return 时间字符串
     * @throws ParseException 解析异常
     */
    public static Date strToDate(String dateStr) throws ParseException {
        return strToDate(dateStr, DATE_FORMAT_TIME);
    }

    /**
     * 字符串转时间戳
     * @param dateStr 时间字符串
     * @return 时间戳
     * @throws ParseException 解析异常
     */
    public static Long strToLong(String dateStr) throws ParseException {
        return strToDate(dateStr).getTime();
    }

    /**
     * 字符串转时间
     *
     * @param dateStr 时间字符串
     * @param format  格式化格式
     * @return 时间字符串
     * @throws ParseException 解析异常
     */
    public static Date strToDate(String dateStr, String format) throws ParseException {
        return new SimpleDateFormat(format).parse(dateStr);
    }

    /**
     * 得到毫秒级时间字符串
     *
     * @param date 时间
     * @return 时间字符串
     */
    public static String dateToMillisStr(Date date) {
        return dateToStr(date, DATE_FORMAT_MILLS_TIME);
    }

    /**
     * 得到Date
     *
     * @param millisDateStr 毫秒级时间字符串
     * @return Date
     * @throws ParseException 解析异常
     */
    public static Date millisStrToDate(String millisDateStr) throws ParseException {
        return strToDate(millisDateStr, DATE_FORMAT_MILLS_TIME);
    }
}
