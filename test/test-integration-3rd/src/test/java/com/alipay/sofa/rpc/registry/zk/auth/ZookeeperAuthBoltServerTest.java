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
package com.alipay.sofa.rpc.registry.zk.auth;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.registry.base.BaseZkTest;
import com.alipay.sofa.rpc.test.EchoService;
import com.alipay.sofa.rpc.test.EchoServiceImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:jjzxjgy@126.com">jianyang</a>
 */
public class ZookeeperAuthBoltServerTest extends BaseZkTest {

    private static ServerConfig        serverConfig;
    private static RegistryConfig      registryConfig;

    private static Map<String, String> parameters = new HashMap<String, String>();

    @BeforeClass
    public static void setUp() {
        parameters.put("scheme", "digest");
        //如果存在多个认证信息，则在参数形式为为user1:passwd1,user2:passwd2
        parameters.put("addAuth", "sofazk:rpc1");

        registryConfig = new RegistryConfig()
            .setProtocol("zookeeper")
            .setAddress("127.0.0.1:2181/authtest")
            .setParameters(parameters);

        serverConfig = new ServerConfig()
            .setProtocol("bolt") // 设置一个协议，默认bolt
            .setPort(12200) // 设置一个端口，默认12200
            .setDaemon(false); // 非守护线程
    }

    @AfterClass
    public static void destroy() {
        serverConfig.destroy();
    }

    @Test
    public void testAll() {

        ProviderConfig<EchoService> providerConfig = new ProviderConfig<EchoService>()
            .setRegistry(registryConfig)
            .setInterfaceId(EchoService.class.getName()) // 指定接口
            .setRef(new EchoServiceImpl()) // 指定实现
            .setServer(serverConfig); // 指定服务端
        providerConfig.export(); // 发布服务

        ConsumerConfig<EchoService> consumerConfig = new ConsumerConfig<EchoService>()
            .setRegistry(registryConfig)
            .setInterfaceId(EchoService.class.getName()) // 指定接口
            .setProtocol("bolt") // 指定协议
            .setTimeout(3000)
            .setConnectTimeout(10 * 1000);
        EchoService echoService = consumerConfig.refer();

        String result = echoService.echoStr("auth test");

        Assert.assertEquals("auth test", result);

    }

}
