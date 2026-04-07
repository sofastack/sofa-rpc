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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * HTTP headers interface for TripleX.
 * Provides a unified abstraction for HTTP/1.1, HTTP/2, and HTTP/3 headers.
 */
public interface HttpHeaders extends Iterable<Map.Entry<String, String>> {

    /**
     * Get header value by name.
     *
     * @param name header name
     * @return header value, or null if not present
     */
    String get(String name);

    /**
     * Get header value by name, returning default if not present.
     *
     * @param name header name
     * @param defaultValue default value
     * @return header value or default
     */
    default String get(String name, String defaultValue) {
        String value = get(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Set header value.
     *
     * @param name header name
     * @param value header value
     * @return this instance for chaining
     */
    HttpHeaders set(String name, String value);

    /**
     * Add header value (allows multiple values for same name).
     *
     * @param name header name
     * @param value header value
     * @return this instance for chaining
     */
    HttpHeaders add(String name, String value);

    /**
     * Remove header by name.
     *
     * @param name header name
     * @return this instance for chaining
     */
    HttpHeaders remove(String name);

    /**
     * Check if header exists.
     *
     * @param name header name
     * @return true if header exists
     */
    boolean contains(String name);

    /**
     * Get all values for a header name.
     *
     * @param name header name
     * @return iterable of values
     */
    Iterable<String> getAll(String name);

    /**
     * Get all header names.
     *
     * @return set of header names
     */
    Set<String> names();

    /**
     * Check if headers are empty.
     *
     * @return true if empty
     */
    boolean isEmpty();

    /**
     * Get the number of headers.
     *
     * @return header count
     */
    int size();

    /**
     * Clear all headers.
     */
    void clear();

    /**
     * Get iterator over header entries.
     *
     * @return iterator
     */
    @Override
    Iterator<Map.Entry<String, String>> iterator();
}