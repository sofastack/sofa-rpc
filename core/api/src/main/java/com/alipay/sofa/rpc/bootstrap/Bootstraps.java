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

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;

/**
 * <p>辅助工具类，更方便的使用发布方法</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class Bootstraps {

    /**
     * 发布一个服务
     *
     * @param providerConfig 服务发布者配置
     * @param <T>            接口类型
     * @return 发布启动类
     */
    public static <T> ProviderBootstrap<T> from(ProviderConfig<T> providerConfig) {
        ProviderBootstrap bootstrap = ExtensionLoaderFactory.getExtensionLoader(ProviderBootstrap.class)
            .getExtension(providerConfig.getBootstrap(),
                new Class[] { ProviderConfig.class },
                new Object[] { providerConfig });
        return (ProviderBootstrap<T>) bootstrap;
    }

    /**
     * 引用一个服务
     *
     * @param consumerConfig 服务消费者配置
     * @param <T>            接口类型
     * @return 引用启动类
     */
    public static <T> ConsumerBootstrap<T> from(ConsumerConfig<T> consumerConfig) {
        ConsumerBootstrap bootstrap = ExtensionLoaderFactory.getExtensionLoader(ConsumerBootstrap.class)
            .getExtension(consumerConfig.getBootstrap(),
                new Class[] { ConsumerConfig.class },
                new Object[] { consumerConfig });
        return (ConsumerBootstrap<T>) bootstrap;
    }
}
