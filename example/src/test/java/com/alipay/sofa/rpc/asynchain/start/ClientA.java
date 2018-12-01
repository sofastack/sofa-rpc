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
package com.alipay.sofa.rpc.asynchain.start;

import com.alipay.sofa.rpc.asynchain.ServiceB;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ClientA {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClientA.class);

    public static void main(String[] args) {
        // A 服务
        ConsumerConfig<ServiceB> consumerConfig = new ConsumerConfig<ServiceB>()
            .setApplication(new ApplicationConfig().setAppName("AAA"))
            .setInterfaceId(ServiceB.class.getName())
            .setDirectUrl("bolt://127.0.0.1:12298?appName=BBB")
            .setRegister(false)
            .setTimeout(3000);

        ServiceB serviceB = consumerConfig.refer();

        while (true) {
            try {
                int ret0 = serviceB.getInt(999);
                LOGGER.info("ret0:" + ret0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(3000);
            } catch (Exception e) {

            }
        }
    }
}
