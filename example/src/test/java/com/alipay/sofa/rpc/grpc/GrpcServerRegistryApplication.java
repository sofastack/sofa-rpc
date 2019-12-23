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
package com.alipay.sofa.rpc.grpc;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.grpc.examples.helloworld.*;

/**
 * @author <a href="mailto:leizhiyuan@gmail.com">leizhiyuan</a>
 */
public class GrpcServerRegistryApplication {

    static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerRegistryApplication.class);

    public static void main(String[] args) {

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("grpc-server");

        int port = 50052;
        if (args.length != 0) {
            LOGGER.debug("first arg is {}", args[0]);
            port = Integer.valueOf(args[0]);
        }

        RegistryConfig registryConfig = new RegistryConfig()
            .setProtocol("zookeeper")
            .setAddress("127.0.0.1:2181");

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_GRPC)
            .setPort(port);

        ProviderConfig<GreeterImpl> providerConfig = new ProviderConfig<GreeterImpl>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_GRPC)
            .setInterfaceId(GreeterGrpc.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig)
            .setRegistry(registryConfig);

        providerConfig.export();

        synchronized (GrpcServerRegistryApplication.class) {
            try {
                while (true) {
                    GrpcServerRegistryApplication.class.wait();
                }
            } catch (InterruptedException e) {
                LOGGER.error("Exit by Interrupted");
            }
        }

    }
}
