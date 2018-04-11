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
package com.alipay.sofa.rpc.invoke.future;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.test.EchoService;
import com.alipay.sofa.rpc.test.EchoServiceImpl;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;

/**
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class FutureServerMain {

    public static void main(String[] args) {

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("future-server");

        ServerConfig serverConfig2 = new ServerConfig()
            .setPort(22222)
            .setDaemon(false);

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setApplication(applicationConfig)
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(1000))
            .setServer(serverConfig2);
        providerConfig.export();

        ProviderConfig<EchoService> providerConfig2 = new ProviderConfig<EchoService>()
            .setApplication(applicationConfig)
            .setInterfaceId(EchoService.class.getName())
            .setRef(new EchoServiceImpl())
            .setServer(serverConfig2);
        providerConfig2.export();
    }
}
