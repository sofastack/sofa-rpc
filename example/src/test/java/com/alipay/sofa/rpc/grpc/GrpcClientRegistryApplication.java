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
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

import java.time.format.DateTimeFormatter;

/**
 * @author <a href="mailto:luanyanqiang@dibgroup.cn">Luan Yanqiang</a>
 */
public class GrpcClientRegistryApplication {

    static final DateTimeFormatter[] datetimeFormatter = new DateTimeFormatter[] { DateTimeFormatter.ISO_DATE_TIME,
                                                       DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                                                       DateTimeFormatter.BASIC_ISO_DATE };

    public static void main(String[] args) {
        final Logger LOGGER = LoggerFactory.getLogger(GrpcClientRegistryApplication.class);

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setProtocol("zookeeper").setAddress("127.0.0.1:2181");

        ConsumerConfig<GreeterGrpc.GreeterBlockingStub> consumerConfig = new ConsumerConfig<GreeterGrpc.GreeterBlockingStub>();
        consumerConfig.setInterfaceId(GreeterGrpc.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_GRPC)
            .setRegistry(registryConfig);

        // GreeterGrpc.GreeterBlockingStub s = new         
        GreeterGrpc.GreeterBlockingStub greeterBlockingStub = (GreeterGrpc.GreeterBlockingStub) consumerConfig.refer();

        LOGGER.info("Grpc stub bean successful: {}", greeterBlockingStub.getClass().getName());

        LOGGER.info("Will try to greet " + "world" + " ...");
        HelloRequest.DateTime dateTime = HelloRequest.DateTime.newBuilder().setDate("2018-12-28").setTime("11:13:00")
            .build();
        HelloRequest request = HelloRequest.newBuilder().setName("world").build();
        HelloReply reply = null;
        try {
            for (int i = 0; i < 10000; i++) {
                try {
                    HelloRequest.DateTime reqDateTime = HelloRequest.DateTime.newBuilder(dateTime).setTime("" + i)
                        .build();
                    request = HelloRequest.newBuilder(request).setName("world_" + i).setDateTime(reqDateTime).build();
                    reply = greeterBlockingStub.sayHello(request);
                    LOGGER.info("Greeting: {}, {}", reply.getMessage(), reply.getDateTime().getDate());
                    // Object r = greeterBlockingStub.sayHello(request);
                    // LOGGER.info("Greeting: {}, {}", r.toString(), r.toString());
                } catch (StatusRuntimeException e) {
                    LOGGER.error("RPC failed: {}", e.getStatus());
                } catch (Throwable e) {
                    LOGGER.error("Unexpected RPC call breaks", e);
                }

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected RPC call breaks", e);
        }

        synchronized (GrpcClientRegistryApplication.class) {
            try {
                while (true) {
                    GrpcClientRegistryApplication.class.wait();
                }
            } catch (InterruptedException e) {
                LOGGER.error("Exit by Interrupted");
            }
        }

        consumerConfig.unRefer();

    }
}
