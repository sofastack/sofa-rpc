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

import com.alipay.sofa.rpc.common.json.JSON;

/**
 * 内置Json工具类
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public final class JSONUtils {

    /**
     * 对象转为json字符串
     *
     * @param object 对象
     * @return json字符串
     */
    public static String toJSONString(Object object) {
        return JSON.toJSONString(object);
    }

    /**
     * 解析为指定对象
     *
     * @param text  json字符串
     * @param clazz 指定类
     * @param <T>   指定对象
     * @return 指定对象
     */
    public static <T> T parseObject(String text, Class<T> clazz) {
        return JSON.parseObject(text, clazz);
    }

}
