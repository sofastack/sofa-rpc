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
package com.alipay.sofa.rpc.transport.triple.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of HttpHeaders using a LinkedHashMap.
 * This implementation preserves header insertion order and supports multiple values per header.
 */
public class DefaultHttpHeaders implements HttpHeaders {

    private final Map<String, List<String>> headers;

    public DefaultHttpHeaders() {
        this.headers = new LinkedHashMap<>();
    }

    public DefaultHttpHeaders(int initialCapacity) {
        this.headers = new LinkedHashMap<>(initialCapacity);
    }

    @Override
    public String get(String name) {
        if (name == null) {
            return null;
        }
        List<String> values = headers.get(name.toLowerCase());
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public HttpHeaders set(String name, String value) {
        if (name == null || value == null) {
            return this;
        }
        List<String> values = new ArrayList<>(1);
        values.add(value);
        headers.put(name.toLowerCase(), values);
        return this;
    }

    @Override
    public HttpHeaders add(String name, String value) {
        if (name == null || value == null) {
            return this;
        }
        String key = name.toLowerCase();
        List<String> values = headers.computeIfAbsent(key, k -> new ArrayList<>(1));
        values.add(value);
        return this;
    }

    @Override
    public HttpHeaders remove(String name) {
        if (name == null) {
            return this;
        }
        headers.remove(name.toLowerCase());
        return this;
    }

    @Override
    public boolean contains(String name) {
        if (name == null) {
            return false;
        }
        return headers.containsKey(name.toLowerCase());
    }

    @Override
    public Iterable<String> getAll(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        List<String> values = headers.get(name.toLowerCase());
        return values != null ? values : Collections.emptyList();
    }

    @Override
    public Set<String> names() {
        return new LinkedHashSet<>(headers.keySet());
    }

    @Override
    public boolean isEmpty() {
        return headers.isEmpty();
    }

    @Override
    public int size() {
        return headers.size();
    }

    @Override
    public void clear() {
        headers.clear();
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        List<Map.Entry<String, String>> entries = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                entries.add(new Map.Entry<String, String>() {
                    private final String key = entry.getKey();
                    private String val = value;

                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return val;
                    }

                    @Override
                    public String setValue(String newValue) {
                        String old = this.val;
                        this.val = newValue;
                        return old;
                    }
                });
            }
        }
        return entries.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DefaultHttpHeaders{");
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}