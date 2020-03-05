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
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author bystander
 * @version : DynamicManagerFactory.java, v 0.1 2019年04月12日 11:36 bystander Exp $
 */
public class DynamicConfigManagerFactory {

    /**
     * 保存全部的配置和注册中心实例
     */
    private final static ConcurrentMap<String, DynamicConfigManager> ALL_DYNAMICS = new ConcurrentHashMap<String, DynamicConfigManager>();

    /**
     * slf4j Logger for this class
     */
    private final static Logger                                      LOGGER       = LoggerFactory
                                                                                      .getLogger(DynamicConfigManagerFactory.class);

    /**
     * 得到动态配置管理
     *
     * @param alias 别名
     * @return DynamicManager 实现
     */
    public static synchronized DynamicConfigManager getDynamicManager(String appName, String alias) {
        if (ALL_DYNAMICS.size() > 3) { // 超过3次 是不是配错了？
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Size of dynamic manager is greater than 3, Please check it!");
            }
        }
        try {
            // 注意：RegistryConfig重写了equals方法，如果多个RegistryConfig属性一样，则认为是一个对象
            DynamicConfigManager registry = ALL_DYNAMICS.get(alias);
            if (registry == null) {
                ExtensionClass<DynamicConfigManager> ext = ExtensionLoaderFactory.getExtensionLoader(
                    DynamicConfigManager.class)
                    .getExtensionClass(alias);
                if (ext == null) {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT, "DynamicConfigManager",
                        alias));
                }
                registry = ext.getExtInstance(new Class[] { String.class }, new Object[] { appName });
                ALL_DYNAMICS.put(alias, registry);
            }
            return registry;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_LOAD_EXT, "DynamicConfigManager", alias),
                e);
        }
    }

}