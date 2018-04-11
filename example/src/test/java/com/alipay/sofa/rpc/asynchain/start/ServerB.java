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
package com.alipay.sofa.rpc.asynchain.start;

import com.alipay.sofa.rpc.asynchain.ServiceB;
import com.alipay.sofa.rpc.asynchain.ServiceBImpl;
import com.alipay.sofa.rpc.asynchain.ServiceC;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ServerB {

    public static void main(String[] args) {

        // B服务里的C服务客户端
        ConsumerConfig<ServiceC> consumerConfig = new ConsumerConfig<ServiceC>()
            .setApplication(new ApplicationConfig().setAppName("BBB"))
            .setInterfaceId(ServiceC.class.getName())
            .setDirectUrl("bolt://127.0.0.1:12299?appName=CCC")
            .setRegister(false)
            .setInvokeType("callback") // 不设置，调用级别可设置
            .setTimeout(2000);

        ServiceC serviceC = consumerConfig.refer();

        ServerConfig serverConfig = new ServerConfig()
            .setPort(12298)
            .setDaemon(false);

        ProviderConfig<ServiceB> providerConfig = new ProviderConfig<ServiceB>()
            .setInterfaceId(ServiceB.class.getName())
            .setApplication(new ApplicationConfig().setAppName("BBB"))
            .setRef(new ServiceBImpl(serviceC))
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();
    }
}
