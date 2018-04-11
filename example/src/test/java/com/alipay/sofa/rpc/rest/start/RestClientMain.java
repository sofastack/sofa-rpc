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
package com.alipay.sofa.rpc.rest.start;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.rest.ExampleObj;
import com.alipay.sofa.rpc.rest.RestService;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class RestClientMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(RestClientMain.class);

    public static void main(String[] args) throws InterruptedException {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-client");
        ConsumerConfig<RestService> consumerConfig = new ConsumerConfig<RestService>()
            .setApplication(application)
            .setInterfaceId(RestService.class.getName())
            .setProtocol("rest")
            .setBootstrap("rest")
            .setDirectUrl("rest://127.0.0.1:8888")
            //.setRegister(false)
            .setTimeout(3000);
        RestService helloService = consumerConfig.refer();

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);

        while (true) {
            try {
                String s = helloService.add(22, "xxx");
                LOGGER.warn("add {}", s);
                s = helloService.query(22);
                LOGGER.warn("get {}", s);
                List<ExampleObj> es = new ArrayList<ExampleObj>();
                es.add(new ExampleObj().setName("xxx").setId(1));
                es.add(new ExampleObj().setName("yyy").setId(2));
                List<ExampleObj> rs = helloService.objects(es);
                LOGGER.warn("rs {}", rs.size());
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
        }
    }

}
