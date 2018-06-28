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
package com.alipay.sofa.rpc.test.exception;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p></p>
 * <p>
 *
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
public class BizThrowExceptionTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {

        // 只有2个线程 执行
        ServerConfig serverConfig = new ServerConfig()
            .setStopTimeout(0)
            .setPort(22222)
            .setProtocol(RpcConstants.PROTOCOL_TYPE_BOLT)
            .setQueues(100).setCoreThreads(5).setMaxThreads(5);

        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<TestExceptionService> providerConfig = new ProviderConfig<TestExceptionService>()
            .setInterfaceId(TestExceptionService.class.getName())
            .setRef(new TestExceptionServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig.export();

        ConsumerConfig<TestExceptionService> consumerConfig = new ConsumerConfig<TestExceptionService>()
            .setInterfaceId(TestExceptionService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22222")
            .setTimeout(1000)
            .setRegister(false);
        final TestExceptionService service = consumerConfig.refer();

        try {
            service.throwRuntimeException();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof RuntimeException);
            Assert.assertEquals(e.getMessage(), "RuntimeException");
        }
        try {
            service.throwException();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof Exception);
            Assert.assertEquals(e.getMessage(), "Exception");
        }
        try {
            service.throwSofaException();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof SofaRpcException);
            Assert.assertEquals(e.getMessage(), "SofaRpcException");
        }
        try {
            service.throwDeclaredException();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof TestException);
            Assert.assertEquals(e.getMessage(), "TestException");
        }

        try {
            service.throwDeclaredExceptionWithoutReturn();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof TestException);
            Assert.assertEquals(e.getMessage(), "DeclaredExceptionWithoutReturn");
        }
    }
}
