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
package com.alipay.sofa.rpc.registry.zk;

import com.alipay.sofa.rpc.base.BaseZkTest;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.model.grpc.GrpcTestServiceGrpc.GrpcTestServiceImplBase;
import com.alipay.sofa.rpc.model.grpc.GrpcTestService_Request_String;
import com.alipay.sofa.rpc.model.grpc.GrpcTestService_Response_String;
import com.alipay.sofa.rpc.model.grpc.impl.GrpcTestServiceImpl;
import com.alipay.sofa.rpc.registry.RegistryFactory;
import io.grpc.stub.StreamObserver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.30 2:31 PM
 */
public class ZookeeperGrpcTest extends BaseZkTest {

    private static RegistryConfig    registryConfig;

    private static ZookeeperRegistry registry;

    @BeforeClass
    public static void setUp() {
        registryConfig = new RegistryConfig()
            .setProtocol("zookeeper")
            .setSubscribe(true)
            .setAddress("127.0.0.1:2181")
            .setRegister(true);

        registry = (ZookeeperRegistry) RegistryFactory.getRegistry(registryConfig);
        registry.init();
        Assert.assertTrue(registry.start());
    }

    @AfterClass
    public static void tearDown() {
        registry.destroy();
        registry = null;
    }

    @Test
    public void test() throws InterruptedException {
        SystemInfo.getLocalHost();

        ServerConfig serverConfig = new ServerConfig()
            .setPort(9091)
            .setProtocol("grpc");
        ProviderConfig<GrpcTestServiceImplBase> providerConfig = new ProviderConfig<GrpcTestServiceImplBase>()
            .setInterfaceId(GrpcTestServiceImplBase.class.getName())
            .setRef(new GrpcTestServiceImpl())
            .setServer(serverConfig)
            .setBootstrap("grpc")
            .setRegistry(registryConfig);
        providerConfig.export();

        ConsumerConfig<GrpcTestServiceImplBase> consumerConfig = new ConsumerConfig<GrpcTestServiceImplBase>()
            .setInterfaceId(GrpcTestServiceImplBase.class.getName())
            .setRegistry(registryConfig)
            .setProtocol("grpc")
            .setBootstrap("grpc")
            .setLazy(true);
        GrpcTestServiceImplBase grpcTestService = consumerConfig.refer();

        Thread.sleep(1000);

        final String[] result = { "", "", "" };
        final CountDownLatch countDownLatch = new CountDownLatch(3);
        grpcTestService.reqString(GrpcTestService_Request_String.newBuilder().setName("YYY").build(),
            new StreamObserver<GrpcTestService_Response_String>() {
                @Override
                public void onNext(GrpcTestService_Response_String value) {
                    countDownLatch.countDown();
                    result[0] = value.getResult();
                }

                @Override
                public void onError(Throwable t) {
                    countDownLatch.countDown();
                    result[1] = t.getMessage();
                }

                @Override
                public void onCompleted() {
                    countDownLatch.countDown();
                    result[2] = "onCompleted";
                }
            });

        countDownLatch.await(1, TimeUnit.SECONDS);
        Assert.assertEquals("success:YYY", result[0]);
        Assert.assertEquals("", result[1]);
        Assert.assertEquals("onCompleted", result[2]);
    }
}