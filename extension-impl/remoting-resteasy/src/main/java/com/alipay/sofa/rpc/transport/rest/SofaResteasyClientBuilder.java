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
package com.alipay.sofa.rpc.transport.rest;

import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.JAXRSProviderManager;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import java.util.Set;

/**
 * 
 * 
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class SofaResteasyClientBuilder extends ResteasyClientBuilder {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SofaResteasyClientBuilder.class);

    /**
     * 注册jaxrs Provider
     *
     * @return SofaResteasyClientBuilder
     */
    public SofaResteasyClientBuilder registerProvider() {
        ResteasyProviderFactory providerFactory = getProviderFactory();
        // 注册内置
        Set<Class> internalProviderClasses = JAXRSProviderManager.getInternalProviderClasses();
        if (CommonUtils.isNotEmpty(internalProviderClasses)) {
            for (Class providerClass : internalProviderClasses) {
                providerFactory.register(providerClass);
            }
        }
        // 注册自定义
        Set<Object> customProviderInstances = JAXRSProviderManager.getCustomProviderInstances();
        if (CommonUtils.isNotEmpty(customProviderInstances)) {
            for (Object provider : customProviderInstances) {
                PropertyInjector propertyInjector = providerFactory.getInjectorFactory()
                    .createPropertyInjector(
                        JAXRSProviderManager.getTargetClass(provider), providerFactory);
                propertyInjector.inject(provider);
                providerFactory.registerProviderInstance(provider);
            }
        }

        return this;
    }

    public SofaResteasyClientBuilder logProviders() {
        if (LOGGER.isDebugEnabled()) {
            Set pcs = getProviderFactory().getProviderClasses();
            StringBuilder sb = new StringBuilder();
            sb.append("\ndefault-providers:\n");

            for (Object provider : pcs) {
                sb.append("  ").append(provider).append("\n");
            }
            LOGGER.debug(sb.toString());
        }
        return this;
    }
}