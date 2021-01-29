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
package com.alipay.sofa.rpc.server.multi;

import com.alipay.sofa.rpc.bootstrap.DefaultProviderBootstrap;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.server.rest.RestService;
import com.alipay.sofa.rpc.server.rest.RestServiceImpl;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class MultiProtocolServerExpTest extends ActivelyDestroyTest {

    @Test
    public void testMultiProtocolExp() throws NoSuchFieldException {
        try {
            Field field = DefaultProviderBootstrap.class.getDeclaredField("EXPORTED_KEYS");
            Map map = null;
            field.setAccessible(true);
            map = (ConcurrentMap<String, AtomicInteger>) field.get(null);
            map.clear();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        }

        try {
            // 只有2个线程 执行
            ServerConfig serverConfig = new ServerConfig()
                .setStopTimeout(0)
                .setPort(22222)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
                .setQueues(100).setCoreThreads(1).setMaxThreads(2);

            // 发布一个服务，每个请求要执行1秒
            ProviderConfig<RestService> providerConfig = new ProviderConfig<RestService>()
                .setInterfaceId(RestService.class.getName())
                .setRef(new RestServiceImpl())
                .setServer(serverConfig)
                .setRepeatedExportLimit(1)
                .setRegister(false);
            providerConfig.export();

            ServerConfig serverConfig2 = new ServerConfig()
                .setStopTimeout(0)
                .setPort(22223)
                .setProtocol(RpcConstants.PROTOCOL_TYPE_REST)
                .setQueues(100).setCoreThreads(1).setMaxThreads(2);

            // 发布一个服务，每个请求要执行1秒
            ProviderConfig<RestService> providerConfig2 = new ProviderConfig<RestService>()
                .setInterfaceId(RestService.class.getName())
                .setRef(new RestServiceImpl())
                .setServer(serverConfig2)
                .setRepeatedExportLimit(1)
                .setRegister(false);
            providerConfig2.export();

            ProviderConfig<RestService> providerConfig3 = new ProviderConfig<RestService>()
                .setInterfaceId(RestService.class.getName())
                .setRef(new RestServiceImpl())
                .setServer(serverConfig2)
                .setRepeatedExportLimit(1)
                .setRegister(false);
            providerConfig3.export();

            Assert.fail();
        } catch (Throwable e) {
            //reflect to fetch export key
            ConcurrentMap<String, AtomicInteger> map = null;

            Field field = DefaultProviderBootstrap.class.getDeclaredField("EXPORTED_KEYS");
            try {
                field.setAccessible(true);
                map = (ConcurrentMap<String, AtomicInteger>) field.get(null);
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            }

            //two providers publish done, the third will false, and revert counter, export value is 1
            for (Map.Entry<String, AtomicInteger> entry : map.entrySet()) {
                AtomicInteger atomicInteger = entry.getValue();
                Assert.assertEquals(1, atomicInteger.get());
            }
        }
    }
}