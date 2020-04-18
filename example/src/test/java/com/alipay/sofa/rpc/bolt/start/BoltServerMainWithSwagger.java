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
package com.alipay.sofa.rpc.bolt.start;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.doc.swagger.rest.SwaggerRestService;
import com.alipay.sofa.rpc.doc.swagger.rest.SwaggerRestServiceImpl;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.EchoService;
import com.alipay.sofa.rpc.test.EchoServiceImpl;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;

/**
 * @author <a href=mailto:orezsilence@163.com>zhangchengxi</a>
 */
public class BoltServerMainWithSwagger {

    private final static Logger LOGGER = LoggerFactory.getLogger(BoltServerMain.class);

    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-server");

        ServerConfig serverConfig = new ServerConfig()
            .setPort(22000)
            .setDaemon(false);

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(application)
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);

        ProviderConfig<EchoService> providerConfig2 = new ProviderConfig<EchoService>()
            .setInterfaceId(EchoService.class.getName())
            .setApplication(application)
            .setRef(new EchoServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();
        providerConfig2.export();

        ServerConfig restServer = new ServerConfig()
            .setProtocol("rest")
            .setPort(8888)
            .setDaemon(false);

        ProviderConfig<SwaggerRestService> restProviderConfig = new ProviderConfig<SwaggerRestService>()
            .setInterfaceId(SwaggerRestService.class.getName())
            .setApplication(application)
            .setRef(new SwaggerRestServiceImpl())
            .setBootstrap("rest")
            .setServer(restServer)
            .setRegister(false);

        restProviderConfig.export();

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);
    }

}
