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
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.common.annotation.VisibleForTesting;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
class AloneBoltClientConnectionManager implements BoltClientConnectionManager {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory
                                           .getLogger(AloneBoltClientConnectionManager.class);

    @VisibleForTesting
    protected AloneBoltClientConnectionManager(boolean addHook) {
        if (addHook) {
            RpcRuntimeContext.registryDestroyHook(new Destroyable.DestroyHook() {
                @Override
                public void preDestroy() {

                }

                @Override
                public void postDestroy() {
                    checkLeak();
                }
            });
        }
    }

    /**
     * 检查是否有没回收
     */
    protected void checkLeak() {

    }

    /**
     * 通过配置获取长连接
     *
     * @param rpcClient       bolt客户端
     * @param transportConfig 传输层配置
     * @param url             传输层地址
     * @return 长连接
     */
    public Connection getConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url) {
        if (rpcClient == null || transportConfig == null || url == null) {
            return null;
        }
        Connection connection;
        try {
            connection = rpcClient.getConnection(url, url.getConnectTimeout());
        } catch (InterruptedException e) {
            throw new SofaRpcRuntimeException(e);
        } catch (RemotingException e) {
            throw new SofaRpcRuntimeException(e);
        }
        if (connection == null) {
            return null;
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
    public void closeConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url) {
        if (rpcClient == null || transportConfig == null || url == null) {
            return;
        }
        //TODO do not close
    }

    @Override
    public boolean isConnectionFine(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url) {
        Connection connection;
        try {
            connection = rpcClient.getConnection(url, url.getConnectTimeout());
        } catch (RemotingException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        return connection != null && connection.isFine();
    }
}
