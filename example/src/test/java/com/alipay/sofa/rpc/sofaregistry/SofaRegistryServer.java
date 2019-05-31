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
package com.alipay.sofa.rpc.sofaregistry;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 * Quick Start Server
 */
public class SofaRegistryServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SofaRegistryServer.class);

    public static void main(String[] args) {
        // 1. 注册中心配置
        /**
         * 运行时项目引入依赖
         <dependency>
             <groupId>com.alipay.sofa</groupId>
             <artifactId>registry-client-all</artifactId>
             <version>5.2.0</version>
         </dependency>
         */
        RegistryConfig registryConfig = new RegistryConfig()
                .setProtocol("sofa") // 设置协议
                .setAddress("127.0.0.1:9603"); // 设置注册中心地址

        // 2. 通信服务配置
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt") // 设置一个协议，默认bolt
            .setPort(12200) // 设置一个端口，默认12200
            .setDaemon(false); // 非守护线程

        // 3. provider 综合配置
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
                .setRegistry(registryConfig)
                .setInterfaceId(HelloService.class.getName()) // 指定接口
                .setRef(new HelloServiceImpl()) // 指定实现
                .setServer(serverConfig); // 指定服务端

        providerConfig.export(); // 发布服务

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);
    }
}