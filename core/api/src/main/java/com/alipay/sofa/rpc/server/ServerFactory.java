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
package com.alipay.sofa.rpc.server;

import com.alipay.sofa.rpc.common.RpcConfigs;
import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory of server
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public final class ServerFactory {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                        LOGGER     = LoggerFactory
                                                                      .getLogger(ServerFactory.class);
    /**
     * 全部服务端
     */
    private final static ConcurrentMap<String, Server> SERVER_MAP = new ConcurrentHashMap<String, Server>();

    /**
     * 初始化Server实例
     *
     * @param serverConfig 服务端配置
     * @return Server
     */
    public synchronized static Server getServer(ServerConfig serverConfig) {
        try {
            Server server = SERVER_MAP.get(Integer.toString(serverConfig.getPort()));
            if (server == null) {
                // 算下网卡和端口
                resolveServerConfig(serverConfig);

                ExtensionClass<Server> ext = ExtensionLoaderFactory.getExtensionLoader(Server.class)
                    .getExtensionClass(serverConfig.getProtocol());
                if (ext == null) {
                    throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_UNSUPPORTED_PROTOCOL,
                        serverConfig.getProtocol()));
                }
                server = ext.getExtInstance();
                server.init(serverConfig);
                SERVER_MAP.put(serverConfig.getPort() + "", server);
            }
            return server;
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_GET_SERVER), e);
        }
    }

    /**
     * 确定下Server的host和port
     *
     * @param serverConfig 服务器配置
     */
    private static void resolveServerConfig(ServerConfig serverConfig) {
        // 绑定到指定网卡 或全部网卡
        String boundHost = serverConfig.getBoundHost();
        if (boundHost == null) {
            String host = serverConfig.getHost();
            if (StringUtils.isBlank(host)) {
                host = SystemInfo.getLocalHost();
                serverConfig.setHost(host);
                // windows绑定到0.0.0.0的某个端口以后，其它进程还能绑定到该端口
                boundHost = SystemInfo.isWindows() ? host : NetUtils.ANYHOST;
            } else {
                boundHost = host;
            }
            serverConfig.setBoundHost(boundHost);
        }

        // 绑定的端口
        if (serverConfig.isAdaptivePort()) {
            int oriPort = serverConfig.getPort();
            int port = NetUtils.getAvailablePort(boundHost, oriPort,
                RpcConfigs.getIntValue(RpcOptions.SERVER_PORT_END));
            if (port != oriPort) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Changed port from {} to {} because the config port is disabled", oriPort, port);
                }
                serverConfig.setPort(port);
            }
        }
    }

    /**
     * 得到全部服务端
     *
     * @return 全部服务端
     */
    public static List<Server> getServers() {
        return new ArrayList<Server>(SERVER_MAP.values());
    }

    /**
     * 关闭全部服务端
     */
    public static void destroyAll() {
        if (CommonUtils.isEmpty(SERVER_MAP)) {
            return;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Destroy all server now!");
        }
        for (Map.Entry<String, Server> entry : SERVER_MAP.entrySet()) {
            String key = entry.getKey();
            Server server = entry.getValue();
            try {
                server.destroy();
            } catch (Exception e) {
                LOGGER.error(LogCodes.getLog(LogCodes.ERROR_DESTROY_SERVER, key), e);
            }
        }
        SERVER_MAP.clear();
    }

    public static void destroyServer(ServerConfig serverConfig) {
        try {
            Server server = serverConfig.getServer();
            if (server != null) {
                serverConfig.setServer(null);
                SERVER_MAP.remove(Integer.toString(serverConfig.getPort()));
                server.destroy();
            }
        } catch (Exception e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_DESTROY_SERVER, serverConfig.getPort()), e);
        }
    }
}
