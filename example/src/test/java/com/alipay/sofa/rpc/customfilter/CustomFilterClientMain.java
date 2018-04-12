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
package com.alipay.sofa.rpc.customfilter;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.test.HelloService;

import java.util.Arrays;

/**
 *
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class CustomFilterClientMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CustomFilterClientMain.class);

    public static void main(String[] args) throws InterruptedException {

        Filter customEchoFilter2 = new CustomEchoFilter2();

        ApplicationConfig application = new ApplicationConfig().setAppName("test-client");

        ConsumerConfig<HelloService> consumerConfig = new ConsumerConfig<HelloService>()
            .setApplication(application)
            .setInterfaceId(HelloService.class.getName())
            .setDirectUrl("bolt://127.0.0.1:22000")
            .setFilter(Arrays.asList("customEcho"))
            .setFilterRef(Arrays.asList(customEchoFilter2))
            .setRegister(false)
            .setTimeout(3000);
        HelloService helloService = consumerConfig.refer();

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);

        try {
            for (int i = 0; i < 100; i++) {
                try {
                    String s = helloService.sayHello("xxx", 22);
                    LOGGER.warn("{}", s);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
            }

        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

}
