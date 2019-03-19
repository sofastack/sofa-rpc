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
package com.alipay.sofa.rpc.transport.bolt;

import com.alipay.remoting.Connection;
import com.alipay.remoting.Url;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.sofa.rpc.common.annotation.VisibleForTesting;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
class ReuseBoltClientConnectionManager extends BoltClientConnectionManager {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                            LOGGER               = LoggerFactory
                                                                                    .getLogger(ReuseBoltClientConnectionManager.class);

    /**
     * 长连接复用时，共享长连接的连接池，一个服务端ip和端口同一协议只建立一个长连接，不管多少接口，共用长连接
     */
    @VisibleForTesting
    final ConcurrentMap<ClientTransportConfig, Connection> urlConnectionMap     = new ConcurrentHashMap<ClientTransportConfig, Connection>();

    /**
     * 长连接复用时，共享长连接的计数器
     */
    @VisibleForTesting
    final ConcurrentMap<Connection, AtomicInteger>         connectionRefCounter = new ConcurrentHashMap<Connection, AtomicInteger>();

    public ReuseBoltClientConnectionManager(boolean addHook) {
        super(addHook);
    }

    /**
     * 检查是否有没回收
     */
    protected void checkLeak() {
        if (CommonUtils.isNotEmpty(urlConnectionMap)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Bolt client transport maybe leak. {}", urlConnectionMap);
            }
            urlConnectionMap.clear();
        }
        if (CommonUtils.isNotEmpty(connectionRefCounter)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Bolt client transport maybe leak. {}", connectionRefCounter);
            }
            connectionRefCounter.clear();
        }
    }

    /**
     * 通过配置获取长连接
     *
     * @param rpcClient       bolt客户端
     * @param transportConfig 传输层配置
     * @param url             传输层地址
     * @return 长连接
     */
    @Override
    public Connection getConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url) {
        if (rpcClient == null || transportConfig == null || url == null) {
            return null;
        }
        Connection connection = urlConnectionMap.get(transportConfig);
        if (connection != null && !connection.isFine()) {
            closeConnection(rpcClient, transportConfig, url);
            connection = null;
        }
        if (connection == null) {
            try {
                connection = rpcClient.getConnection(url, url.getConnectTimeout());
            } catch (Exception e) {
                LOGGER.warn("get connection failed in url," + url);
            }
            if (connection == null) {
                return null;
            }
            // 保存唯一长连接
            Connection oldConnection = urlConnectionMap.putIfAbsent(transportConfig, connection);
            if (oldConnection != null) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Multiple threads init ClientTransport with same key:" + url);
                }
                //only if new connection is not equals old connection,we can close it
                if (connection != oldConnection) {
                    rpcClient.closeStandaloneConnection(connection); //如果同时有人插入，则使用第一个
                    connection = oldConnection;
                }
            } else {

                // 增加计数器
                AtomicInteger counter = connectionRefCounter.get(connection);
                if (counter == null) {
                    counter = new AtomicInteger(0);
                    AtomicInteger oldCounter = connectionRefCounter.putIfAbsent(connection, counter);
                    if (oldCounter != null) {
                        counter = oldCounter;
                    }
                }
                int currentCount = counter.incrementAndGet();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Bolt client transport {} of {}, current ref count is: {}", url.toString(),
                        NetUtils.channelToString(connection.getLocalAddress(), connection.getRemoteAddress()),
                        currentCount);
                }
            }
        }
        return connection;
    }

    /**
     * 关闭长连接
     *
     * @param rpcClient       bolt客户端
     * @param transportConfig 传输层配置
     * @param url             传输层地址
     */
    @Override
    public void closeConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url) {
        if (rpcClient == null || transportConfig == null || url == null) {
            return;
        }
        // 先删除
        Connection connection = urlConnectionMap.remove(transportConfig);
        if (connection == null) {
            return;
        }
        // 再判断是否需要关闭
        boolean needDestroy;
        AtomicInteger integer = connectionRefCounter.get(connection);
        if (integer == null) {
            needDestroy = true;
        } else {
            // 当前连接引用数
            int currentCount = integer.decrementAndGet();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client transport {} of {} , current ref count is: {}", url.toString(),
                    NetUtils.channelToString(connection.getLocalAddress(), connection.getRemoteAddress()),
                    currentCount);
            }
            if (currentCount <= 0) {
                // 此长连接无任何引用，可以销毁
                connectionRefCounter.remove(connection);
                needDestroy = true;
            } else {
                needDestroy = false;
            }
        }
        if (needDestroy) {
            rpcClient.closeStandaloneConnection(connection);
        }
    }

    @Override
    public boolean isConnectionFine(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url) {
        Connection connection = this.getConnection(rpcClient, transportConfig, url);
        return connection != null && connection.isFine();
    }
}
