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
package com.alipay.sofa.rpc.test.bootstrap.bolt;

import com.alipay.sofa.rpc.client.Cluster;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.proxy.jdk.JDKProxy;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltProviderBootstrapTest extends ActivelyDestroyTest {
    @Test
    public void export() throws Exception {

        // 发布一个服务，每个请求要执行2秒
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22223)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig0 = new ProviderConfig<HelloService>()
            .setId("p-0")
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(2000))
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig0.export();

        ProviderConfig<HelloService> providerConfig1 = new ProviderConfig<HelloService>()
            .setId("p-1")
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(2000))
            .setServer(serverConfig)
            .setRegister(false);
        try {
            providerConfig1.export();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }

        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>()
            .setId("p-2")
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(2000))
            .setServer(serverConfig)
            .setRepeatedExportLimit(2)
            .setRegister(false);
        providerConfig2.export();

        ProviderConfig<HelloService> providerConfig3 = new ProviderConfig<HelloService>()
            .setId("p-3")
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(2000))
            .setServer(serverConfig)
            .setRepeatedExportLimit(2)
            .setRegister(false);
        try {
            providerConfig3.export();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcRuntimeException);
        }
    }

    @Test
    public void testAttrUpdate() {
        // 发布一个服务
        ServerConfig serverConfig0 = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22224)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);
        ProviderConfig<HelloService> providerConfig0 = new ProviderConfig<HelloService>()
            .setId("p-0")
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("attr")
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig0)
            .setRepeatedExportLimit(5);
        providerConfig0.export();

        ConsumerConfig<HelloService> consumerConfig0 = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setUniqueId("attr")
            .setProxy("jdk")
            .setDirectUrl("bolt://127.0.0.1:22224")
            .setTimeout(500);
        HelloService proxy = consumerConfig0.refer();
        Invoker invoker = JDKProxy.parseInvoker(proxy);
        Cluster cluster = consumerConfig0.getConsumerBootstrap().getCluster();
        boolean error = false;
        try {
            proxy.sayHello("11", 11);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);

        // wrong key
        error = false;
        try {
            providerConfig0.getConfigListener().attrUpdated(Collections.singletonMap("weighttttt", "asdasd"));
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);
        // wrong value
        error = false;
        try {
            providerConfig0.getConfigListener().attrUpdated(Collections.singletonMap("weight", "asdasd"));
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);

        // 切到一个没有的分组
        providerConfig0.getConfigListener().attrUpdated(Collections.singletonMap("uniqueId", "attr2"));

        // 切到一个有的分组
        error = false;
        try {
            proxy.sayHello("11", 11);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertTrue(error);

        // 切到一个有的分组
        providerConfig0.getConfigListener().attrUpdated(Collections.singletonMap("uniqueId", "attr"));
        error = false;
        try {
            proxy.sayHello("11", 11);
        } catch (Exception e) {
            error = true;
        }
        Assert.assertFalse(error);
    }
}