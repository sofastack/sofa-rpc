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
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ClientTransport of same provider will be reused.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ReusableClientTransportHolder implements ClientTransportHolder {
    /**
     * slf4j Logger for this class
     */
    private final static Logger                                     LOGGER              = LoggerFactory
                                                                                            .getLogger(ReusableClientTransportHolder.class);

    /**
     * 长连接复用时，共享长连接的连接池，一个服务端ip和端口同一协议只建立一个长连接，不管多少接口，共用长连接
     */
    private final ConcurrentHashMap<String, ClientTransport>        clientTransportMap  = new ConcurrentHashMap<String, ClientTransport>();

    /**
     * 长连接复用时，共享长连接的计数器
     */
    private final ConcurrentHashMap<ClientTransport, AtomicInteger> transportRefCounter = new ConcurrentHashMap<ClientTransport, AtomicInteger>();

    /**
     * 通过配置获取长连接
     *
     * @param config 传输层配置
     * @return 传输层
     */
    @Override
    public ClientTransport getClientTransport(ClientTransportConfig config) {
        String key = getAddr(config);
        ClientTransport transport = clientTransportMap.get(key);
        if (transport == null) {
            transport = ExtensionLoaderFactory.getExtensionLoader(ClientTransport.class)
                .getExtension(config.getContainer(),
                    new Class[] { ClientTransportConfig.class },
                    new Object[] { config });
            ClientTransport oldTransport = clientTransportMap.putIfAbsent(key, transport); // 保存唯一长连接
            if (oldTransport != null) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Multiple threads init ClientTransport with same key:" + key);
                }
                transport.destroy(); //如果同时有人插入，则使用第一个
                transport = oldTransport;
            }
        }
        AtomicInteger counter = transportRefCounter.get(transport);
        if (counter == null) {
            counter = new AtomicInteger(0);
            AtomicInteger oldCounter = transportRefCounter.putIfAbsent(transport, counter);
            if (oldCounter != null) {
                counter = oldCounter;
            }
        }
        counter.incrementAndGet(); // 计数器加1
        return transport;
    }

    private static String getAddr(ClientTransportConfig config) {
        ProviderInfo providerInfo = config.getProviderInfo();
        return providerInfo.getProtocolType() + "://" + providerInfo.getHost() + ":" + providerInfo.getPort();
    }

    @Override
    public boolean removeClientTransport(ClientTransport clientTransport) {
        if (clientTransport == null) {
            return false;
        }
        boolean needDestroy;
        AtomicInteger integer = transportRefCounter.get(clientTransport);
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
                clientTransportMap.remove(key);
                transportRefCounter.remove(clientTransport);
                needDestroy = true;
            } else {
                needDestroy = false;
            }
        }
        return needDestroy;
    }

    @Override
    public int size() {
        return clientTransportMap.size();
    }

    @Override
    public void destroy() {

        for (Map.Entry<String, ClientTransport> entrySet : clientTransportMap.entrySet()) {
            ClientTransport clientTransport = entrySet.getValue();
            if (clientTransport.isAvailable()) {
                clientTransport.destroy();
            }
        }
        clientTransportMap.clear();
        transportRefCounter.clear();

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
