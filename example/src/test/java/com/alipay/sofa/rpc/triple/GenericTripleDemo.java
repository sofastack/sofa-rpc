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
package com.alipay.sofa.rpc.triple;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.grpc.StatusRuntimeException;

/**
 * @author zhaowang
 * @version : GenericTripleDemo.java, v 0.1 2020年05月28日 3:15 下午 zhaowang Exp $
 */
public class GenericTripleDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericTripleDemo.class);

    public static void main(String[] args) {
        RpcRunningState.setDebugMode(true);

        ApplicationConfig clientApp = new ApplicationConfig().setAppName("triple-client");

        ApplicationConfig serverApp = new ApplicationConfig().setAppName("triple-server");

        int port = 50052;
        if (args.length != 0) {
            LOGGER.debug("first arg is {}", args[0]);
            port = Integer.valueOf(args[0]);
        }

        RegistryConfig registryConfig = new RegistryConfig()
            .setProtocol("zookeeper")
            .setAddress("127.0.0.1:2181");

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        ProviderConfig<OriginHello> providerConfig = new ProviderConfig<OriginHello>()
            .setApplication(serverApp)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(OriginHello.class.getName())
            .setRef(new OriginHelloImpl())
            .setServer(serverConfig)
            .setRegistry(registryConfig);

        providerConfig.export();

        ConsumerConfig<OriginHello> consumerConfig = new ConsumerConfig<OriginHello>();
        consumerConfig.setInterfaceId(OriginHello.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setRegistry(registryConfig)
            .setApplication(clientApp);

        OriginHello helloService = consumerConfig.refer();

        LOGGER.info("Grpc stub bean successful: {}", helloService.getClass().getName());

        LOGGER.info("Will try to greet " + "world" + " ...");
        while (true) {
            try {
                try {
                    HelloRequest1 helloRequest1 = new HelloRequest1();
                    helloRequest1.setName("ab");
                    HelloRequest2 helloRequest2 = new HelloRequest2();
                    helloRequest2.setName("cd");
                    HelloResponse result = helloService.hello2(helloRequest1, helloRequest2);
                    LOGGER.info("Invoke Success,hello: {} ", result.getMessage());
                } catch (StatusRuntimeException e) {
                    LOGGER.error("RPC failed: {}", e.getStatus());
                } catch (Throwable e) {
                    LOGGER.error("Unexpected RPC call breaks", e);
                }
            } catch (Exception e) {
                LOGGER.error("Unexpected RPC call breaks", e);
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
}