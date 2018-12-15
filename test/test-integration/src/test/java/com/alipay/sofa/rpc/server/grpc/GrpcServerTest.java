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
package com.alipay.sofa.rpc.server.grpc;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc;
import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc.GrpcTestServiceImplBase;
import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc.GrpcTestServiceStub;
import com.alipay.sofa.rpc.model.grpc.impl.GrpcTestServiceImpl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.30 9:59 AM
 */
public class GrpcServerTest {

    private final static int port = 9096;

    @Test
    public void test() throws InterruptedException {
        //init server
        ServerConfig serverConfig = new ServerConfig()
            .setPort(port);
        GrpcServer server = new GrpcServer();
        server.init(serverConfig);

        //register processor
        ProviderConfig<GrpcTestServiceImplBase> providerConfig = new ProviderConfig<GrpcTestServiceImplBase>()
            .setInterfaceId(GrpcTestServiceImplBase.class.getName())
            .setRef(new GrpcTestServiceImpl());
        server.registerProcessor(providerConfig, null);

        //start server
        server.start();

        //invoke
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("127.0.0.1", port).usePlaintext().build();
        GrpcTestServiceStub stub = GrpcTestServiceGrpc.newStub(managedChannel);
        String[] result = GrpcTestUtil.invokeUNARY(stub);
        Assert.assertEquals("success:AAA", result[0]);
        Assert.assertEquals("", result[1]);
        Assert.assertEquals("onCompleted", result[2]);

        //unRegister processor
        server.unRegisterProcessor(providerConfig, false);

        //invoke2
        String[] result2 = GrpcTestUtil.invokeUNARY(stub);
        Assert.assertEquals("", result2[0]);
        Assert.assertEquals("UNIMPLEMENTED: Method not found: GrpcTestService/reqString", result2[1]);
        Assert.assertEquals("", result2[2]);

        //register
        server.registerProcessor(providerConfig, null);

        //invoke3
        String[] result3 = GrpcTestUtil.invokeUNARY(stub);
        Assert.assertEquals("success:AAA", result3[0]);
        Assert.assertEquals("", result3[1]);
        Assert.assertEquals("onCompleted", result3[2]);

        //stop server
        server.stop();

        //invoke4
        String[] result4 = GrpcTestUtil.invokeUNARY(stub);
        Assert.assertEquals("", result4[0]);
        Assert.assertEquals("UNAVAILABLE: HTTP/2 error code: NO_ERROR\nReceived Goaway", result4[1]);
        Assert.assertEquals("", result4[2]);
    }
}