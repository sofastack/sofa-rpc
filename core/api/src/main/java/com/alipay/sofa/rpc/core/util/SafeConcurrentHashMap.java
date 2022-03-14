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
package com.alipay.sofa.rpc.core.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author zhaowang
 * @version : SafeConcurrentHashMap.java, v 0.1 2021年12月16日 3:19 下午 zhaowang
 */
public class SafeConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {

    @Override
    public V putIfAbsent(K key, V value) {
        if (key != null && value != null) {
            return super.putIfAbsent(key, value);
        }
        return get(key);
    }

    @Override
    public V put(K key, V value) {
        if (key != null && value != null) {
            return super.put(key, value);
        }
        return get(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            return null;
        }
        return super.get(key);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null) {
            return null;
        }
        return super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null) {
            return null;
        }
        return super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null) {
            return null;
        }
        return super.compute(key, remappingFunction);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        // if value == null ,HashMap will throw NPE
        if (key == null && value != null) {
            return null;
        }
        return super.merge(key, value, remappingFunction);
    }
}