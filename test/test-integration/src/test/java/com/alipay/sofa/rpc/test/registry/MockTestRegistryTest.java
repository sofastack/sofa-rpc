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
package com.alipay.sofa.rpc.test.registry;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class MockTestRegistryTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {

        RegistryConfig registryConfig = new RegistryConfig()
            .setProtocol("mocktest");

        ServerConfig serverConfig2 = new ServerConfig()
            .setPort(22222)
            .setDaemon(false);

        // 服务端
        ProviderConfig<HelloService> CProvider = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(1000))
            .setRegistry(registryConfig)
            .setServer(serverConfig2);
        CProvider.export();

        // 客户端
        ConsumerConfig<HelloService> BConsumer = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setInvokeType(RpcConstants.INVOKER_TYPE_SYNC)
            .setTimeout(5000)
            .setRegistry(registryConfig);
        HelloService helloService = BConsumer.refer();

        // 正常
        boolean error = false;
        try {
            String ret = helloService.sayHello("xxx", 22);
            Assert.assertNotNull(ret);
        } catch (Exception e) {
            e.printStackTrace();
            error = true;
        }
        Assert.assertFalse(error);
    }
}
