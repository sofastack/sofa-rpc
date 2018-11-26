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
package com.alipay.sofa.rpc.bootstrap;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.ext.Extensible;

/**
 * 发布服务的包装类，包括具体的启动后的对象
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
public abstract class ProviderBootstrap<T> {

    /**
     * 服务发布者配置
     */
    protected final ProviderConfig<T> providerConfig;

    /**
     * 构造函数
     *
     * @param providerConfig 服务发布者配置
     */
    protected ProviderBootstrap(ProviderConfig<T> providerConfig) {
        this.providerConfig = providerConfig;
    }

    /**
     * 得到服务发布者配置
     *
     * @return 服务发布者配置
     */
    public ProviderConfig<T> getProviderConfig() {
        return providerConfig;
    }

    /**
     * 发布一个服务
     */
    public abstract void export();

    /**
     * 取消发布一个服务
     */
    public abstract void unExport();
}
