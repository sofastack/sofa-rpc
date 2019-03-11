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
package com.alipay.sofa.rpc.hystrix;

/**
 * Default implements, returns a constant fallback
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class DefaultFallbackFactory<T> implements FallbackFactory<T> {

    private T fallback;

    public DefaultFallbackFactory(T fallback) {
        this.fallback = fallback;
    }

    @Override
    public T create(FallbackContext context) {
        return fallback;
    }
}
