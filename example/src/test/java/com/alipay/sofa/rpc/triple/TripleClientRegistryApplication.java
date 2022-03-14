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
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.SofaGreeterTriple;

/**
 * @author <a href="mailto:luanyanqiang@dibgroup.cn">Luan Yanqiang</a>
 */
public class TripleClientRegistryApplication {
    private final static Logger LOGGER = LoggerFactory.getLogger(TripleClientRegistryApplication.class);

    public static void main(String[] args) {

        ApplicationConfig clientApp = new ApplicationConfig().setAppName("triple-client");

        /*   RegistryConfig registryConfig = new RegistryConfig();
           registryConfig.setProtocol("zookeeper").setAddress("127.0.0.1:2181");*/

        ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig = new ConsumerConfig<SofaGreeterTriple.IGreeter>();
        consumerConfig.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            //.setRegistry(registryConfig)
            .setApplication(clientApp)
            .setDirectUrl("tri://10.15.232.18:19544")
            .setRegister(false);

        SofaGreeterTriple.IGreeter greeterBlockingStub = consumerConfig.refer();

        LOGGER.info("Triple stub bean successful: {}", greeterBlockingStub.getClass().getName());

        LOGGER.info("Will try to greet " + "world" + " ...");
        HelloRequest.DateTime dateTime = HelloRequest.DateTime.newBuilder().setDate("2018-12-28").setTime("11:13:00")
            .build();
        HelloRequest request = HelloRequest.newBuilder().setName("world").build();
        HelloReply reply = null;
        try {
            try {
                HelloRequest.DateTime reqDateTime = HelloRequest.DateTime.newBuilder(dateTime).setTime("")
                    .build();
                request = HelloRequest.newBuilder(request).setName("world").setDateTime(reqDateTime).build();
                reply = greeterBlockingStub.sayHello(request);
                LOGGER.info("Invoke Success,Greeting: {}, {}", reply.getMessage(), reply.getDateTime().getDate());
            } catch (StatusRuntimeException e) {
                LOGGER.error("RPC failed: {}", e.getStatus());
            } catch (Throwable e) {
                LOGGER.error("Unexpected RPC call breaks", e);
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected RPC call breaks", e);
        }

    }
}
