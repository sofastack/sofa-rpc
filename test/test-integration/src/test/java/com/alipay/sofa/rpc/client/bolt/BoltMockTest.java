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
package com.alipay.sofa.rpc.client.bolt;

import com.alipay.sofa.rpc.common.MockMode;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.json.JSON;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhiyuan.lzy@antfin.com">zhiyuan.lzy</a>
 */
public class BoltMockTest extends ActivelyDestroyTest {

    @Test
    public void testLocal() {

        final ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setBootstrap("bolt")
            .setApplication(new ApplicationConfig().setAppName("clientApp"))
            .setReconnectPeriod(1000)
            .setMockMode("local")
            .setMockRef(new HelloService() {
                @Override
                public String sayHello(String name, int age) {
                    return "mock";
                }
            });

        HelloService helloService = consumerConfig.refer();
        Assert.assertEquals("mock", helloService.sayHello("xx", 22));

    }

    @Test
    public void testRemote() {

        HttpMockServer.initSever(1235);
        HttpMockServer.addMockPath("/", JSON.toJSONString("mockJson"));
        HttpMockServer.start();
        final ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setBootstrap("bolt")
            .setApplication(new ApplicationConfig().setAppName("clientApp"))
            .setReconnectPeriod(1000)
            .setMockMode(MockMode.REMOTE)
            .setParameter("mockUrl", "http://127.0.0.1:1235/");

        HelloService helloService = consumerConfig.refer();
        Assert.assertEquals("mockJson", helloService.sayHello("xx", 22));

        HttpMockServer.stop();
    }
}
