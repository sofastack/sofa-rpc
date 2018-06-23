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

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Load {@link FallbackFactory} and fallback from {@link com.alipay.sofa.rpc.config.ConsumerConfig}
 */
public class FallbackFactoryLoader {

    private static final Map<AbstractInterfaceConfig, NullableFallbackFactory> FALLBACK_FACTORY_CACHE = new ConcurrentHashMap<AbstractInterfaceConfig, NullableFallbackFactory>();

    public static FallbackFactory load(AbstractInterfaceConfig consumerConfig) {
        if (!FALLBACK_FACTORY_CACHE.containsKey(consumerConfig)) {
            synchronized (HystrixFilter.class) {
                if (!FALLBACK_FACTORY_CACHE.containsKey(consumerConfig)) {
                    FALLBACK_FACTORY_CACHE.put(consumerConfig, init(consumerConfig));
                }
            }
        }
        return FALLBACK_FACTORY_CACHE.get(consumerConfig).getFallbackFactory();
    }

    private static NullableFallbackFactory init(AbstractInterfaceConfig consumerConfig) {
        String fallbackFactoryClass = consumerConfig.getParameter(HystrixConstants.SOFA_HYSTRIX_FALLBACK_FACTORY);
        if (StringUtils.isNotBlank(fallbackFactoryClass)) {
            try {
                return new NullableFallbackFactory((FallbackFactory) Class.forName(fallbackFactoryClass).newInstance());
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to init fallback-factory: " + fallbackFactoryClass, e);
            }
        }
        String fallbackClass = consumerConfig.getParameter(HystrixConstants.SOFA_HYSTRIX_FALLBACK);
        if (StringUtils.isNotBlank(fallbackClass)) {
            try {
                return new NullableFallbackFactory(new DefaultFallbackFactory(Class.forName(fallbackClass)
                    .newInstance()));
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to init fallback: " + fallbackClass, e);
            }
        }
        return new NullableFallbackFactory(null);
    }

    private static class NullableFallbackFactory {

        private FallbackFactory fallbackFactory;

        public NullableFallbackFactory(FallbackFactory fallbackFactory) {
            this.fallbackFactory = fallbackFactory;
        }

        public FallbackFactory getFallbackFactory() {
            return fallbackFactory;
        }
    }
}
