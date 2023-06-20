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
package com.alipay.sofa.rpc.bootstrap.dubbo;

import com.alibaba.dubbo.common.Constants;
import org.apache.dubbo.common.constants.CommonConstants;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alipay.sofa.rpc.bootstrap.dubbo.demo.DemoService;
import com.alipay.sofa.rpc.bootstrap.dubbo.demo.DemoServiceImpl;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.MethodConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import org.apache.dubbo.config.ConfigKeys;
import org.apache.dubbo.config.context.ConfigMode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author <a href=mailto:leizhiyuan@gmail.com>leizhiyuan</a>
 */
public class DubooServerTest {

    ProviderConfig<DemoService> providerConfig;

    ConsumerConfig<DemoService> consumerConfig;

    private static String       OLD_VALUE_SHUTDOWN_WAIT_KEY;
    private static String       OLD_VALUE_DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE;
    private static String       OLD_VALUE_DUBBO_CONFIG_MODE;

    //dubbo close wait time
    @BeforeClass
    public static void before() {
        RpcRunningState.setUnitTestMode(true);
        OLD_VALUE_SHUTDOWN_WAIT_KEY = System.getProperty(CommonConstants.SHUTDOWN_WAIT_KEY);
        OLD_VALUE_DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE = System
            .getProperty(ConfigKeys.DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE);
        OLD_VALUE_DUBBO_CONFIG_MODE = System.getProperty(ConfigKeys.DUBBO_CONFIG_MODE);

        System.setProperty(CommonConstants.SHUTDOWN_WAIT_KEY, "1");
        System.setProperty(ConfigKeys.DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE, "true");
        System.setProperty(ConfigKeys.DUBBO_CONFIG_MODE, ConfigMode.IGNORE.name());
    }

    @AfterClass
    public static void after() {
        if (OLD_VALUE_SHUTDOWN_WAIT_KEY == null) {
            System.clearProperty(CommonConstants.SHUTDOWN_WAIT_KEY);
        } else {
            System.setProperty(CommonConstants.SHUTDOWN_WAIT_KEY, OLD_VALUE_SHUTDOWN_WAIT_KEY);
        }

        if (OLD_VALUE_DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE == null) {
            System.clearProperty(ConfigKeys.DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE);
        } else {
            System.setProperty(ConfigKeys.DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE,
                OLD_VALUE_DUBBO_CONFIG_IGNORE_DUPLICATED_INTERFACE);
        }

        if (OLD_VALUE_DUBBO_CONFIG_MODE == null) {
            System.clearProperty(ConfigKeys.DUBBO_CONFIG_MODE);
        } else {
            System.setProperty(ConfigKeys.DUBBO_CONFIG_MODE, OLD_VALUE_DUBBO_CONFIG_MODE);
        }
    }

    @After
    public void afterMethod() {
        RpcInternalContext.removeAllContext();
        RpcInvokeContext.removeContext();
    }

