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
package com.alipay.sofa.rpc.test.client;

import com.alipay.remoting.rpc.RpcServer;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhiyuan.lzy@antfin.com">zhiyuan.lzy</a>
 */
public class BoltClientReverseCommunicationTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {
        testReverseCommunication();
        RpcRuntimeContext.destroy();
    }

    public void testReverseCommunication() {
        // 只有2个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22223)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(1000))
            .setServer(serverConfig)
            .setRepeatedExportLimit(-1)
            .setRegister(false);
        providerConfig.export();

        RpcServer rpcServerRemoting = new RpcServer(22222, true);
        final BoltServerMockProcesser processor = new BoltServerMockProcesser(rpcServerRemoting);
        rpcServerRemoting.registerUserProcessor(processor);
        rpcServerRemoting.start();
        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(3000)
            .setRegister(false)
            .setServer(serverConfig);
        final HelloService helloService = consumerConfig.refer();

        try {
            helloService.sayHello("xx", 1);
            Thread.sleep(1000);
        } catch (Throwable e) {
        }

        //服务端反响通信，并且收到了客户端正常的响应
        Assert.assertTrue(processor.isReversed());

        try {
            serverConfig.destroy();
        } catch (Exception e) {
        }
    }
}