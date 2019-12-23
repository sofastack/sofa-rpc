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
package com.alipay.sofa.rpc.test.grpc;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public class GrpcServerTest {

    @Test
    //同步调用,直连
    public void testSync() {

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("grpc-server");

        int port = 50052;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_GRPC)
            .setPort(port);

        ProviderConfig<GreeterImpl> providerConfig = new ProviderConfig<GreeterImpl>()
            .setApplication(applicationConfig)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_GRPC)
            .setInterfaceId(GreeterGrpc.class.getName())
            .setRef(new GreeterImpl())
            .setServer(serverConfig);

        providerConfig.export();

        ConsumerConfig<GreeterGrpc.GreeterBlockingStub> consumerConfig = new ConsumerConfig<GreeterGrpc.GreeterBlockingStub>();
        consumerConfig.setInterfaceId(GreeterGrpc.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_GRPC)
            .setDirectUrl("grpc://127.0.0.1:" + port);

        GreeterGrpc.GreeterBlockingStub greeterBlockingStub = consumerConfig.refer();

        HelloRequest.DateTime dateTime = HelloRequest.DateTime.newBuilder().setDate("2018-12-28").setTime("11:13:00")
            .build();
        HelloReply reply = null;
        HelloRequest request = HelloRequest.newBuilder().setName("world").setDateTime(dateTime).build();
        reply = greeterBlockingStub.sayHello(request);

        Assert.assertNotNull(reply);

    }

    @BeforeClass
    public static void adBeforeClass() {
        RpcRunningState.setUnitTestMode(true);
    }

    @After
    public void afterMethod() {
        RpcRuntimeContext.destroy();
        RpcInternalContext.removeAllContext();
        RpcInvokeContext.removeContext();
    }
}