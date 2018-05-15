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
package com.alipay.sofa.rpc.codec.sofahessian;

import com.caucho.hessian.io.Deserializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 适配多ClassLoader环境，可切换业务的ClassLoader
 *
 * @author <a href=mailto:hongwei.yhw@antfin.com>HongWei Yi</a>
 */
public class MultipleClassLoaderSofaSerializerFactory extends SingleClassLoaderSofaSerializerFactory {

    private final ConcurrentMap<String, ConcurrentMap<ClassLoader, Deserializer>> cachedTypeDeserializerMap = new ConcurrentHashMap<String, ConcurrentMap<ClassLoader, Deserializer>>();

    @Override
    protected Deserializer getDeserializerFromCachedType(String type) {
        Map<ClassLoader, Deserializer> map = cachedTypeDeserializerMap.get(type);
        if (map == null) {
            return null;
        }

        return map.get(Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected void putDeserializerToCachedType(String type, Deserializer deserializer) {

        ConcurrentMap<ClassLoader, Deserializer> concurrentMap = cachedTypeDeserializerMap
            .get(type);

        if (concurrentMap == null) {
            ConcurrentMap<ClassLoader, Deserializer> newMap = new ConcurrentHashMap<ClassLoader, Deserializer>();
            concurrentMap = cachedTypeDeserializerMap.putIfAbsent(type, newMap);
            if (concurrentMap == null) {
                concurrentMap = newMap;
            }
        }

        concurrentMap.put(Thread.currentThread().getContextClassLoader(), deserializer);
    }
}
