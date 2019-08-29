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
package com.alipay.sofa.rpc.codec.bolt;

import java.util.Iterator;
import java.util.Map;

/**
 * Map data converter (flat/tree)
 * 
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ContextMapConverter {

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
