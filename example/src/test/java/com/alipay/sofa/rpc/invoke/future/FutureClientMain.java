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
package com.alipay.sofa.rpc.invoke.future;

import com.alipay.sofa.rpc.api.future.SofaResponseFuture;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.message.BoltResponseFuture;
import com.alipay.sofa.rpc.test.EchoService;
import com.alipay.sofa.rpc.test.HelloService;

/**
 * <p>接口级别的Callback</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class FutureClientMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(FutureClientMain.class);

    public static void main(String[] args) throws InterruptedException {

        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("future-client");

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setApplication(applicationConfig)
            .setInterfaceId(HelloService.class.getName())
            .setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE)
            .setTimeout(50000)
            .setDirectUrl("bolt://127.0.0.1:22222?appName=future-server");
        HelloService helloService = consumerConfig.refer();

        ConsumerConfig<EchoService> consumerConfig2 = new ConsumerConfig<EchoService>()
            .setApplication(applicationConfig)
            .setInterfaceId(EchoService.class.getName())
            .setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE)
            .setTimeout(50000)
            .setDirectUrl("bolt://127.0.0.1:22222?appName=future-server");
        EchoService echoService = consumerConfig2.refer();

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);

        while (true) {
            try {
                String s1 = helloService.sayHello("xxx", 22);
                LOGGER.warn("must null :{}", s1);
                BoltResponseFuture future1 = (BoltResponseFuture) SofaResponseFuture.getFuture();

                String s2 = echoService.echoStr("yyy");
                LOGGER.warn("must null :{}", s2);
                BoltResponseFuture future2 = (BoltResponseFuture) SofaResponseFuture.getFuture();

                s1 = (String) future1.get();
                LOGGER.warn("get future1: {}, elapse: {}", s1, future1.getElapsedTime());
                s2 = (String) future2.get();
                LOGGER.warn("get future2: {}, elapse: {}", s2, future2.getElapsedTime());

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(2000);
            } catch (Exception ignore) {
            }
        }

    }

}
