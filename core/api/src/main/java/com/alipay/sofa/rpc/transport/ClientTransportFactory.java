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
package com.alipay.sofa.rpc.transport;

import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.annotation.VisibleForTesting;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory of ClientTransport
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ClientTransportFactory {
    /**
     * slf4j Logger for this class
     */
    private final static Logger                LOGGER                  = LoggerFactory
                                                                           .getLogger(ClientTransportFactory.class);

    /**
     * 不可复用长连接管理器
     */
    private final static ClientTransportHolder CLIENT_TRANSPORT_HOLDER = new NotReusableClientTransportHolder();

    /**
     * 销毁长连接
     *
     * @param clientTransport   ClientTransport
     * @param disconnectTimeout disconnect timeout
     */
    public static void releaseTransport(ClientTransport clientTransport, int disconnectTimeout) {
        if (clientTransport == null) {
            return;
        }
        boolean needDestroy = CLIENT_TRANSPORT_HOLDER.removeClientTransport(clientTransport);
        // 执行销毁动作
        if (needDestroy) {
            if (disconnectTimeout > 0) { // 需要等待结束时间
                int count = clientTransport.currentRequests();
                if (count > 0) { // 有正在调用的请求
                    long start = RpcRuntimeContext.now();
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("There are {} outstanding call in transport, wait {}ms to end",
                            count, disconnectTimeout);
                    }
                    while (clientTransport.currentRequests() > 0
                        && RpcRuntimeContext.now() - start < disconnectTimeout) { // 等待返回结果
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                    }
                } // 关闭前检查已有请求？
            }
            // disconnectTimeout已过
            int count = clientTransport.currentRequests();
            if (count > 0) { // 还有正在调用的请求
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("There are {} outstanding call in client transport, but shutdown now.", count);
                }
            }
            // 反向的也删一下
            if (REVERSE_CLIENT_TRANSPORT_MAP != null) {
                String key = NetUtils.channelToString(clientTransport.remoteAddress(), clientTransport.localAddress());
                REVERSE_CLIENT_TRANSPORT_MAP.remove(key);
            }
            clientTransport.destroy();
        }
    }

    /**
     * 通过配置获取长连接
     *
     * @param config 传输层配置
     * @return 传输层
     */
    public static ClientTransport getClientTransport(ClientTransportConfig config) {
        return CLIENT_TRANSPORT_HOLDER.getClientTransport(config);
    }

    /**
     * 关闭全部客户端连接
     */
    public static void closeAll() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Shutdown all client transport now!");
        }
        try {
            CLIENT_TRANSPORT_HOLDER.destroy();
        } catch (Exception e) {
            LOGGER.error(LogCodes.getLog(LogCodes.ERROR_DESTORY_ALL_TRANSPORT), e);
        }
    }

    @VisibleForTesting
    static ClientTransportHolder getClientTransportHolder() {
        return CLIENT_TRANSPORT_HOLDER;
    }

    /**
     * 反向虚拟的长连接对象, 缓存一个长连接一个<br>
     * {"127.0.0.1:22000<->127.0.0.1:54321": ClientTransport}
     */
    private volatile static ConcurrentMap<String, ClientTransport> REVERSE_CLIENT_TRANSPORT_MAP = null;

    /**
     * 构建反向的（服务端到客户端）虚拟长连接
     *
     * @param container Container of client transport
     * @param channel   Exists channel from client
     * @return reverse client transport of exists channel
     */
    public static ClientTransport getReverseClientTransport(String container, AbstractChannel channel) {
        if (REVERSE_CLIENT_TRANSPORT_MAP == null) { // 初始化
            synchronized (ClientTransportFactory.class) {
                if (REVERSE_CLIENT_TRANSPORT_MAP == null) {
                    REVERSE_CLIENT_TRANSPORT_MAP = new ConcurrentHashMap<String, ClientTransport>();
                }
            }
        }
        String key = NetUtils.channelToString(channel.remoteAddress(), channel.localAddress());
        ClientTransport transport = REVERSE_CLIENT_TRANSPORT_MAP.get(key);
        if (transport == null) {
            synchronized (ClientTransportFactory.class) {
                transport = REVERSE_CLIENT_TRANSPORT_MAP.get(key);
                if (transport == null) {
                    ClientTransportConfig config = new ClientTransportConfig()
                        .setProviderInfo(new ProviderInfo().setHost(channel.remoteAddress().getHostName())
                            .setPort(channel.remoteAddress().getPort()))
                        .setContainer(container);
                    transport = ExtensionLoaderFactory.getExtensionLoader(ClientTransport.class)
                        .getExtension(config.getContainer(),
                            new Class[] { ClientTransportConfig.class },
                            new Object[] { config });
                    transport.setChannel(channel);
                    REVERSE_CLIENT_TRANSPORT_MAP.put(key, transport); // 保存唯一长连接
                }
            }
        }
        return transport;
    }

    /**
     * Find reverse client transport by channel key
     *
     * @param channelKey channel key
     * @return client transport
     * @see ClientTransportFactory#getReverseClientTransport
     * @see NetUtils#channelToString
     */
    public static ClientTransport getReverseClientTransport(String channelKey) {
        return REVERSE_CLIENT_TRANSPORT_MAP != null ?
            REVERSE_CLIENT_TRANSPORT_MAP.get(channelKey) : null;
    }

    /**
     * Remove client transport from reverse map by channel key
     *
     * @param channelKey channel key
     */
    public static void removeReverseClientTransport(String channelKey) {
        if (REVERSE_CLIENT_TRANSPORT_MAP != null) {
            REVERSE_CLIENT_TRANSPORT_MAP.remove(channelKey);
        }
    }
}
