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
package com.alipay.sofa.rpc.hystrix;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;

/**
 * @author BaoYi
 * @date 2021/12/26 1:22 PM
 */
public class HystrixServerMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(HystrixServerMain.class);

    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-hystrix");

        ServerConfig serverConfig = new ServerConfig()
            .setPort(22999)
            .setDaemon(false);

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(application)
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);
    }

}
