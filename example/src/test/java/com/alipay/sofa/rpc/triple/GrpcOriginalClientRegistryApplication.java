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

import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import java.time.format.DateTimeFormatter;

/**
 * @author <a href="mailto:luanyanqiang@dibgroup.cn">Luan Yanqiang</a>
 */
public class GrpcOriginalClientRegistryApplication {

    static final DateTimeFormatter[] datetimeFormatter = new DateTimeFormatter[] { DateTimeFormatter.ISO_DATE_TIME,
                                                       DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                                                       DateTimeFormatter.BASIC_ISO_DATE };

    public static void main(String[] args) {
        ManagedChannel channel = NettyChannelBuilder.forAddress("127.0.0.1", 50051).usePlaintext()
            .build();
        GreeterGrpc.GreeterBlockingStub client = GreeterGrpc.newBlockingStub(channel);
        HelloReply res = client.sayHello(HelloRequest.newBuilder().setName("fuck").build());
        System.out.println(res);
    }
}
