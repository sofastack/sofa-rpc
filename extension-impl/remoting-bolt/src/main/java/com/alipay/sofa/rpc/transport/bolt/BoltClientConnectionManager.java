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
import com.alipay.sofa.rpc.transport.ClientTransportConfig;

/**
 * @author bystander
 * @version $Id: BoltClientConnectionManager.java, v 0.1 2019年01月29日 11:58 bystander Exp $
 */
public interface BoltClientConnectionManager {

    /**
     * get connection
     * @param rpcClient
     * @param transportConfig
     * @param url
     * @return
     */
    public Connection getConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url);

    /**
     * close connection
     * @param rpcClient
     * @param transportConfig
     * @param url
     */
    public void closeConnection(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url);

    /**
     * judge connection status
     * @param rpcClient
     * @param transportConfig
     * @param url
     * @return
     */
    public boolean isConnectionFine(RpcClient rpcClient, ClientTransportConfig transportConfig, Url url);

}