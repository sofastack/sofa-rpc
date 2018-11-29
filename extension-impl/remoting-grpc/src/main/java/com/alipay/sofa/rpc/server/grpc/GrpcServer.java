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
package com.alipay.sofa.rpc.server.grpc;

import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.server.Server;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.util.MutableHandlerRegistry;

/**
 *
 * @author LiangEn.LiWei
 * @date 2018.11.20 5:27 PM
 */
@Extension("grpc")
public class GrpcServer implements Server {

    private static final Logger      LOGGER          = LoggerFactory.getLogger(GrpcServer.class);

    protected ServerConfig           serverConfig;

    protected volatile boolean       started;

    protected io.grpc.Server         server;

    protected MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();

    @Override
    public void init(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        server = NettyServerBuilder.forPort(9090).fallbackHandlerRegistry(handlerRegistry).build();
    }

    @Override
    public void start() {
        if (started) {
            return;
        }
        synchronized (this) {
            try {
                server.start();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Start the http rest server at port {}", serverConfig.getPort());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean hasNoEntry() {
        return false;
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        try {
            // 关闭端口，不关闭线程池
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Stop the http rest server at port {}", serverConfig.getPort());
            }
            server.shutdown();
        } catch (Exception e) {
            LOGGER.error("Stop the http rest server at port " + serverConfig.getPort() + " error !", e);
        }
        started = false;
    }

    @Override
    public void registerProcessor(ProviderConfig providerConfig, Invoker instance) {

        handlerRegistry.addService((BindableService) providerConfig.getRef());
    }

    @Override
    public void unRegisterProcessor(ProviderConfig providerConfig, boolean closeIfNoEntry) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void destroy(DestroyHook hook) {

    }
}