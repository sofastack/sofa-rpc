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
package com.alipay.sofa.rpc.codec.fury;

import com.alipay.sofa.rpc.codec.fury.model.Registered.DemoRequest;
import com.alipay.sofa.rpc.codec.fury.model.Registered.DemoResponse;
import com.alipay.sofa.rpc.codec.fury.model.whitelist.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertTrue;

/**
 * @author lipan
 */
public class FuryHelperTest {

    @Test
    public void testHotUpdate() throws ClassNotFoundException {
        FuryHelper furyHelper = new FuryHelper();

        // 获取初始的请求类和响应类
        Class[] initialReqClasses = furyHelper
            .getReqClass("com.alipay.sofa.rpc.codec.fury.model.Registered.HelloServiceImpl", "sayHello");
        Class[] initialRespClass = furyHelper.getRespClass(
            "com.alipay.sofa.rpc.codec.fury.model.Registered.HelloServiceImpl",
            "sayHello");

        URL url4Impl2 = HelloServiceImpl2.class.getProtectionDomain().getCodeSource().getLocation();

        SpecificTestClassLoader cl2 = new SpecificTestClassLoader("cl2", new URL[] { url4Impl2 });
        cl2.addWhiteListClass(HelloServiceImpl2.class.getName());

        // 使用类加载器加载更新后的类
        Class<?> updatedInterfaceClass = cl2
            .loadClass("com.alipay.sofa.rpc.codec.fury.model.whitelist.HelloServiceImpl2");
        System.out.println(updatedInterfaceClass);

        // 更新FuryHelper中的类加载器
        ClassLoader loader = furyHelper.getClassLoader(updatedInterfaceClass.getName());

        // 获取更新后的请求类和响应类
        Class[] updatedReqClasses = furyHelper.getReqClass(updatedInterfaceClass.getName(), "sayHello");
        Class[] updatedRespClass = furyHelper.getRespClass(updatedInterfaceClass.getName(), "sayHello");

        System.out.println(initialReqClasses[0].getClassLoader());

        // 检查是否使用了新的类加载器
        Assert.assertEquals(initialReqClasses[0].getClassLoader(), updatedReqClasses[0].getClassLoader());
        Assert.assertEquals(initialRespClass[0].getClassLoader(), updatedRespClass[0].getClassLoader());
    }

    class SpecificTestClassLoader extends URLClassLoader {

        private String                                identity;

        private ClassLoader                           refClassLoader;

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
            if (url == null) {
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

    private final FuryHelper furyHelper = new FuryHelper();

    @Test
    public void getReqClass() {
        Class[] req = furyHelper.getReqClass(
            DemoService.class.getCanonicalName(), "say");
        assertTrue(req[0] == DemoRequest.class);
    }

    @Test
    public void getResClass() {
        Class[] res = furyHelper.getRespClass(
            DemoService.class.getCanonicalName(), "say");
        assertTrue(res[0] == DemoResponse.class);
    }
}
