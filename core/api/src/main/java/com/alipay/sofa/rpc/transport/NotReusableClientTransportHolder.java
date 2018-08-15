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
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ClientTransport of same provider will not be reused.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class NotReusableClientTransportHolder implements ClientTransportHolder {
    /**
     * slf4j Logger for this class
     */
    private final static Logger                                         LOGGER        = LoggerFactory
                                                                                          .getLogger(NotReusableClientTransportHolder.class);

    /**
     * 长连接不复用的时候，一个ClientTransportConfig对应一个ClientTransport
     */
    private final ConcurrentMap<ClientTransportConfig, ClientTransport> allTransports = new ConcurrentHashMap<ClientTransportConfig, ClientTransport>();

    @Override
    public ClientTransport getClientTransport(ClientTransportConfig config) {

        ClientTransport transport = allTransports.get(config);
        if (transport == null) {
            transport = ExtensionLoaderFactory.getExtensionLoader(ClientTransport.class)
                .getExtension(config.getContainer(),
                    new Class[] { ClientTransportConfig.class },
                    new Object[] { config });
            ClientTransport old = allTransports.putIfAbsent(config, transport); // 保存唯一长连接
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

    @Override
    public boolean removeClientTransport(ClientTransport clientTransport) {
        if (clientTransport == null) {
            return false;
        }
        // 未开启长连接复用，可以销毁
        allTransports.remove(clientTransport.getConfig());
        return true;
    }

    @Override
    public int size() {
        return allTransports.size();
    }

    @Override
    public void destroy() {
        for (Map.Entry<ClientTransportConfig, ClientTransport> entrySet : allTransports.entrySet()) {
            ClientTransport clientTransport = entrySet.getValue();
            if (clientTransport.isAvailable()) {
                clientTransport.destroy();
            }
        }
        allTransports.clear();
    }

    @Override
    public void destroy(DestroyHook hook) {
        if (hook != null) {
            hook.preDestroy();
        }
        destroy();
        if (hook != null) {
            hook.postDestroy();
        }
    }
}
