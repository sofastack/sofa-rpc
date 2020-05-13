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
package com.alipay.sofa.rpc.triple;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.fliggy.fcecore.tripleservice.generated.MultiMtopTripleRequest;
import com.fliggy.fcecore.tripleservice.generated.MultiMtopTripleResponse;
import com.fliggy.fcecore.tripleservice.generated.SofaGreeterTriple;

/**
 * @author <a href="mailto:luanyanqiang@dibgroup.cn">Luan Yanqiang</a>
 */
public class FiggerRegistryApplication {
    private final static Logger LOGGER = LoggerFactory.getLogger(FiggerRegistryApplication.class);

    public static void main(String[] args) {

        ApplicationConfig clientApp = new ApplicationConfig().setAppName("triple-client");

        /*   RegistryConfig registryConfig = new RegistryConfig();
           registryConfig.setProtocol("zookeeper").setAddress("127.0.0.1:2181");*/

        ConsumerConfig<SofaGreeterTriple.IGreeter> consumerConfig = new ConsumerConfig<SofaGreeterTriple.IGreeter>();
        consumerConfig.setInterfaceId(SofaGreeterTriple.IGreeter.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            //.setRegistry(registryConfig)
            .setApplication(clientApp)
            .setDirectUrl("tri://sofagw-rz00b-100083024149.eu95.alipay.net:80")
            .setRegister(false);

        SofaGreeterTriple.IGreeter greeterBlockingStub = consumerConfig.refer();

        LOGGER.info("Triple stub bean successful: {}", greeterBlockingStub.getClass().getName());

        MultiMtopTripleRequest request = MultiMtopTripleRequest.newBuilder().setRids("1").build();
        MultiMtopTripleResponse response = greeterBlockingStub.getTripleData(request);

        System.out.println("fuck");
        System.out.println(response.getData());
    }
}
