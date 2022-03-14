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

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.SampleService;
import com.alipay.sofa.rpc.test.SampleServiceImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * @Author: BaoYi
 * @Date: 2021/8/1 5:19 下午
 */
public class AbstractClusterTest extends ActivelyDestroyTest {

    @Test
    public void testResolveTimeoutByProvider() {

        RegistryConfig registryConfig = new RegistryConfig()
            .setProtocol("mocktest");

        ServerConfig serverConfig = new ServerConfig()
            .setPort(12122) // 设置一个端口，默认12200
            .setDaemon(false); // 非守护线程

        ProviderConfig<SampleService> providerConfig = new ProviderConfig<SampleService>()
            .setInterfaceId(SampleService.class.getName()) // 指定接口
            .setRef(new SampleServiceImpl()) // 指定实现
            .setServer(serverConfig) // 指定服务端
            .setRegistry(registryConfig)
            .setTimeout(5000);

        providerConfig.export(); // 发布服务*/

        ConsumerConfig<SampleService> consumerConfig = new ConsumerConfig<SampleService>()
            .setInterfaceId(SampleService.class.getName()) // 指定接口
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT) // 指定协议
            .setRegistry(registryConfig);

        // 生成代理类
        SampleService sampleService = consumerConfig.refer();

        Assert.assertEquals("sleep 4000 ms", sampleService.testTimeout(4000));

    }
}
