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
package com.alipay.sofa.rpc.protobuf;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.bolt.BoltServer;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProtobufServiceServerMain {

    /** Logger for ProtobufServiceServerMain **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtobufServiceServerMain.class);

    public static void main(String[] args) {
        ServerConfig serverConfig = new ServerConfig()
            .setProtocol("bolt") // 设置一个协议，默认bolt
            .setPort(12200) // 设置一个端口，默认12200
            .setDaemon(false); // 非守护线程

        ProviderConfig<ProtoService> providerConfig = new ProviderConfig<ProtoService>()
            .setInterfaceId(ProtoService.class.getName()) // 指定接口
            .setRef(new ProtoServiceImpl()) // 指定实现
            .setServer(serverConfig); // 指定服务端

        providerConfig.export(); // 发布服务

        LOGGER.error("started at pid {}", RpcRuntimeContext.PID);

        final AtomicInteger cnt = ((ProtoServiceImpl) providerConfig.getRef()).getCounter();
        final ThreadPoolExecutor executor = ((BoltServer) serverConfig.getServer()).getBizThreadPool();
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
