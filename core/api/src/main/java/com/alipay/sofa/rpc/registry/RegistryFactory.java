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
package com.alipay.sofa.rpc.registry;

import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory of Registry
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class RegistryFactory {

    /**
     * 保存全部的配置和注册中心实例
     */
    private final static ConcurrentMap<RegistryConfig, Registry> ALL_REGISTRIES = new ConcurrentHashMap<RegistryConfig, Registry>();

    /**
     * slf4j Logger for this class
     */
    private final static Logger                                  LOGGER         = LoggerFactory
                                                                                    .getLogger(RegistryFactory.class);

    /**
     * 得到注册中心对象
     *
     * @param registryConfig RegistryConfig类
     * @return Registry实现
     */
    public static synchronized Registry getRegistry(RegistryConfig registryConfig) {
        if (ALL_REGISTRIES.size() > 3) { // 超过3次 是不是配错了？
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Size of registry is greater than 3, Please check it!");
            }
        }
        String protocol = null;
        try {
            // 注意：RegistryConfig重写了equals方法，如果多个RegistryConfig属性一样，则认为是一个对象
            Registry registry = ALL_REGISTRIES.get(registryConfig);
            if (registry == null) {
                protocol = registryConfig.getProtocol();
                ExtensionClass<Registry> ext = ExtensionLoaderFactory.getExtensionLoader(Registry.class)
                    .getExtensionClass(protocol);
                if (ext == null) {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT, "Registry", protocol));
                }
                registry = ext.getExtInstance(new Class[] { RegistryConfig.class }, new Object[] { registryConfig });
                ALL_REGISTRIES.put(registryConfig, registry);
            }
            return registry;
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT, "Registry", protocol));
        }
    }

    /**
     * 得到全部注册中心配置
     *
     * @return 注册中心配置
     */
    public static List<RegistryConfig> getRegistryConfigs() {
        return new ArrayList<RegistryConfig>(ALL_REGISTRIES.keySet());
    }

    /**
     * 得到全部注册中心
     *
     * @return 注册中心
     */
    public static List<Registry> getRegistries() {
        return new ArrayList<Registry>(ALL_REGISTRIES.values());
    }

    /**
     * 关闭全部注册中心
     */
    public static void destroyAll() {
        for (Map.Entry<RegistryConfig, Registry> entry : ALL_REGISTRIES.entrySet()) {
            RegistryConfig config = entry.getKey();
            Registry registry = entry.getValue();
            try {
                registry.destroy();
                ALL_REGISTRIES.remove(config);
            } catch (Exception e) {
                LOGGER.error(LogCodes.getLog(LogCodes.ERROR_DESTRORY_REGISTRY, config), e);
            }
        }
    }
}
