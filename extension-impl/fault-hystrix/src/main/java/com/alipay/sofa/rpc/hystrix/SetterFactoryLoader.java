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
 * Load {@link SetterFactory} from {@link com.alipay.sofa.rpc.config.ConsumerConfig}
 */
public class SetterFactoryLoader {

    private static final Map<AbstractInterfaceConfig, SetterFactory> SETTER_FACTORY_CACHE = new ConcurrentHashMap<AbstractInterfaceConfig, SetterFactory>();

    public static SetterFactory load(AbstractInterfaceConfig consumerConfig) {
        if (!SETTER_FACTORY_CACHE.containsKey(consumerConfig)) {
            synchronized (HystrixFilter.class) {
                if (!SETTER_FACTORY_CACHE.containsKey(consumerConfig)) {
                    SETTER_FACTORY_CACHE.put(consumerConfig, init(consumerConfig));
                }
            }
        }
        return SETTER_FACTORY_CACHE.get(consumerConfig);
    }

    private static SetterFactory init(AbstractInterfaceConfig consumerConfig) {
        String setterFactoryClass = consumerConfig.getParameter(HystrixConstants.SOFA_HYSTRIX_SETTER_FACTORY);
        if (StringUtils.isNotBlank(setterFactoryClass)) {
            try {
                return (SetterFactory) Class.forName(setterFactoryClass).newInstance();
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to init setter-factory: " + setterFactoryClass, e);
            }
        }
        return new DefaultSetterFactory();
    }
}
