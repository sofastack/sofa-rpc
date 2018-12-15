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

import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc.GrpcTestServiceImplBase;
import com.alipay.sofa.rpc.model.grpc.impl.GrpcTestServiceImpl;
import com.alipay.sofa.rpc.server.ServerFactory;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.22 8:00 PM
 */
public class GrpcInvokeTest extends ActivelyDestroyTest {

    private GrpcTestServiceImplBase                 grpcTestService;

    private ServerConfig                            serverConfig;

    private ProviderConfig<GrpcTestServiceImplBase> providerConfig;

    private ConsumerConfig<GrpcTestServiceImplBase> consumerConfig;

    @Before
    public void before() {
        SystemInfo.getLocalHost();

        serverConfig = new ServerConfig()
            .setPort(9091)
            .setProtocol("grpc");

        providerConfig = new ProviderConfig<GrpcTestServiceImplBase>()
            .setInterfaceId(GrpcTestServiceImplBase.class.getName())
            .setRef(new GrpcTestServiceImpl())
            .setServer(serverConfig)
            .setBootstrap("grpc")
            .setRegister(false);
        providerConfig.export();

        consumerConfig = new ConsumerConfig<GrpcTestServiceImplBase>()
            .setInterfaceId(GrpcTestServiceImplBase.class.getName())
            .setDirectUrl("grpc://127.0.0.1:9091")
            .setProtocol("grpc")
            .setBootstrap("grpc")
            .setLazy(true)
            .setRegister(false);
        grpcTestService = consumerConfig.refer();
    }

    @After
    public void after() {
        ServerFactory.destroyAll();
    }

    @Test
    public void testUNARY() throws InterruptedException {

        //invoke1
        String[] result = GrpcTestUtil.invokeUNARY(grpcTestService);
        Assert.assertEquals("success:AAA", result[0]);
        Assert.assertEquals("", result[1]);
        Assert.assertEquals("onCompleted", result[2]);

        //unExport
        providerConfig.unExport();

        //invoke2
        String[] result2 = GrpcTestUtil.invokeUNARY(grpcTestService);
        Assert.assertEquals("", result2[0]);
        Assert.assertEquals("UNAVAILABLE: HTTP/2 error code: NO_ERROR\nReceived Goaway", result2[1]);
        Assert.assertEquals("", result2[2]);

        //unRefer
        consumerConfig.unRefer();
        try {
            GrpcTestUtil.invokeUNARY(grpcTestService);
            fail("Expected an Exception to be thrown");
        } catch (Exception e) {
            Assert.assertEquals("Client has been destroyed!", e.getMessage());
        }
    }

    @Test
    public void testServerStream() throws InterruptedException {

        //invoke1
        String[] result = GrpcTestUtil.invokeServerStream(grpcTestService);
        Assert.assertEquals("success_1:AAA;success_2:AAA;success_3:AAA;", result[0]);
        Assert.assertEquals("", result[1]);
        Assert.assertEquals("onCompleted", result[2]);

        //unExport
        providerConfig.unExport();

        //invoke2
        String[] result2 = GrpcTestUtil.invokeServerStream(grpcTestService);
        Assert.assertEquals("", result2[0]);
        Assert.assertEquals("UNAVAILABLE: HTTP/2 error code: NO_ERROR\nReceived Goaway", result2[1]);
        Assert.assertEquals("", result2[2]);

        //unRefer
        consumerConfig.unRefer();
        try {
            GrpcTestUtil.invokeServerStream(grpcTestService);
            fail("Expected an Exception to be thrown");
        } catch (Exception e) {
            Assert.assertEquals("Client has been destroyed!", e.getMessage());
        }
    }
}