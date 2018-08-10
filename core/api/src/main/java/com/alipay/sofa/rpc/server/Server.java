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
package com.alipay.sofa.rpc.server;

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.Extensible;
import com.alipay.sofa.rpc.invoke.Invoker;

/**
 * Server SPI
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
public interface Server extends Destroyable {
    /**
     * 启动server端
     *
     * @param serverConfig ServerConfig
     */
    void init(ServerConfig serverConfig);

    /**
     * 启动
     */
    void start();

    /**
     * 是否已经启动
     *
     * @return 是否启动
     */
    boolean isStarted();

    /**
     * 是否还绑定了服务（没有可以销毁）
     *
     * @return has service entry
     */
    boolean hasNoEntry();

    /**
     * 停止
     */
    void stop();

    /**
     * 注册服务
     *
     * @param providerConfig 服务提供者配置
     * @param instance       服务提供者实例
     */
    void registerProcessor(ProviderConfig providerConfig, Invoker instance);

    /**
     * 取消注册服务
     *
     * @param providerConfig 服务提供者配置
     * @param closeIfNoEntry 如果没有注册服务，最后一个关闭Server
     */
    void unRegisterProcessor(ProviderConfig providerConfig, boolean closeIfNoEntry);
}
