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
package com.dib.sofa.rpc.bootstrap.grpc;

import com.alipay.sofa.rpc.bootstrap.ProviderBootstrap;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.Version;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.*;
import com.alipay.sofa.rpc.ext.Extension;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.BindableService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

/**
 * Provider bootstrap for grpc
 *
 * @author <a href=mailto:luanyanqiang@dibgroup.cn>Luan Yanqiang</a>
 */
@Extension("grpc")
public class GrpcProviderBootstrap<T> extends ProviderBootstrap<T> {

    /**
     * 构造函数
     *
     * @param providerConfig 服务发布者配置
     */
    protected GrpcProviderBootstrap(ProviderConfig<T> providerConfig) {
        super(providerConfig);

    }

    /**
     * 是否已发布
     */
    protected transient volatile boolean exported;

    private Server                       server;
    private String                       host;
    private int                          port;
    private final static Logger          LOGGER = LoggerFactory
                                                    .getLogger(GrpcProviderBootstrap.class.getName());

    @Override
    public void export() {
        if (exported) {
            return;
        }

        String interfaceType = providerConfig.getInterfaceId();
        Object ref = providerConfig.getRef();
        if (ref instanceof String) {
            try {
                ref = getInterfaceClass(interfaceType).newInstance();
            } catch (Exception e) {
                //TODO: handle exception
            }
        }

        try {
            port = (providerConfig.getServer().get(0)).getPort();
            server = ServerBuilder.forPort(port)
                .addService((io.grpc.BindableService) ref)
                // .addService((io.grpc.BindableService) providerConfig.getRef())
                .build()
                .start();
            exported = true;
        } catch (IOException e) {
            LOGGER.error("starting GPRC server fails.");
        }

        LOGGER.info("GRPC server starts successfully, port: {}", port);

        return;
    }

    public Class<?> getInterfaceClass(String interfaceType) {

        try {
            Class<?> interfaceClass = this.getClass().getClassLoader().loadClass(interfaceType);
            return interfaceClass;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            // No type found for shortcut FactoryBean instance:
            // fall back to full creation of the FactoryBean instance
            return null;
        } finally {
            return null;
        }
    }

    /**
     * 取消发布（从server里取消注册）
     */
    @Override
    public synchronized void unExport() {
        if (!exported) {
            return;
        }
        // serviceConfig.unexport();
        exported = false;
    }

    /**
     * 得到已发布的全部list
     *
     * @return urls urls
     */
    public List<String> buildUrls() {
        if (exported) {
            List<ServerConfig> servers = providerConfig.getServer();
            if (servers != null && !servers.isEmpty()) {
                List<String> urls = new ArrayList<String>();
                for (ServerConfig server : servers) {
                    StringBuilder sb = new StringBuilder(200);
                    sb.append(server.getProtocol()).append("://").append(server.getHost())
                        .append(":").append(server.getPort()).append(server.getContextPath())
                        .append(providerConfig.getInterfaceId())
                        .append("?uniqueId=").append(providerConfig.getUniqueId())
                        .append(getKeyPairs("version", "1.0"))
                        .append(getKeyPairs("delay", providerConfig.getDelay()))
                        .append(getKeyPairs("weight", providerConfig.getWeight()))
                        .append(getKeyPairs("register", providerConfig.isRegister()))
                        .append(getKeyPairs("maxThreads", server.getMaxThreads()))
                        .append(getKeyPairs("ioThreads", server.getIoThreads()))
                        .append(getKeyPairs("threadPoolType", server.getThreadPoolType()))
                        .append(getKeyPairs("accepts", server.getAccepts()))
                        .append(getKeyPairs("dynamic", providerConfig.isDynamic()))
                        .append(getKeyPairs(RpcConstants.CONFIG_KEY_RPC_VERSION, Version.RPC_VERSION));
                    urls.add(sb.toString());
                }
                return urls;
            }
        }
        return null;
    }

    /**
     * Gets key pairs.
     *
     * @param key   the key
     * @param value the value
     * @return the key pairs
     */
    private String getKeyPairs(String key, Object value) {
        if (value != null) {
            return "&" + key + "=" + value.toString();
        } else {
            return "";
        }
    }
}
