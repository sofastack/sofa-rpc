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
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;

/**
 *
 *
 * @author <a href=mailto:luanyanqiang@dibgroup.cn>Luan Yanqiang</a>
 */
public class GrpcServerMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(GrpcServerMain.class);

    public static void main(String[] args) {
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("grpc-server");

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("grpc")
            .setHost("127.0.0.1")
            .setPort(50052);

        ProviderConfig<GreeterGrpc.GreeterImplBase> providerConfig = new ProviderConfig<GreeterGrpc.GreeterImplBase>()
            .setInterfaceId(GreeterGrpc.GreeterImplBase.class.getName())
            .setBootstrap("grpc")
            .setApplication(applicationConfig)
            .setRef(new GreeterImpl())
            .setUniqueId("xxx")
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();

        LOGGER.info("Grpc service started at pid {}", RpcRuntimeContext.PID);

        synchronized (GrpcServerMain.class) {
            while (true) {
                try {
                    GrpcServerMain.class.wait();
                } catch (InterruptedException e) {

                }
            }
        }
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

}