    @Test
    //同步调用,直连
    public void testSync() {
        try {
            // 只有1个线程 执行
            ServerConfig serverConfig = new ServerConfig()
                .setStopTimeout(60000)
                .setPort(20880)
                .setProtocol("dubbo")
                .setQueues(100).setCoreThreads(1).setMaxThreads(2);

            // 发布一个服务，每个请求要执行1秒
            ApplicationConfig serverApplacation = new ApplicationConfig();
            serverApplacation.setAppName("server");
            providerConfig = new ProviderConfig<DemoService>()
                .setInterfaceId(DemoService.class.getName())
                .setRef(new DemoServiceImpl())
                .setBootstrap("dubbo")
                .setServer(serverConfig)
                // .setParameter(RpcConstants.CONFIG_HIDDEN_KEY_WARNING, "false")
                .setRegister(false).setApplication(serverApplacation);
            providerConfig.export();

            ApplicationConfig clientApplication = new ApplicationConfig();
            clientApplication.setAppName("client");
            consumerConfig = new ConsumerConfig<DemoService>()
                .setInterfaceId(DemoService.class.getName())
                .setDirectUrl("dubbo://127.0.0.1:20880")
                .setBootstrap("dubbo")
                .setTimeout(30000)
                .setRegister(false).setProtocol("dubbo").setApplication(clientApplication);
            final DemoService demoService = consumerConfig.refer();

            String result = demoService.sayHello("xxx");
            Assert.assertTrue(result.equalsIgnoreCase("hello xxx"));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    @Test
    //单向调用
    public void testOneWay() {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(20880)
            .setProtocol("dubbo")
            .setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ApplicationConfig serverApplacation = new ApplicationConfig();
        serverApplacation.setAppName("server");
        providerConfig = new ProviderConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setRef(new DemoServiceImpl())
            .setServer(serverConfig)
            .setBootstrap("dubbo")
            // .setParameter(RpcConstants.CONFIG_HIDDEN_KEY_WARNING, "false")
            .setRegister(false).setApplication(serverApplacation);
        providerConfig.export();

        ApplicationConfig clientApplication = new ApplicationConfig();
        clientApplication.setAppName("client");

        List<MethodConfig> methodConfigs = new ArrayList<MethodConfig>();

        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setInvokeType(RpcConstants.INVOKER_TYPE_ONEWAY);
        methodConfig.setName("sayHello");

        methodConfigs.add(methodConfig);
        consumerConfig = new ConsumerConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setDirectUrl("dubbo://127.0.0.1:20880")
            .setTimeout(30000)
            .setRegister(false)
            .setProtocol("dubbo")
            .setBootstrap("dubbo")
            .setApplication(clientApplication)
            .setInvokeType(RpcConstants.INVOKER_TYPE_ONEWAY)
            .setMethods(methodConfigs);
        final DemoService demoService = consumerConfig.refer();
        String tmp = demoService.sayHello("xxx");
        Assert.assertEquals(null, tmp);

    }

    @Test
    //future调用,从future中取值.
    public void testFuture() {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(20880)
            .setProtocol("dubbo")
            .setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ApplicationConfig serverApplacation = new ApplicationConfig();
        serverApplacation.setAppName("server");
        providerConfig = new ProviderConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setRef(new DemoServiceImpl())
            .setServer(serverConfig)
            .setBootstrap("dubbo")
            // .setParameter(RpcConstants.CONFIG_HIDDEN_KEY_WARNING, "false")
            .setRegister(false).setApplication(serverApplacation);
        providerConfig.export();

        ApplicationConfig clientApplication = new ApplicationConfig();
        clientApplication.setAppName("client");
        List<MethodConfig> methodConfigs = new ArrayList<MethodConfig>();

        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE);
        methodConfig.setName("sayHello");
        consumerConfig = new ConsumerConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setDirectUrl("dubbo://127.0.0.1:20880")
            .setTimeout(30000)
            .setRegister(false).setProtocol("dubbo")
            .setBootstrap("dubbo")
            .setApplication(clientApplication)
            .setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE)
            .setMethods(methodConfigs);
        final DemoService demoService = consumerConfig.refer();

        String result = demoService.sayHello("xxx");
        Assert.assertEquals(null, result);

        Future<Object> future = RpcContext.getContext().getFuture();

        String futureResult = null;
        try {
            futureResult = (String) future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Assert.assertEquals("Hello xxx", futureResult);

    }

    @Test
    //同步泛化调用,直连
    public void testGenericSync() {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(20880)
            .setProtocol("dubbo")
            .setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ApplicationConfig serverApplacation = new ApplicationConfig();
        serverApplacation.setAppName("server");
        providerConfig = new ProviderConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setRef(new DemoServiceImpl())
            .setBootstrap("dubbo")
            .setServer(serverConfig)
            // .setParameter(RpcConstants.CONFIG_HIDDEN_KEY_WARNING, "false")
            .setRegister(false).setApplication(serverApplacation);
        providerConfig.export();

        ApplicationConfig clientApplication = new ApplicationConfig();
        clientApplication.setAppName("client");
        consumerConfig = new ConsumerConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setDirectUrl("dubbo://127.0.0.1:20880")
            .setBootstrap("dubbo")
            .setTimeout(30000)
            .setRegister(false)
            .setProtocol("dubbo")
            .setApplication(clientApplication)
            .setGeneric(true);
        final GenericService demoService = (GenericService) consumerConfig.refer();

        String result = (String) demoService.$invoke("sayHello", new String[] { "java.lang.String" },
            new Object[] { "xxx" });
        Assert.assertEquals(result, "Hello xxx");

    }

    @Test
    //同步调用,直连,dubbo 服务指定版本version
    public void testWithParameterWithVersion() {
        try {
            // 只有1个线程 执行
            ServerConfig serverConfig = new ServerConfig()
                .setStopTimeout(60000)
                .setPort(20880)
                .setProtocol("dubbo")
                .setQueues(100).setCoreThreads(1).setMaxThreads(2);

            // 发布一个服务，每个请求要执行1秒
            ApplicationConfig serverApplacation = new ApplicationConfig();
            serverApplacation.setAppName("server");
            providerConfig = new ProviderConfig<DemoService>()
                .setInterfaceId(DemoService.class.getName())
                .setRef(new DemoServiceImpl())
                .setBootstrap("dubbo")
                .setServer(serverConfig)
                .setParameter("version", "1.0.1")
                .setRegister(false).setApplication(serverApplacation);
            providerConfig.export();

            ApplicationConfig clientApplication = new ApplicationConfig();
            clientApplication.setAppName("client");
            consumerConfig = new ConsumerConfig<DemoService>()
                .setInterfaceId(DemoService.class.getName())
                .setDirectUrl("dubbo://127.0.0.1:20880")
                .setBootstrap("dubbo")
                .setTimeout(30000)
                .setParameter("version", "1.0.1")
                .setRegister(false).setProtocol("dubbo").setApplication(clientApplication);
            final DemoService demoService = consumerConfig.refer();

            String result = demoService.sayHello("xxx");
            Assert.assertTrue(result.equalsIgnoreCase("hello xxx"));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    @Test(expected = org.apache.dubbo.rpc.RpcException.class)
    //同步调用,直连,dubbo 消费没有指定dubbo服务版本version
    public void testConsumerWithNoDubboServiceVersion() {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(60000)
            .setPort(20880)
            .setProtocol("dubbo")
            .setQueues(100).setCoreThreads(1).setMaxThreads(2);

        // 发布一个服务，每个请求要执行1秒
        ApplicationConfig serverApplacation = new ApplicationConfig();
        serverApplacation.setAppName("server");
        providerConfig = new ProviderConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setRef(new DemoServiceImpl())
            .setBootstrap("dubbo")
            .setServer(serverConfig)
            .setParameter("version", "1.0.1")
            .setRegister(false).setApplication(serverApplacation);
        providerConfig.export();

        ApplicationConfig clientApplication = new ApplicationConfig();
        clientApplication.setAppName("client");
        consumerConfig = new ConsumerConfig<DemoService>()
            .setInterfaceId(DemoService.class.getName())
            .setDirectUrl("dubbo://127.0.0.1:20880")
            .setBootstrap("dubbo")
            .setTimeout(30000)
            .setRegister(false).setProtocol("dubbo").setApplication(clientApplication);
        final DemoService demoService = consumerConfig.refer();

        String result = demoService.sayHello("xxx");
        Assert.assertTrue(result.equalsIgnoreCase("hello xxx"));

    }
}