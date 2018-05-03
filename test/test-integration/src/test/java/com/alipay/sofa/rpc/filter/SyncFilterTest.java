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
package com.alipay.sofa.rpc.filter;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class SyncFilterTest extends ActivelyDestroyTest {

    @Test
    public void test() {
        ServerConfig serverConfig2 = new ServerConfig()
            .setPort(22222)
            .setDaemon(false);

        // ProviderConfig
        TestSyncFilter filter1 = new TestSyncFilter();
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setRef(new HelloServiceImpl(1000))
            .setFilterRef(Arrays.asList((Filter) filter1))
            .setApplication(new ApplicationConfig().setAppName("sss"))
            .setServer(serverConfig2);

        providerConfig.export();

        // ConsumerConfig
        TestSyncFilter filter0 = new TestSyncFilter();
        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setInvokeType(RpcConstants.INVOKER_TYPE_SYNC)
            .setApplication(new ApplicationConfig().setAppName("ccc"))
            .setTimeout(5000)
            .setFilterRef(Arrays.asList((Filter) filter0))
            .setDirectUrl("bolt://127.0.0.1:22222?appName=sss");
        HelloService helloService = consumerConfig.refer();

        try {
            helloService.sayHello("xxx", 12);
            // Consumer side
            Assert.assertEquals("xxx", filter0.args[0]);
            Assert.assertEquals(12, filter0.args[1]);
            Assert.assertEquals(HelloService.class.getName(), filter0.interfaceName);
            Assert.assertEquals("com.alipay.sofa.rpc.test.HelloService:1.0", filter0.targetServiceUniqueName);
            Assert.assertEquals("sayHello", filter0.methodName);
            Assert.assertEquals(RpcConstants.INVOKER_TYPE_SYNC, filter0.invokeType);
            Assert.assertNull(filter0.targetAppName);

            // Provider side
            Assert.assertEquals("xxx", filter1.args[0]);
            Assert.assertEquals(12, filter1.args[1]);
            Assert.assertEquals(HelloService.class.getName(), filter1.interfaceName);
            Assert.assertEquals("com.alipay.sofa.rpc.test.HelloService:1.0", filter1.targetServiceUniqueName);
            Assert.assertEquals("sayHello", filter1.methodName);
            Assert.assertEquals("sss", filter1.targetAppName);
        } catch (Exception e) {
        }
    }
}
