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
package com.alipay.sofa.rpc.dubbo.start;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.EchoService;
import com.alipay.sofa.rpc.test.EchoServiceImpl;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;

/**
 *
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class DubboServerMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(DubboServerMain.class);

    public static void main(String[] args) {
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("dubbo-server");

        ServerConfig serverConfig = new ServerConfig()
            //        .setHost("0.0.0.0")
            //        .setPort(22222)
            .setProtocol("dubbo")
            .setHost("127.0.0.1")
            .setPort(20080)
            .setSerialization("hessian2")
            .setDaemon(false);

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setBootstrap("dubbo")
            .setApplication(applicationConfig)
            .setRef(new HelloServiceImpl())
            .setUniqueId("xxx")
            .setServer(serverConfig)
            .setRegister(false);

        ProviderConfig<EchoService> providerConfig2 = new ProviderConfig<EchoService>()
            .setInterfaceId(EchoService.class.getName())
            .setRef(new EchoServiceImpl())
            .setApplication(applicationConfig)
            .setBootstrap("dubbo")
            .setUniqueId("xxx")
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();
        providerConfig2.export();

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);

        synchronized (DubboServerMain.class) {
            while (true) {
                try {
                    DubboServerMain.class.wait();
                } catch (InterruptedException e) {

                }
            }
        }
    }

}
