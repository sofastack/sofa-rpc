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
package com.alipay.sofa.rpc.triple.ark;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.test.AnotherHelloServiceImpl;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;
import org.junit.Test;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author junyuan
 * @version MultiClassLoaderTest.java, v 0.1 2022年12月30日 15:02 junyuan Exp $
 */
public class MultiClassLoaderTest {

    private static final AtomicInteger PORT = new AtomicInteger(51003);

    public void init() {

    }

    /*
     *          Launcher.AppClassLoader   -> HelloService (Interface)
     *              /          \
     *             /            \
     *          cl1              cl2
     *           |                |
     *           v                v
     *    HelloServiceImpl      AnotherHelloServiceImpl
     */
    @Test
    public void test() throws InterruptedException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        URL url4Impl = HelloServiceImpl.class.getProtectionDomain().getCodeSource().getLocation();
        URL url4AnotherImpl = AnotherHelloServiceImpl.class.getProtectionDomain().getCodeSource().getLocation();

        SpecificTestClassLoader cl1 = new SpecificTestClassLoader("test_classloader_1", new URL[]{url4Impl}, oldClassLoader);
        cl1.addWhiteListClass(HelloServiceImpl.class.getName());

        SpecificTestClassLoader cl2 = new SpecificTestClassLoader("test_classloader_2", new URL[]{url4AnotherImpl}, oldClassLoader);
        cl2.addWhiteListClass(AnotherHelloServiceImpl.class.getName());

        SpecificTestClassLoader clientClassloader = new SpecificTestClassLoader("client_classLoader", new URL[]{}, oldClassLoader);

        int port = getPort();

        ServerConfig serverConfig = new ServerConfig()
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setPort(port);

        ApplicationConfig clientApp = new ApplicationConfig().setAppName("triple-client");
        ApplicationConfig serverApp = new ApplicationConfig().setAppName("triple-server");

        Thread.currentThread().setContextClassLoader(cl1);
        ProviderConfig<HelloService> providerConfig1 = getProviderConfig(1)
                .setServer(serverConfig)
                .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setInterfaceId(HelloService.class.getName())
                .setRef(new HelloServiceImpl())
                .setApplication(serverApp)
                .setRegister(false)
                .setUniqueId("helloService");
        providerConfig1.export();

        Thread.currentThread().setContextClassLoader(cl2);
        ProviderConfig<HelloService> providerConfig2 = getProviderConfig(1)
                .setServer(serverConfig)
                .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setInterfaceId(HelloService.class.getName())
                .setRef(new AnotherHelloServiceImpl())
                .setApplication(serverApp)
                .setRegister(false)
                .setUniqueId("anotherHelloService");
        providerConfig2.export();

        Thread.currentThread().setContextClassLoader(clientClassloader);
        ConsumerConfig<HelloService> consumerConfig1 = new ConsumerConfig<>();
        consumerConfig1.setInterfaceId(HelloService.class.getName())
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setDirectUrl("tri://127.0.0.1:" + port)
                .setRegister(false)
                .setApplication(clientApp)
                .setUniqueId("helloService");
        HelloService helloService1 = consumerConfig1.refer();

        ConsumerConfig<HelloService> consumerConfig2 = new ConsumerConfig<>();
        consumerConfig2.setInterfaceId(HelloService.class.getName())
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setDirectUrl("tri://127.0.0.1:" + port)
                .setRegister(false)
                .setApplication(clientApp)
                .setUniqueId("anotherHelloService");
        HelloService helloService2 = consumerConfig2.refer();

        String result1 = helloService1.sayHello("impl1", 1);

        Assert.isTrue(result1.contains("impl1"), "helloService1 run fail, result is " + result1);

        String result2 = helloService2.sayHello("impl2", 2);
        Assert.isTrue(result2.contains("impl2"), "anotherHelloService2 run fail, result is " + result2);

        Thread.currentThread().setContextClassLoader(cl1);
        providerConfig1.unExport();
        Thread.currentThread().setContextClassLoader(cl2);
        providerConfig2.unExport();

        Thread.currentThread().setContextClassLoader(oldClassLoader);
        serverConfig.destroy();

