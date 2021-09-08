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
package com.alipay.sofa.rpc.tripleGeneric;

import com.alipay.sofa.rpc.api.GenericService;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TripleGenericTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleGenericTest.class);

    private GenericService      helloService;

    @Before
    public void init() {
        RpcRunningState.setDebugMode(true);

        ApplicationConfig clientApp = new ApplicationConfig().setAppName("triple-client");

        ApplicationConfig serverApp = new ApplicationConfig().setAppName("triple-server");

        int port = 50062;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        TripleGenericInterfaceImpl ref = new TripleGenericInterfaceImpl();
        ProviderConfig<TripleGenericInterface> providerConfig = new ProviderConfig<TripleGenericInterface>()
            .setSerialization("json")
            .setApplication(serverApp)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(TripleGenericInterface.class.getName())
            .setRef(ref)
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();

        ConsumerConfig<GenericService> consumerConfig = new ConsumerConfig<GenericService>();
        consumerConfig.setInterfaceId(TripleGenericInterface.class.getName())
            .setSerialization("json")
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("localhost:" + port)
            .setRegister(false)
            .setGeneric(true)
            .setApplication(clientApp);

        helloService = consumerConfig.refer();

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
        LOGGER.info("Grpc stub bean successful: {}", helloService.getClass().getName());
    }

    @Test
    public void testEchoStr() {
        try {
            Object echoStr = helloService.$invoke("echoStr",
                new String[] { "java.lang.String" },
                new Object[] { "zhangsan" });
            LOGGER.warn("generic return :{}", echoStr);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testEchoInt() {
        try {
            Object echoInt = helloService.$invoke("echoInt",
                new String[] { "java.lang.Integer" },
                new Object[] { 23 });
            LOGGER.warn("generic return :{}", echoInt);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testEchoDate() {
        try {
            Object echoDate = helloService.$invoke("echoDate",
                new String[] { "java.util.Date" },
                new Object[] { new Date() });
            LOGGER.warn("generic return :{}", echoDate);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testEchoEntity(){
        try{
            Map<String, Object> genericObject = new HashMap<>();
            genericObject.put("age", 23);
            genericObject.put("name", "zhangsan");
            genericObject.put("birth", new Date());
            Map<String, Object> innerMap = new HashMap<>();
            innerMap.put("add", "xiafeilu");
            genericObject.put("inner", innerMap);

            Object echoEntity = helloService.$invoke("echoEntity",
                    new String[]{"com.alipay.sofa.rpc.tripleGeneric.TestEntity"},
                    new Object[]{genericObject});
            LOGGER.warn("generic return :{}", echoEntity);
        }catch (Exception e){
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testVoidParam() {
        try {
            Object result = helloService.$invoke("testVoidParam", new String[] {}, new Object[] {});
            LOGGER.warn("generic return :{}", result);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testVoidResp() {
        try {
            helloService.$invoke("testVoidResp", new String[] {}, new Object[] {});
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
