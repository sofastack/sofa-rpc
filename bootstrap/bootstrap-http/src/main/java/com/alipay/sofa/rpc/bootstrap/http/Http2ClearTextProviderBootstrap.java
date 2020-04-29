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
package com.alipay.sofa.rpc.bootstrap.http;

import com.alipay.sofa.rpc.bootstrap.DefaultProviderBootstrap;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.ext.Extension;

/**
 *
 * @author <a href="mailto:zhiyuan.lzy@antfin.com">zhiyuan.lzy</a>
 */
@Extension("h2c")
public class Http2ClearTextProviderBootstrap<T> extends DefaultProviderBootstrap<T> {
    /**
     * 构造函数
     *
     * @param providerConfig 服务发布者配置
     */
    protected Http2ClearTextProviderBootstrap(ProviderConfig<T> providerConfig) {
        super(providerConfig);
    }
}