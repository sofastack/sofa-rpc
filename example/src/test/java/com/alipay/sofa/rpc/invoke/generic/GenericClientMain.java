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
package com.alipay.sofa.rpc.invoke.generic;

import com.alipay.hessian.generic.model.GenericObject;
import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class GenericClientMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericClientMain.class);

    public static void main(String[] args) {
        ApplicationConfig applicationConfig = new ApplicationConfig().setAppName("generic-client");

        ConsumerConfig<GenericService> consumerConfig = new ConsumerConfig<GenericService>()
            .setApplication(applicationConfig)
            .setInterfaceId(TestGenericService.class.getName())
            .setGeneric(true)
            .setTimeout(50000)
            .setDirectUrl("bolt://127.0.0.1:22222?appName=generic-server");
        GenericService testService = consumerConfig.refer();

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);

        while (true) {
            try {
                String s1 = (String) testService.$invoke("echoStr", new String[] { "java.lang.String" },
                    new Object[] { "1111" });
                LOGGER.warn("generic return :{}", s1);

                GenericObject genericObject = new GenericObject(
                    "com.alipay.sofa.rpc.invoke.generic.TestObj");
                genericObject.putField("str", "xxxx");
                genericObject.putField("num", 222);

                GenericObject o2 = (GenericObject) testService.$genericInvoke("echoObj",
                    new String[] { "com.alipay.sofa.rpc.invoke.generic.TestObj" },
                    new Object[] { genericObject });
                LOGGER.warn("generic return :{}", o2);

                TestObj o3 = testService.$genericInvoke("echoObj",
                    new String[] { "com.alipay.sofa.rpc.invoke.generic.TestObj" },
                    new Object[] { genericObject }, TestObj.class);
                LOGGER.warn("generic return :{}", o3);

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