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
package com.alipay.sofa.rpc.transmit.registry;

import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory of transmit registry
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
public class TransmitRegistryFactory {

    /**
     * 全部转发注册中心
     */
    private final static ConcurrentHashMap<String, TransmitRegistry> REGISTRY_MAP = new ConcurrentHashMap<String, TransmitRegistry>();

    public synchronized static TransmitRegistry getIpTransmitRegistry(RegistryConfig registryConfig) {
        try {
            TransmitRegistry registry = REGISTRY_MAP.get(registryConfig.getProtocol());
            if (registry == null) {

                ExtensionClass<TransmitRegistry> ext = ExtensionLoaderFactory
                    .getExtensionLoader(TransmitRegistry.class)
                    .getExtensionClass(registryConfig.getProtocol());
                if (ext == null) {
                    throw ExceptionUtils.buildRuntime("ip transmit registry protocol", registryConfig.getProtocol(),
                        "Unsupported protocol of server!");
                }
                registry = ext.getExtInstance();
                registry.init(registryConfig);
                REGISTRY_MAP.put(registryConfig.getProtocol(), registry);
            }
            return registry;
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(e.getMessage(), e);
        }
    }

}