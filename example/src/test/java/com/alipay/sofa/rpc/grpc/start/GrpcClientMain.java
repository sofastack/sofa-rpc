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
package com.alipay.sofa.rpc.grpc.start;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

import java.net.URL;

/**
 * <p></p>
 * <p>
 *
 *
 * @author <a href=mailto:luanyanqiang@dibgroup.cn>Luan Yanqiang</a>
 */
public class GrpcClientMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(GrpcClientMain.class);

    public static void main(String[] args) {
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("grpc-client");

        ConsumerConfig<GreeterGrpc.GreeterBlockingStub> consumerConfig = new ConsumerConfig<GreeterGrpc.GreeterBlockingStub>()
            .setApplication(applicationConfig)
            .setInterfaceId(GreeterGrpc.class.getName())
            .setBootstrap("grpc")
            .setProtocol("grpc")
            .setUniqueId("xxx")
            .setDirectUrl("http://127.0.0.1:50052")
            .setRegister(false);
        // .setTimeout(3000);

        GreeterGrpc.GreeterBlockingStub greeterBlockingStub = consumerConfig.refer();

        LOGGER.error("started at pid {}", RpcRuntimeContext.PID);

        LOGGER.info("Will try to greet " + "world" + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName("world").build();
        HelloReply reply = null;
        try {
            for (int i = 0; i < 100; i++) {
                try {
                    reply = greeterBlockingStub.sayHello(request);
                } catch (StatusRuntimeException e) {
                    LOGGER.error("RPC failed: {0}", e.getStatus());
                }
                LOGGER.info("Greeting: {}", reply.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        synchronized (GrpcClientMain.class) {
            try {
                while (true) {
                    GrpcClientMain.class.wait();
                }
            } catch (InterruptedException e) {
                LOGGER.error("Exit by Interrupted");
            }
        }
    }

}
