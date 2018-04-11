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

import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.ext.Extensible;

/**
 * Registry of transmit.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extensible
public interface TransmitRegistry {

    /**
     * 初始化
     * @param registryConfig 配置
     */
    void init(RegistryConfig registryConfig);

    /**
     * 注册转发节点
     *
     * @param appName 应用
     * @param dataId  服务
     */
    void register(String appName, String dataId);

    /**
     * 订阅转发节点
     *
     * @param appName  应用
     * @param dataId   服务
     * @param callback 订阅回调
     */
    void subscribe(final String appName, String dataId, TransmitRegistryCallback callback);

    /**
     * 销毁资源
     */
    void destroy();
}
