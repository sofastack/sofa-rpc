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
package com.alipay.sofa.rpc.transmit.ip;

import com.alipay.sofa.rpc.base.Destroyable;
import com.alipay.sofa.rpc.client.ExcludeRouter;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.context.RpcRuntimeContext;
import com.alipay.sofa.rpc.router.IpTransmitRouter;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预热转发连接池。
 * 预热转发连接抽象为一个服务连接。
 * 屏蔽预热转发连接层与其它服务连接层的影响。
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class IpTransmitClientFactory {

    /**
     * 转发客户端缓存 {服务:转发客户端}
     */
    private final static ConcurrentHashMap<String, IpTransmitClient> SERVICE_TRANSMIT_CLIENTS = new ConcurrentHashMap<String, IpTransmitClient>();

    static {
        RpcRuntimeContext.registryDestroyHook(new Destroyable.DestroyHook() {
            @Override
            public void preDestroy() {
                destroyAll();
            }

            @Override
            public void postDestroy() {
            }
        });
    }

    /**
     * 获取转发客户端
     *
     * @param config 转发客户端配置
     * @return 转发客户端
     */
    public static IpTransmitClient getTransmitClient(IpTransmitClientConfig config) {
        // 获取客户端
        String serviceKey = buildKey(config);
        IpTransmitClient client = SERVICE_TRANSMIT_CLIENTS.get(serviceKey);
        if (client == null) {
            client = initTransmitClient(config);
            IpTransmitClient oldClient = SERVICE_TRANSMIT_CLIENTS.putIfAbsent(serviceKey, client);
            if (oldClient != null) {
                client = oldClient;
            }
            // 初始化下
            client.init();
        }
        return client;
    }

    private static String buildKey(IpTransmitClientConfig clientConfig) {
        return clientConfig.getAppName() + ":" + clientConfig.getInterfaceId() + ":" + clientConfig.getUniqueId();
    }

    /**
     * 初始化转发客户端
     *
     * @param config 转发客户端配置
     * @return 转发客户端对象（未初始化）
     */
    private static IpTransmitClient initTransmitClient(IpTransmitClientConfig config) {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setAppName(config.getAppName());
        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setId("transmit-" + consumerConfig.getId());
        consumerConfig.setApplication(applicationConfig);
        consumerConfig.setInterfaceId(config.getInterfaceId());
        consumerConfig.setRegister(false);
        consumerConfig.setSubscribe(false);
        consumerConfig.setCluster("failover");
        consumerConfig.setRetries(0);
        consumerConfig.setRouterRef(Arrays.asList(new ExcludeRouter("-*"), new IpTransmitRouter()));
        consumerConfig.setRepeatedReferLimit(-1);
        consumerConfig.setUniqueId(config.getUniqueId());

        return new IpTransmitClient(consumerConfig, config.getPort());
    }

    /**
     * 全部客户端删除某个地址
     *
     * @param ip 删除的IP
     */
    public static void removeConnectionByIp(String ip) {
        for (Map.Entry<String, IpTransmitClient> entry : SERVICE_TRANSMIT_CLIENTS.entrySet()) {
            entry.getValue().removeConnectionByIp(ip);
        }
    }

    /**
     * 全部客户端增加某个地址
     *
     * @param ip 增加的IP
     */
    public static void addConnectionByIp(String ip) {
        for (Map.Entry<String, IpTransmitClient> entry : SERVICE_TRANSMIT_CLIENTS.entrySet()) {
            entry.getValue().addConnectionByIp(ip);
        }
    }

    /**
     * 销毁全部客户端
     */
    public static void destroyAll() {
        for (Map.Entry<String, IpTransmitClient> entry : SERVICE_TRANSMIT_CLIENTS.entrySet()) {
            try {
                entry.getValue().destroy();
            } catch (Exception ignore) { // NOPMD
            }
        }
    }

    /**
     * 销毁指定应用客户端
     */
    public static void destroyByApp(String appName) {
        String prefix = appName + ":";
        for (Map.Entry<String, IpTransmitClient> entry : SERVICE_TRANSMIT_CLIENTS.entrySet()) {
            try {
                String key = entry.getKey();
                if (key.startsWith(prefix)) {
                    entry.getValue().destroy();
                    SERVICE_TRANSMIT_CLIENTS.remove(entry.getKey());
                }
            } catch (Exception ignore) { // NOPMD
            }
        }
    }
}