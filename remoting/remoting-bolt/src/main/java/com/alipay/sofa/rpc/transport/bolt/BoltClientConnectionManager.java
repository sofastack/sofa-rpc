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
import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.common.annotation.VisibleForTesting;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.transport.ClientTransportConfig;

/**
 * @author bystander
 * @version $Id: BoltClientConnectionManager.java, v 0.1 2019年01月29日 11:58 bystander Exp $
 */
public abstract class BoltClientConnectionManager {

    @VisibleForTesting
    public BoltClientConnectionManager(boolean addHook) {
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
    protected abstract void checkLeak();

    /**
     * get connection
     * @param rpcClient
     * @param transportConfig
     * @param url
     * @return the connection or null
     */
    public abstract Connection getConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url);

    /**
     * close connection
     * @param rpcClient
     * @param transportConfig
     * @param url
     */
    public abstract void closeConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url);

    /**
     * judge connection status
     * @param rpcClient
     * @param transportConfig
     * @param url
     * @return true /false
     */
    public abstract boolean isConnectionFine(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url);

}