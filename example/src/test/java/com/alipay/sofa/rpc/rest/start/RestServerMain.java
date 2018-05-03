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
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.rest.RestService;
import com.alipay.sofa.rpc.rest.RestServiceImpl;

/**
 *
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class RestServerMain {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(RestServerMain.class);

    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-server");

        /*
         访问地址：
         POST http://127.0.0.1:8888/rest/hello/code/name
         GET http://127.0.0.1:8888/rest/hello/code
         PUT http://127.0.0.1:8888/rest/hello/code/name
         DELETE http://127.0.0.1:8888/rest/hello/code
         GET http://127.0.0.1:8888/rest/get/1234567890
         POST http://127.0.0.1:8888/rest/post/1234567890 bodydddddd
         */

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("rest")
            .setPort(8888)
            .setDaemon(false);

        ProviderConfig<RestService> providerConfig = new ProviderConfig<RestService>()
            .setInterfaceId(RestService.class.getName())
            .setApplication(application)
            .setRef(new RestServiceImpl())
            .setBootstrap("rest")
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();

        LOGGER.warn("started at pid {}", RpcRuntimeContext.PID);
    }

}
