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
package com.alipay.sofa.rpc.transmit.ip;

import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import com.alipay.sofa.rpc.transmit.TransmitConfig;
import com.alipay.sofa.rpc.transmit.TransmitConfigHelper;
import com.alipay.sofa.rpc.transmit.TransmitLauncherFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一直转发到指定地址
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class IpTransmitProxyTest extends ActivelyDestroyTest {

    @Test
    public void test() throws Exception {

        String appName = "testApp";
        ApplicationConfig applicationConfig = new ApplicationConfig()
            .setAppName(appName);

        // 服务端 B0
        ServerConfig serverConfig = new ServerConfig()
            .setHost(SystemInfo.getLocalHost())
            .setPort(12200)
            .setDaemon(true)
            .setProtocol("bolt");

        ProviderConfig<HelloService> providerConfig = new ProviderConfig<HelloService>();
        providerConfig.setInterfaceId(HelloService.class.getName());
        providerConfig.setRef(new HelloServiceImpl("from bbbb1111"));
        providerConfig.setUniqueId("yyy");
        providerConfig.setFilter(Arrays.asList("-ipTransmit")); // just for test
        providerConfig.setServer(serverConfig);
        providerConfig.setApplication(applicationConfig);
        providerConfig.setRepeatedExportLimit(-1);
        providerConfig.setRegister(false);
        providerConfig.export();

        // 注册可以被转发
        IpTransmitLauncher ipTransmitLauncher = (IpTransmitLauncher) TransmitLauncherFactory.getTransmitLauncher("ip");
        ipTransmitLauncher.setRegistry(new MockIpTransmitRegistry());
        ipTransmitLauncher.getRegistry().register(appName, ipTransmitLauncher.generateDataId(appName, "xxx"));

        // 全部转出去  B1 转到 B0
        String rule = "weightStarted:1,address:127.0.0.1";
        TransmitConfig config = TransmitConfigHelper.parseTransmitConfig(appName, rule);
        Assert.assertTrue(config.getWeightStarting() == 1.0d);
        Assert.assertTrue(config.getDuring() == 0);
        Assert.assertTrue(config.getWeightStarted() == 1.0d);
        Assert.assertEquals(config.getAddress(), "127.0.0.1");
        config.setUniqueIdValue("xxx");
        config.setTransmitTimeout(3000);
        ipTransmitLauncher.load(appName, config);

        // 服务端 
        ServerConfig serverConfig2 = new ServerConfig()
            .setHost(SystemInfo.getLocalHost())
            .setPort(12201)
            .setProtocol("bolt");
        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>();
        providerConfig2.setInterfaceId(HelloService.class.getName());
        providerConfig2.setRef(new HelloServiceImpl("from bbbb2222"));
        providerConfig2.setUniqueId("yyy");
        providerConfig2.setServer(serverConfig2);
        providerConfig2.setRegister(false);
        providerConfig2.setApplication(applicationConfig);
        providerConfig2.setRepeatedExportLimit(-1);
        providerConfig2.export();

        String localIp = serverConfig2.getBoundHost();
        serverConfig2.setPort(12200); // just for test

        //客户端
        ApplicationConfig clientApp = new ApplicationConfig()
            .setAppName("testClient");
        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>();
        consumerConfig.setUniqueId("yyy");
        consumerConfig.setProtocol("bolt");
        consumerConfig.setInterfaceId(HelloService.class.getName());
        consumerConfig.setApplication(clientApp);
        consumerConfig.setDirectUrl("bolt://" + localIp + ":12201?appName=testApp"); // 调用B1
        consumerConfig.setTimeout(60000);
        final HelloService helloService = consumerConfig.refer();

        // 开始预热转发
        String old = SystemInfo.getLocalHost();
        try {
            SystemInfo.setLocalHost("111.111.111.111");
            ipTransmitLauncher.startTransmit(appName);
        } finally {
            SystemInfo.setLocalHost(old);
        }
        final AtomicInteger cnt = new AtomicInteger(0);
        final int time = 1;
        final CountDownLatch latch = new CountDownLatch(time * time);
        for (int i = 0; i < time; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < time; j++) {
                        try {
                            if ("from bbbb1111".equals(helloService.sayHello("1111", 23))) {
                                cnt.incrementAndGet();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        latch.countDown();
                    }
                }
            }).start();
        }

        latch.await(2000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(cnt.get() == time * time);

        //主动关闭转发，就不会转发
        ipTransmitLauncher.stopTransmit(appName);
        final CountDownLatch latch2 = new CountDownLatch(time * time);
        for (int i = 0; i < time; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < time; j++) {
                        if ("from bbbb2222".equals(helloService.sayHello("2222", 23))) {
                            cnt.incrementAndGet();
                        }
                        latch2.countDown();
                    }
                }
            }).start();
        }

        latch2.await(2000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(cnt.get() == time * time * 2);

        ipTransmitLauncher.unload(appName);
    }
}