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
package com.alipay.sofa.rpc.config;

import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GrpcInterceptorManager {

    /**
     * 自定义jaxrs Consumer 拦截器
     */
    private static List<ClientInterceptor> customConsumerInstances = Collections
                                                                       .synchronizedList(new ArrayList<ClientInterceptor>());

    /**
     * 自定义jaxrs Provider 拦截器
     */
    private static List<ServerInterceptor> customProviderInstances = Collections
                                                                       .synchronizedList(new ArrayList<ServerInterceptor>());

    /**
     * 获取全部内置的jaxrs Provider类
     *
     * @return 全部内置的jaxrs Provider类
     */
    public static List<ServerInterceptor> getInternalProviderClasses() {
        return customProviderInstances;
    }

    /**
     * 注册自定义jaxrs Provider实例
     */
    public static void registerCustomProviderInstance(ServerInterceptor provider) {
        customProviderInstances.add(provider);
    }

    /**
     * remove custom jaxrs provider instace
     *
     * @param provider
     */
    public static void removeCustomProviderInstance(ServerInterceptor provider) {
        customProviderInstances.remove(provider);
    }

    /**
     * remove custom jaxrs provider instace
     *
     * @param provider
     */
    public static void removeCustomProviders() {
        customProviderInstances.clear();
    }

    /**
     * 获取全部内置的jaxrs Provider类
     *
     * @return 全部内置的jaxrs Provider类
     */
    public static List<ClientInterceptor> getInternalConsumerClasses() {
        return customConsumerInstances;
    }

    /**
     * 注册自定义jaxrs Provider实例
     */
    public static void registerCustomConsumerInstance(ClientInterceptor provider) {
        customConsumerInstances.add(provider);
    }

    /**
     * remove custom jaxrs provider instace
     *
     * @param provider
     */
    public static void removeCustomConsumers() {
        customConsumerInstances.clear();
    }

    public static void removeCustomConsumerInstance(ClientInterceptor provider) {
        customConsumerInstances.remove(provider);
    }

}
