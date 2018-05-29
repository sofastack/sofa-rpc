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
package com.alipay.sofa.rpc.http2;

import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.protobuf.ProtoService;
import com.alipay.sofa.rpc.protobuf.ProtoServiceImpl;
import com.alipay.sofa.rpc.server.http.Http2ClearTextServer;
import com.alipay.sofa.rpc.test.HelloService;
import com.alipay.sofa.rpc.test.HelloServiceImpl;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class Http2ServerMain {
    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Http2ServerMain.class);

    public static void main(String[] args) {
        ApplicationConfig application = new ApplicationConfig().setAppName("test-server");

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("h2c")
            .setPort(12300)
            .setDaemon(false);

        ProviderConfig<ProtoService> providerConfig = new ProviderConfig<ProtoService>()
            .setInterfaceId(ProtoService.class.getName())
            .setApplication(application)
            .setRef(new ProtoServiceImpl())
            .setServer(serverConfig);

        providerConfig.export();

        ProviderConfig<HelloService> providerConfig2 = new ProviderConfig<HelloService>()
            .setInterfaceId(HelloService.class.getName())
            .setApplication(application)
            .setRef(new HelloServiceImpl())
            .setServer(serverConfig)
            .setRegister(false);
        providerConfig2.export();
        // http://127.0.0.1:12300/com.alipay.sofa.rpc.test.HelloService/sayHello

        LOGGER.error("started at pid {}", RpcRuntimeContext.PID);

        final AtomicInteger cnt = ((ProtoServiceImpl) providerConfig.getRef()).getCounter();
        final ThreadPoolExecutor executor = ((Http2ClearTextServer) serverConfig.getServer()).getBizThreadPool();
        Thread thread = new Thread(new Runnable() {
            private long last = 0;

            @Override
            public void run() {
                while (true) {
                    long count = cnt.get();
                    long tps = count - last;
                    LOGGER.error("last 1s invoke: {}, queue: {}", tps, executor.getQueue().size());
                    last = count;

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }, "Print-tps-THREAD");
        thread.start();
    }

}
