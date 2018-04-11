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

import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>服务端通讯层工厂类</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ServerTransportFactory {

    /**
     * 保留了 端口 和 服务通讯层
     */
    public static final Map<String, ServerTransport> SERVER_TRANSPORT_MAP = new ConcurrentHashMap<String, ServerTransport>();

    /**
     * Get ServerTransport
     *
     * @param serverConfig ServerTransportConfig
     * @return ServerTransport
     */
    public static ServerTransport getServerTransport(ServerTransportConfig serverConfig) {
        ServerTransport serverTransport = ExtensionLoaderFactory.getExtensionLoader(ServerTransport.class)
            .getExtension(serverConfig.getContainer(),
                new Class[] { ServerTransportConfig.class },
                new Object[] { serverConfig });
        if (serverTransport != null) {
            String key = Integer.toString(serverConfig.getPort());
            SERVER_TRANSPORT_MAP.put(key, serverTransport);
        }
        return serverTransport;
    }
}
