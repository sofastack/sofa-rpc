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
package com.alipay.sofa.rpc.common;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author zhaowang
 * @version : MetadataHolder.java, v 0.1 2020年09月09日 4:09 下午 zhaowang Exp $
 */
public class MetadataHolder {
    static final ThreadLocal<Map<String,String>> localContext = new ThreadLocal<>();

    public static Map<String, String> getMetaHolder() {
        Map<String, String> stringStringMap = localContext.get();
        if(stringStringMap == null){
            LinkedHashMap<String, String> value = new LinkedHashMap<>();
            localContext.set(value);
            return value;
        }
        return stringStringMap;
    }

    public static void clear(){
        localContext.remove();
    }
}