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
 * 预热转发到指定地址
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class IpTransmitWarmingUpProxyTest extends ActivelyDestroyTest {

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
        providerConfig.setFilter(Arrays.asList("-ipTransmit"));
        providerConfig.setServer(serverConfig);
        providerConfig.setRegister(false);
        providerConfig.setApplication(applicationConfig);
        providerConfig.setRepeatedExportLimit(-1);
        providerConfig.export();

        // 注册可以被转发
        IpTransmitLauncher ipTransmitLauncher = (IpTransmitLauncher) TransmitLauncherFactory.getTransmitLauncher("ip");
        ipTransmitLauncher.setRegistry(new MockIpTransmitRegistry());
        ipTransmitLauncher.getRegistry().register(appName, ipTransmitLauncher.generateDataId(appName, "xxx"));
        /*
        weightStarting: 预热期内的转发权重或概率，RPC 框架内部会在集群中随机找一台机器以此权重转出。 【1是一直转发 0是不转发】
        during: 预热期的时间长度，单位为秒
        weightStarted: 预热期过后的转发权重，将会一直生效   【1是一直转发 0是不转发】
        address: 预热期过后的转发地址，将会一直生效
        uniqueId: 同 appName 多集群部署的情况下，要区别不同集群可以通过配置此项区分。指定一个自定义的系统变量，保证集群唯一即可。core_unique 是一个 sofa-config.properties 的配置，可以动态替换; 使用方式是在sofa-config.properties中类似定义 core_unique=xxx
        【uniqueId 不是 service或者reference 的 uniqueId】
        
        core_proxy_url=weightStarting:1,during:2,weightStarted:0,address:127.0.0.1,uniqueId:core_unique
        core_unique = xxx
        */

        // 3秒内全部转出去  B1 转到 B0
        String rule = "weightStarting:1,during:2,weightStarted:0,uniqueId:core_unique";
        TransmitConfig config = TransmitConfigHelper.parseTransmitConfig(appName, rule);
        Assert.assertTrue(config.getWeightStarting() == 1.0d);
        Assert.assertTrue(config.getDuring() == 2000);
        Assert.assertTrue(config.getWeightStarted() == 0.0d);
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

        // 开始预热转发（2s 内）
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
                        if ("from bbbb1111".equals(helloService.sayHello("1111", 23))) {
                            cnt.incrementAndGet();
                        }
                        latch.countDown();
                    }
                }
            }).start();
        }

        Thread.sleep(3500);
        Assert.assertTrue(cnt.get() == time * time);

        //时间过了调用不会转发
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