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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 兼容类型直接的转换
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class CompatibleTypeUtils {

    private CompatibleTypeUtils() {
    }

    /**
     * 兼容类型转换。
     * <ul>
     * <li> String -&gt; char, enum, Date </li>
     * <li> Number -&gt; Number </li>
     * <li> List -&gt; Array </li>
     * </ul>
     *
     * @param value 原始值
     * @param type  目标类型
     * @return 目标值
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object convert(Object value, Class<?> type) {
        if (value == null || type == null || type.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (value instanceof String) {
            String string = (String) value;
            if (char.class.equals(type) || Character.class.equals(type)) {
                if (string.length() != 1) {
                    throw new IllegalArgumentException(String.format("can not convert String(%s) to char!" +
                        " when convert String to char, the String must only 1 char.", string));
                }
                return string.charAt(0);
            } else if (type.isEnum()) {
                return Enum.valueOf((Class<Enum>) type, string);
            } else if (type == BigInteger.class) {
                return new BigInteger(string);
            } else if (type == BigDecimal.class) {
                return new BigDecimal(string);
            } else if (type == Short.class || type == short.class) {
                return Short.valueOf(string);
            } else if (type == Integer.class || type == int.class) {
                return Integer.valueOf(string);
            } else if (type == Long.class || type == long.class) {
                return Long.valueOf(string);
            } else if (type == Double.class || type == double.class) {
                return new Double(string);
            } else if (type == Float.class || type == float.class) {
                return new Float(string);
            } else if (type == Byte.class || type == byte.class) {
                return Byte.valueOf(string);
            } else if (type == Boolean.class || type == boolean.class) {
                return Boolean.valueOf(string);
            } else if (type == Date.class || type == java.sql.Date.class || type == java.sql.Time.class ||
                type == java.sql.Timestamp.class) {
                try {
                    if (type == Date.class) {
                        return DateUtils.strToDate(string, DateUtils.DATE_FORMAT_TIME);
                    } else if (type == java.sql.Date.class) {
                        return new java.sql.Date(DateUtils.strToLong(string));
                    } else if (type == java.sql.Timestamp.class) {
                        return new java.sql.Timestamp(DateUtils.strToLong(string));
                    } else {
                        return new java.sql.Time(DateUtils.strToLong(string));
                    }
                } catch (ParseException e) {
                    throw new IllegalStateException("Failed to parse date " + value + " by format " +
                        DateUtils.DATE_FORMAT_TIME + ", cause: " + e.getMessage(), e);
                }
            } else if (type == Class.class) {
                return ClassTypeUtils.getClass((String) value);
            }
        } else if (value instanceof Number) {
            Number number = (Number) value;
            if (type == byte.class || type == Byte.class) {
                return number.byteValue();
            } else if (type == short.class || type == Short.class) {
                return number.shortValue();
            } else if (type == int.class || type == Integer.class) {
                return number.intValue();
            } else if (type == long.class || type == Long.class) {
                return number.longValue();
            } else if (type == float.class || type == Float.class) {
                return number.floatValue();
            } else if (type == double.class || type == Double.class) {
                return number.doubleValue();
            } else if (type == BigInteger.class) {
                return BigInteger.valueOf(number.longValue());
            } else if (type == BigDecimal.class) {
                return BigDecimal.valueOf(number.doubleValue());
            } else if (type == Date.class) {
                return new Date(number.longValue());
            } else if (type == java.sql.Date.class) {
                return new java.sql.Date(number.longValue());
            } else if (type == java.sql.Time.class) {
                return new java.sql.Time(number.longValue());
            } else if (type == java.sql.Timestamp.class) {
                return new java.sql.Timestamp(number.longValue());
            }
        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            if (type.isArray()) {
                int length = collection.size();
                Object array = Array.newInstance(type.getComponentType(), length);
                int i = 0;
                for (Object item : collection) {
                    Array.set(array, i++, item);
                }
                return array;
            } else if (!type.isInterface()) {
                try {
                    Collection result = (Collection) type.newInstance();
                    result.addAll(collection);
                    return result;
                } catch (Throwable ignore) { // NOPMD
                }
            } else if (type == List.class) {
                return new ArrayList<Object>(collection);
            } else if (type == Set.class) {
                return new HashSet<Object>(collection);
            }
        } else if (value.getClass().isArray() && Collection.class.isAssignableFrom(type)) {
            Collection collection;
            if (!type.isInterface()) {
                try {
                    collection = (Collection) type.newInstance();
                } catch (Throwable e) {
                    collection = new ArrayList<Object>();
                }
            } else if (type == Set.class) {
                collection = new HashSet<Object>();
            } else {
                collection = new ArrayList<Object>();
            }
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                collection.add(Array.get(value, i));
            }
            return collection;
        }
        return value;
    }
}
