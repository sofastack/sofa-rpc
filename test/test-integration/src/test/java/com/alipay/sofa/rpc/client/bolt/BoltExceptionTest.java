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
package com.alipay.sofa.rpc.client.bolt;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRouteException;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.HelloService;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class BoltExceptionTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {

        final String directUrl = "bolt://127.0.0.1:12300";
        final ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl(directUrl)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setBootstrap("bolt")
            .setApplication(new ApplicationConfig().setAppName("clientApp"))
            .setReconnectPeriod(1000);

        HelloService helloService = consumerConfig.refer();

        // 关闭后再调用一个抛异常
        try {
            helloService.sayHello("xx", 22);
        } catch (Exception e) {
            // 应该抛出异常
            Assert.assertTrue(e instanceof SofaRouteException);

            Assert.assertTrue(e.getMessage().contains(directUrl));
        }
    }
}
