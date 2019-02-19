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
package com.alipay.sofa.rpc.dynamic;

import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.config.DynamicConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author bystander
 * @version $Id: DynamicConfigerFactory.java, v 0.1 2018年12月26日 20:29 bystander Exp $
 */
public class DynamicConfigerFactory {

    /**
     * 保存全部的配置和注册中心实例
     */
    private final static ConcurrentMap<DynamicConfig, DynamicConfiger> ALL_DYNAMIC_CONFIGER = new ConcurrentHashMap<DynamicConfig, DynamicConfiger>();

    /**
     * may add paramter to RegistryConfig
     *
     * @param registryConfig
     * @return
     */
    public static synchronized DynamicConfiger getDynamicConfig(DynamicConfig registryConfig) {

        try {
            // 注意：RegistryConfig重写了equals方法，如果多个RegistryConfig属性一样，则认为是一个对象
            DynamicConfiger registry = ALL_DYNAMIC_CONFIGER.get(registryConfig);
            if (registry == null) {
                ExtensionClass<DynamicConfiger> ext = ExtensionLoaderFactory.getExtensionLoader(DynamicConfiger.class)
                    .getExtensionClass(registryConfig.getProtocol());
                if (ext == null) {
                    throw ExceptionUtils.buildRuntime("registry.protocol", registryConfig.getProtocol(),
                        "Unsupported protocol of registry config !");
                }
                registry = ext.getExtInstance(new Class[] { DynamicConfig.class }, new Object[] { registryConfig });

                //TODO optimize me
                registry.init();

                ALL_DYNAMIC_CONFIGER.put(registryConfig, registry);
            }
            return registry;
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(e.getMessage(), e);
        }
    }
}