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
package com.alipay.sofa.rpc.hystrix;

import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * @author <a href=mailto:scienjus@gmail.com>ScienJus</a>
 */
public class HystrixFilterSyncTest extends ActivelyDestroyTest {

    private ProviderConfig<HystrixService> providerConfig;
    private ServerConfig                   serverConfig;
    private ConsumerConfig<HystrixService> consumerConfig;

    @After
    public void afterMethod() {
        if (providerConfig != null) {
            providerConfig.unExport();
        }
        if (serverConfig != null) {
            serverConfig.destroy();
        }
        if (consumerConfig != null) {
            consumerConfig.unRefer();
        }

    }

    @Test
    public void testSuccess() {
        providerConfig = defaultServer(0);
        providerConfig.export();

        consumerConfig = defaultClient();

        HystrixService helloService = consumerConfig.refer();

        String result = helloService.sayHello("abc", 24);
        Assert.assertEquals("hello abc from server! age: 24", result);

    }

    @Test
    public void testHystrixTimeout() {
        providerConfig = defaultServer(2000);
        providerConfig.export();

        consumerConfig = defaultClient()
            .setTimeout(10000);

        HystrixService helloService = consumerConfig.refer();

        try {
            helloService.sayHello("abc", 24);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof SofaRpcException);
            Assert.assertTrue(e.getCause() instanceof HystrixRuntimeException);
        }
    }

    @Test
    public void testHystrixFallback() {
        providerConfig = defaultServer(2000);
        providerConfig.export();

        consumerConfig = defaultClient()
            .setTimeout(10000)
            .setFilterRef(Collections.<Filter> singletonList(new HystrixFilter()));

        SofaHystrixConfig.registerFallback(consumerConfig, new HystrixServiceFallback());

        HystrixService helloService = consumerConfig.refer();

        String result = helloService.sayHello("abc", 24);
        Assert.assertEquals("fallback abc from server! age: 24", result);
    }

    @Test
    public void testHystrixFallbackFactory() {
        providerConfig = defaultServer(2000);
        providerConfig.export();

        consumerConfig = defaultClient()
            .setTimeout(10000);

        SofaHystrixConfig.registerFallbackFactory(consumerConfig, new HystrixServiceFallbackFactory());

        HystrixService helloService = consumerConfig.refer();

        String result = helloService.sayHello("abc", 24);
        Assert.assertEquals(
            "fallback abc from server! age: 24, error: com.netflix.hystrix.exception.HystrixTimeoutException",
            result);
    }

    private ProviderConfig<HystrixService> defaultServer(int sleep) {
        serverConfig = new ServerConfig()
            .setPort(22222)
            .setDaemon(false);

        return new ProviderConfig<HystrixService>()
            .setInterfaceId(HystrixService.class.getName())
            .setRef(new InvokeCounterHystrixService(sleep))
            .setServer(serverConfig);
    }

    private ConsumerConfig<HystrixService> defaultClient() {
        return new ConsumerConfig<HystrixService>()
            .setInterfaceId(HystrixService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setParameter(HystrixConstants.SOFA_HYSTRIX_ENABLED, String.valueOf(true));
    }
}
