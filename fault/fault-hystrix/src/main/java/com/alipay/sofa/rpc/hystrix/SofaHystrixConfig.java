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

import com.alipay.sofa.rpc.config.ConsumerConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hystrix Config
 *
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class SofaHystrixConfig {

    private static final Map<ConsumerConfig, FallbackFactory> FALLBACK_FACTORY_MAPPING = new ConcurrentHashMap<ConsumerConfig, FallbackFactory>();

    private static final Map<ConsumerConfig, SetterFactory>   SETTER_FACTORY_MAPPING   = new ConcurrentHashMap<ConsumerConfig, SetterFactory>();

    private static FallbackFactory                            GLOBAL_FALLBACK_FACTORY  = new NoopFallbackFactory();

    private static SetterFactory                              GLOBAL_SETTER_FACTORY    = new DefaultSetterFactory();

    public static void registerFallback(ConsumerConfig consumerConfig, Object fallback) {
        FALLBACK_FACTORY_MAPPING.put(consumerConfig, new DefaultFallbackFactory<Object>(fallback));
    }

    public static void clearFallback() {
        FALLBACK_FACTORY_MAPPING.clear();
    }

    public static void registerFallbackFactory(ConsumerConfig consumerConfig, FallbackFactory fallbackFactory) {
        FALLBACK_FACTORY_MAPPING.put(consumerConfig, fallbackFactory);
    }

    public static void registerSetterFactory(ConsumerConfig consumerConfig, SetterFactory setterFactory) {
        SETTER_FACTORY_MAPPING.put(consumerConfig, setterFactory);
    }

    public static void clearSetterFactory() {
        SETTER_FACTORY_MAPPING.clear();
    }

    public static void registerGlobalFallbackFactory(FallbackFactory fallbackFactory) {
        GLOBAL_FALLBACK_FACTORY = fallbackFactory;
    }

    public static void registerGlobalSetterFactory(SetterFactory setterFactory) {
        GLOBAL_SETTER_FACTORY = setterFactory;
    }

    public static FallbackFactory loadFallbackFactory(ConsumerConfig consumerConfig) {
        FallbackFactory fallbackFactory = FALLBACK_FACTORY_MAPPING.get(consumerConfig);
        if (fallbackFactory != null) {
            return fallbackFactory;
        }
        return GLOBAL_FALLBACK_FACTORY;
    }

    public static SetterFactory loadSetterFactory(ConsumerConfig consumerConfig) {
        SetterFactory setterFactory = SETTER_FACTORY_MAPPING.get(consumerConfig);
        if (setterFactory != null) {
            return setterFactory;
        }
        return GLOBAL_SETTER_FACTORY;
    }
}