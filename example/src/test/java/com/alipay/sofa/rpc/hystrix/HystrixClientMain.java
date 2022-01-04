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

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.HelloService;

/**
 * @author BaoYi
 * @date 2021/12/26 1:16 PM
 */
public class HystrixClientMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(HystrixClientMain.class);

    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-hystrix");

        // 全局开启
        RpcConfigs.putValue(HystrixConstants.SOFA_HYSTRIX_ENABLED, true);

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setApplication(application)
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22999")
            .setRegister(false)
            .setTimeout(3000)
            .setParameter(HystrixConstants.SOFA_HYSTRIX_ENABLED, String.valueOf(true));

        // 可以直接使用默认的 FallbackFactory 直接注入 Fallback 实现
        //SofaHystrixConfig.registerFallback(consumerConfig, new HelloServiceFallback());
        // 也可以自定义 FallbackFactory 直接注入 FallbackFactory
        SofaHystrixConfig.registerFallbackFactory(consumerConfig, new HelloServiceFallbackFactory());

        HelloService helloService = consumerConfig.refer();

        String rpcResp = helloService.sendMsg("test", -1);
        LOGGER.info(rpcResp);

        String hystrixResp = helloService.sendMsg("timeoutTest", 3000);
        LOGGER.info(hystrixResp);
    }

}