        // =========================================================================================
        // then another brand new cl would do server init
        SpecificTestClassLoader cl3 = new SpecificTestClassLoader("test_classloader_3", new URL[]{url4AnotherImpl}, oldClassLoader);
        cl3.addWhiteListClass(AnotherHelloServiceImpl.class.getName());
        SpecificTestClassLoader clientClassloader2 = new SpecificTestClassLoader("client_classLoader_2", new URL[]{}, oldClassLoader);

        Thread.currentThread().setContextClassLoader(cl3);
        ProviderConfig<HelloService> providerConfig3 = getProviderConfig(1)
                .setServer(serverConfig)
                .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setInterfaceId(HelloService.class.getName())
                .setRef(new AnotherHelloServiceImpl())
                .setApplication(serverApp)
                .setRegister(false)
                .setUniqueId("anotherHelloService");
        providerConfig2.export();

        Thread.currentThread().setContextClassLoader(clientClassloader2);
        ConsumerConfig<HelloService> consumerConfig3 = new ConsumerConfig<>();
        consumerConfig3.setInterfaceId(HelloService.class.getName())
                .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
                .setDirectUrl("tri://127.0.0.1:" + port)
                .setRegister(false)
                .setApplication(clientApp)
                .setUniqueId("anotherHelloService");
        HelloService helloService3 = consumerConfig3.refer();

        String result3 = helloService3.sayHello("impl3", 2);
        Assert.isTrue(result3.contains("impl3"), "anotherHelloService3 run fail, result is " + result3);

        Thread.currentThread().setContextClassLoader(cl3);
        providerConfig3.unExport();

        Thread.currentThread().setContextClassLoader(oldClassLoader);
        serverConfig.destroy();
    }


    private ProviderConfig<HelloService> getProviderConfig(int exportLimit) {
        ProviderConfig<HelloService> providerConfig = new ProviderConfig<>();
        providerConfig.setRepeatedExportLimit(exportLimit);
        return providerConfig;
    }


    private int getPort() {
        int andIncrement = PORT.getAndIncrement();
        return andIncrement;
    }



    /**
     * a specific classloader
     * would load class with refClassloader if load nothing by itself
     * logic of this classloader would be similar to com.alipay.sofa.ark.container.service.classloader.BizClassLoader
     */
    class SpecificTestClassLoader extends URLClassLoader {

        private String identity;

        private ClassLoader refClassLoader;

        private Set<String/* forbidden class name */> blackList = new HashSet<>();

        /** active only if not null */
        private Set<String/* permitted class name */> whiteList = new HashSet<>();

        public SpecificTestClassLoader(String identity, URL[] urls) {
            super(urls);
            this.identity = identity;
        }

        public SpecificTestClassLoader(String identity, URL[] urls, ClassLoader ref) {
            super(urls);
            this.identity = identity;
            this.refClassLoader = ref;
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> clazz = null;

            // skip load if in black list
            if (!blackList.contains(name)) {
                clazz = whiteListLoad(name, resolve);
            }

            if (clazz == null) {
                clazz = refClassLoader.loadClass(name);
            }

            if (clazz == null) {
                throw new ClassNotFoundException();
            }

            return clazz;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            Enumeration<URL> urls = super.getResources(name);
            if (!urls.hasMoreElements()) {
                urls = refClassLoader.getResources(name);
            }
            return urls;
        }

        @Override
        public URL getResource(String name) {
            URL url = super.getResource(name);
            if (url == null ) {
                url = refClassLoader.getResource(name);
            }
            return url;
        }

        /** do load only if white list is not empty and class do in white list */
        private Class<?> whiteListLoad(String className, boolean resolve) throws ClassNotFoundException {
            Class<?> clazz = null;
            if (!whiteList.isEmpty()) {
                if (!whiteList.contains(className)) {
                    // class is not allowed to load with current cl
                    return null;
                }
            }

            return super.loadClass(className, resolve);
        }

        public void addBlackListClass(String className) {
            this.blackList.add(className);
        }

        public void addWhiteListClass(String className) {
            this.whiteList.add(className);
        }
    }
}