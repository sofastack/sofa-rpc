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
package com.alipay.sofa.rpc.transport.http;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;
import com.alipay.sofa.rpc.server.bolt.pb.Group;
import com.alipay.sofa.rpc.server.http.HttpService;
import com.alipay.sofa.rpc.server.http.HttpServiceImpl;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class Http2ClearTextPriorKnowledgeTest extends ActivelyDestroyTest {

    @Test
    public void testProtobuf() {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(12300)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_H2C)
            .setDaemon(true);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HttpService> providerConfig = new ProviderConfig<HttpService>()
            .setInterfaceId(HttpService.class.getName())
            .setRef(new HttpServiceImpl())
            .setApplication(new ApplicationConfig().setAppName("serverApp"))
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig.export();

        {
            ConsumerConfig<HttpService> consumerConfig = new ConsumerConfig<HttpService>()
                .setInterfaceId(HttpService.class.getName())
                .setSerialization(RpcConstants.SERIALIZE_PROTOBUF)
                .setDirectUrl("h2c://127.0.0.1:12300")
                .setApplication(new ApplicationConfig().setAppName("clientApp"))
                .setProtocol(RpcConstants.PROTOCOL_TYPE_H2C);

            HttpService httpService = consumerConfig.refer();

            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
            EchoResponse response = httpService.echoPb(request);
            Assert.assertEquals("helloxxx", response.getMessage());
            Assert.assertEquals(200, response.getCode());
        }

        {
            ConsumerConfig<HttpService> consumerConfig2 = new ConsumerConfig<HttpService>()
                .setInterfaceId(HttpService.class.getName())
                .setSerialization(RpcConstants.SERIALIZE_PROTOBUF)
                .setDirectUrl("h2c://127.0.0.1:12300")
                .setApplication(new ApplicationConfig().setAppName("clientApp"))
                .setProtocol(RpcConstants.PROTOCOL_TYPE_H2C)
                .setInvokeType(RpcConstants.INVOKER_TYPE_ONEWAY)
                .setRepeatedReferLimit(-1);
            HttpService httpService2 = consumerConfig2.refer();
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
            try {
                httpService2.echoPb(request);
                // NOT SUPPORTED NOW, If want support this, need add key to head.
                Assert.fail();
            } catch (Exception e) {

            }
        }

        {
            ConsumerConfig<HttpService> consumerConfig3 = new ConsumerConfig<HttpService>()
                .setInterfaceId(HttpService.class.getName())
                .setSerialization(RpcConstants.SERIALIZE_PROTOBUF)
                .setDirectUrl("h2c://127.0.0.1:12300")
                .setApplication(new ApplicationConfig().setAppName("clientApp"))
                .setProtocol(RpcConstants.PROTOCOL_TYPE_H2C)
                .setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE)
                .setRepeatedReferLimit(-1);
            HttpService httpService3 = consumerConfig3.refer();
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
            EchoResponse response = httpService3.echoPb(request);
            Assert.assertNull(response);

            ResponseFuture future = RpcInvokeContext.getContext().getFuture();
            try {
                response = (EchoResponse) future.get();
                Assert.assertEquals("helloxxx", response.getMessage());
                Assert.assertEquals(200, response.getCode());
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail();
            }
        }

        {
            final EchoResponse[] result = new EchoResponse[1];
            final CountDownLatch latch = new CountDownLatch(1);
            ConsumerConfig<HttpService> consumerConfig4 = new ConsumerConfig<HttpService>()
                .setInterfaceId(HttpService.class.getName())
                .setSerialization(RpcConstants.SERIALIZE_PROTOBUF)
                .setDirectUrl("h2c://127.0.0.1:12300")
                .setApplication(new ApplicationConfig().setAppName("clientApp"))
                .setProtocol(RpcConstants.PROTOCOL_TYPE_H2C)
                .setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK)
                .setOnReturn(new SofaResponseCallback() {
                    @Override
                    public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                        result[0] = (EchoResponse) appResponse;
                        latch.countDown();
                    }

                    @Override
                    public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                        latch.countDown();
                    }

                    @Override
                    public void onSofaException(SofaRpcException sofaException, String methodName, RequestBase request) {
                        latch.countDown();
                    }
                })
                .setRepeatedReferLimit(-1);
            HttpService httpService4 = consumerConfig4.refer();
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.A).setName("xxx").build();
            EchoResponse response = httpService4.echoPb(request);
            Assert.assertNull(response);

            try {
                latch.await(2000, TimeUnit.MILLISECONDS);
                response = result[0];
                Assert.assertNotNull(response);
                Assert.assertEquals("helloxxx", response.getMessage());
                Assert.assertEquals(200, response.getCode());
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail();
            }
        }
    }
}
