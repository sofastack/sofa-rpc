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
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alipay.sofa.rpc.common.RpcConfigs.getBooleanValue;
import static com.alipay.sofa.rpc.common.RpcOptions.TRANSPORT_CONNECTION_REUSE;

/**
 * Factory of ClientTransport
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ClientTransportFactory {
    /**
     * slf4j Logger for this class
     */
    private final static Logger                                            LOGGER                = LoggerFactory
                                                                                                     .getLogger(ClientTransportFactory.class);

    /**
     * 是否长连接复用
     */
    final static boolean                                                   CHANNEL_REUSE         = getBooleanValue(TRANSPORT_CONNECTION_REUSE);

    /**
     * 长连接不复用的时候，一个ClientTransportConfig对应一个ClientTransport
     */
    final static ConcurrentHashMap<ClientTransportConfig, ClientTransport> ALL_TRANSPORT_MAP     = CHANNEL_REUSE ? null
                                                                                                     : new ConcurrentHashMap<ClientTransportConfig, ClientTransport>();

    /**
     * 长连接复用时，共享长连接的连接池，一个服务端ip和端口同一协议只建立一个长连接，不管多少接口，共用长连接
     */
    final static ConcurrentHashMap<String, ClientTransport>                CLIENT_TRANSPORT_MAP  = CHANNEL_REUSE ? new ConcurrentHashMap<String, ClientTransport>()
                                                                                                     : null;

    /**
     * 长连接复用时，共享长连接的计数器
     */
    final static ConcurrentHashMap<ClientTransport, AtomicInteger>         TRANSPORT_REF_COUNTER = CHANNEL_REUSE ? new ConcurrentHashMap<ClientTransport, AtomicInteger>()
                                                                                                     : null;

    /**
     * 通过配置获取长连接
     *
     * @param config 传输层配置
     * @return 传输层
     */
    public static ClientTransport getClientTransport(ClientTransportConfig config) {
        if (CHANNEL_REUSE) {
            String key = getAddr(config);
            ClientTransport transport = CLIENT_TRANSPORT_MAP.get(key);
            if (transport == null) {
                transport = ExtensionLoaderFactory.getExtensionLoader(ClientTransport.class)
                    .getExtension(config.getContainer(),
                        new Class[] { ClientTransportConfig.class },
                        new Object[] { config });
                ClientTransport oldTransport = CLIENT_TRANSPORT_MAP.putIfAbsent(key, transport); // 保存唯一长连接
                if (oldTransport != null) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Multiple threads init ClientTransport with same key:" + key);
                    }
                    transport.destroy(); //如果同时有人插入，则使用第一个
                    transport = oldTransport;
                }
            }
            AtomicInteger counter = TRANSPORT_REF_COUNTER.get(transport);
            if (counter == null) {
                counter = new AtomicInteger(0);
                AtomicInteger oldCounter = TRANSPORT_REF_COUNTER.putIfAbsent(transport, counter);
                if (oldCounter != null) {
                    counter = oldCounter;
                }
            }
            counter.incrementAndGet(); // 计数器加1
            return transport;
        } else {
            ClientTransport transport = ALL_TRANSPORT_MAP.get(config);
            if (transport == null) {
                transport = ExtensionLoaderFactory.getExtensionLoader(ClientTransport.class)
                    .getExtension(config.getContainer(),
                        new Class[] { ClientTransportConfig.class },
                        new Object[] { config });
                ClientTransport old = ALL_TRANSPORT_MAP.putIfAbsent(config, transport); // 保存唯一长连接
                if (old != null) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Multiple threads init ClientTransport with same ClientTransportConfig!");
                    }
                    transport.destroy(); //如果同时有人插入，则使用第一个
                    transport = old;
                }
            }
            return transport;
        }
    }

    private static String getAddr(ClientTransportConfig config) {
        ProviderInfo providerInfo = config.getProviderInfo();
        return providerInfo.getProtocolType() + "://" + providerInfo.getHost() + ":" + providerInfo.getPort();
    }

    /**
     * 销毁长连接
     *
     * @param clientTransport ClientTransport
     * @param disconnectTimeout disconnect timeout
     */
    public static void releaseTransport(ClientTransport clientTransport, int disconnectTimeout) {
        if (clientTransport == null) {
            return;
        }
        boolean needDestroy;
        if (CHANNEL_REUSE) { // 开启长连接复用，根据连接引用数判断
            AtomicInteger integer = TRANSPORT_REF_COUNTER.get(clientTransport);
            if (integer == null) {
                needDestroy = true;
            } else {
                int currentCount = integer.decrementAndGet(); // 当前连接引用数
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Client transport {} of {} , current ref count is: {}", clientTransport,
                        NetUtils.channelToString(clientTransport.localAddress(), clientTransport.remoteAddress()),
                        currentCount);
                }
                if (currentCount <= 0) { // 此长连接无任何引用，可以销毁
                    String key = getAddr(clientTransport.getConfig());
                    CLIENT_TRANSPORT_MAP.remove(key);
                    TRANSPORT_REF_COUNTER.remove(clientTransport);
                    needDestroy = true;
                } else {
                    needDestroy = false;
                }
            }
        } else { // 未开启长连接复用，可以销毁
            ALL_TRANSPORT_MAP.remove(clientTransport.getConfig());
            needDestroy = true;
        }
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
                    LOGGER.warn("There are {} outstanding call in client transport," +
                        " and shutdown now", count);
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
     * 关闭全部客户端连接
     */
    public static void closeAll() {
        if ((CHANNEL_REUSE && CommonUtils.isEmpty(CLIENT_TRANSPORT_MAP))
            || (!CHANNEL_REUSE && CommonUtils.isEmpty(ALL_TRANSPORT_MAP))) {
            return;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Shutdown all client transport now!");
        }
        try {
            if (CHANNEL_REUSE) {
                for (Map.Entry<String, ClientTransport> entrySet : CLIENT_TRANSPORT_MAP.entrySet()) {
                    ClientTransport clientTransport = entrySet.getValue();
                    if (clientTransport.isAvailable()) {
                        clientTransport.destroy();
                    }
                }
                CLIENT_TRANSPORT_MAP.clear();
                TRANSPORT_REF_COUNTER.clear();
            } else {
                for (Map.Entry<ClientTransportConfig, ClientTransport> entrySet : ALL_TRANSPORT_MAP.entrySet()) {
                    ClientTransport clientTransport = entrySet.getValue();
                    if (clientTransport.isAvailable()) {
                        clientTransport.destroy();
                    }
                }
                ALL_TRANSPORT_MAP.clear();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 反向虚拟的长连接对象, 缓存一个长连接一个<br>
     * {"127.0.0.1:22000<->127.0.0.1:54321": ClientTransport}
     */
    private volatile static ConcurrentHashMap<String, ClientTransport> REVERSE_CLIENT_TRANSPORT_MAP = null;

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

    //    /**
    //     * 检查Future列表，删除超时请求
    //     */
    //    public static void checkFuture() {
    //        for (Map.Entry<String, ClientTransport> entrySet : connectionPool.entrySet()) {
    //            try {
    //                ClientTransport clientTransport = entrySet.getValue();
    //                if (clientTransport instanceof AbstractTCPClientTransport) {
    //                    AbstractTCPClientTransport aClientTransport = (AbstractTCPClientTransport) clientTransport;
    //                    aClientTransport.checkFutureMap();
    //                }
    //            } catch (Exception e) {
    //                logger.error(e.getMessage(), e);
    //            }
    //        }
    //    }
}
