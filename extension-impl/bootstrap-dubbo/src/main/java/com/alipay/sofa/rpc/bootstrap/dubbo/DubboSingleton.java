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
package com.alipay.sofa.rpc.bootstrap.dubbo;

import com.alibaba.dubbo.config.DubboShutdownHook;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Save singleton object of dubbo
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class DubboSingleton {

    static {
        RpcRuntimeContext.registryDestroyHook(new Destroyable.DestroyHook() {
            @Override
            public void preDestroy() {
                destroyAll();
            }

            @Override
            public void postDestroy() {

            }
        });
    }

    /**
     * sofa.SeverConfig --> dubbo.ProtocolConfig
     */
    final static ConcurrentMap<ServerConfig, ProtocolConfig>                            SERVER_MAP   = new ConcurrentHashMap<ServerConfig, ProtocolConfig>();

    /**
     * sofa.RegistryConfig --> dubbo.RegistryConfig
     */
    final static ConcurrentMap<RegistryConfig, com.alibaba.dubbo.config.RegistryConfig> REGISTRY_MAP = new ConcurrentHashMap<RegistryConfig, com.alibaba.dubbo.config.RegistryConfig>();

    /**
     * Destroy all dubbo resources
     */
    public static void destroyAll() {
        DubboShutdownHook.getDubboShutdownHook().destroyAll();
    }
}
