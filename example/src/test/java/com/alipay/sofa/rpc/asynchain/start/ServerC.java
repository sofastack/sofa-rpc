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

import com.alipay.sofa.rpc.asynchain.ServiceC;
import com.alipay.sofa.rpc.asynchain.ServiceCImpl;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ServerC {

    public static void main(String[] args) {
        ServerConfig serverConfig = new ServerConfig()
            .setPort(12299)
            .setDaemon(false);

        ProviderConfig<ServiceC> providerConfig = new ProviderConfig<ServiceC>()
            .setInterfaceId(ServiceC.class.getName())
            .setApplication(new ApplicationConfig().setAppName("CCC"))
            .setRef(new ServiceCImpl(1000))
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();
    }
}
