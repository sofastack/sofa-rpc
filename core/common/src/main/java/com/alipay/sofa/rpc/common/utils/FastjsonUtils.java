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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.lang.reflect.Type;

/**
 * Fastjson 工具类
 *
 * @author chengming
 * @version FastjsonUtils.java, v 0.1 2024年03月14日 1:36 PM chengming
 */
public class FastjsonUtils {

    private static final ParserConfig safeParserConfig = new ParserConfig();

    public FastjsonUtils() {
    }

    public static ParserConfig getSafeInstance() {
        return safeParserConfig;
    }

    public String toJSONString(Object obj) {
        return JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect);
    }

    public <T> T toJavaObject(String json, Type type) {
        return JSON.parseObject(json, type);
    }

    public static JSONObject parseObject(String text) {
        Object obj = JSON.parse(text, safeParserConfig);
        if (obj instanceof JSONObject) {
            return (JSONObject) obj;
        } else {
            try {
                return (JSONObject) JSON.toJSON(obj);
            } catch (RuntimeException var3) {
                throw new JSONException("can not cast to JSONObject.", var3);
            }
        }
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        return JSON.parseObject(text, clazz, safeParserConfig, null, JSON.DEFAULT_PARSER_FEATURE, new Feature[0]);
    }

    public static <T> T parseObject(String text, TypeReference<T> type, Feature... features) {
        return JSON.parseObject(text, type.getType(), safeParserConfig, JSON.DEFAULT_PARSER_FEATURE, features);
    }

    public static Object read(String json, String path) {
        return JSONPath.compile(path).eval(JSON.parse(json, safeParserConfig));
    }

    static {
        safeParserConfig.setSafeMode(true);
    }
}