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
package com.alipay.sofa.rpc.registry.zk;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Zookeeper Observer
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public abstract class AbstractZookeeperObserver {

    /**
     * Init or add list.
     *
     * @param <K>
     *         the key parameter
     * @param <V>
     *         the value parameter
     * @param orginMap
     *         the orgin map
     * @param key
     *         the key
     * @param needAdd
     *         the need add
     */
    protected <K, V> void initOrAddList(Map<K, List<V>> orginMap, K key, V needAdd) {
        List<V> listeners = orginMap.get(key);
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList<V>();
            listeners.add(needAdd);
            orginMap.put(key, listeners);
        } else {
            listeners.add(needAdd);
        }
    }
}
