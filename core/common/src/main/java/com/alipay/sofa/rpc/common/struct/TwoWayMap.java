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
package com.alipay.sofa.rpc.common.struct;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * two-way map, you can get key by value, or get value by key.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@ThreadSafe
public class TwoWayMap<K, V> implements Map<K, V> {

    private ConcurrentMap<K, V> kvMap = new ConcurrentHashMap<K, V>();

    private ConcurrentMap<V, K> vkMap = new ConcurrentHashMap<V, K>();

    private ReentrantLock       lock  = new ReentrantLock();

    @Override
    public int size() {
        return kvMap.size();
    }

    @Override
    public boolean isEmpty() {
        return kvMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return kvMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return vkMap.containsKey(value);
    }

    @Override
    public V get(Object key) {
        return kvMap.get(key);
    }

    public K getKey(Object value) {
        return vkMap.get(value);
    }

    @Override
    public V put(K key, V value) {
        lock.lock();
        try {
            kvMap.put(key, value);
            vkMap.put(value, key);
            return value;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        lock.lock();
        try {
            V value = kvMap.remove(key);
            vkMap.remove(value);
            return value;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        lock.tryLock();
        try {
            kvMap.clear();
            vkMap.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        return kvMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return kvMap.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return kvMap.entrySet();
    }
}
