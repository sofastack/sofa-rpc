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
package com.alipay.sofa.rpc.registry.multicast;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderHelper;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.common.struct.NamedThreadFactory;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.event.ConsumerSubEvent;
import com.alipay.sofa.rpc.event.EventBus;
import com.alipay.sofa.rpc.event.ProviderPubEvent;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.listener.ProviderInfoListener;
import com.alipay.sofa.rpc.log.LogCodes;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.registry.Registry;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaowang
 * @version : MultiCastRegistry.java, v 0.1 2020年03月04日 2:14 下午 zhaowang Exp $
 */
@Extension("multicast")
public class MulticastRegistry extends Registry {

    private final static Logger LOGGER = LoggerFactory.getLogger(MulticastRegistry.class);

    private static final int DEFAULT_MULTICAST_PORT = 1234;
    private static final String EXT_NAME = "multicast";
    private static final String SPACE = " ";
    private static final String REGISTER = "register";
    private static final String UNREGISTER = "unregister";
    private static final String SUBSCRIBE = "subscribe";
    private static final String UNSUBSCRIBE = "unsubscribe";
    private static final String CLEAN_PERIOD = "cleanPeriod";
    private static final String CLEAN = "clean";

    private InetAddress multicastAddress;

    private MulticastSocket multicastSocket;

    private int multicastPort;

    /**
     * 内存里的服务列表 {service : [provider...]}
     */
    protected Map<String, ProviderGroup> allProviderCache = new ConcurrentHashMap<>();

    /**
     * 订阅者通知列表（key为订阅者关键字，value为ConsumerConfig列表）
     */
    protected Map<String, List<ConsumerConfig>> notifyListeners = new ConcurrentHashMap<>();

    protected Map<String, ProviderGroup> registeredCache = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("SofaMulticastRegistryCleanTimer", true));

    private ScheduledFuture<?> cleanFuture;

    private int cleanPeriod = 60 * 1000;


    /**
     * 注册中心配置
     *
     * @param registryConfig 注册中心配置
     */
    protected MulticastRegistry(RegistryConfig registryConfig) {
        super(registryConfig);
    }

    @Override
    public boolean start() {
        if (multicastSocket == null) {
            LOGGER.warn("Please invoke MulticastRegistry.init() first!");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void register(ProviderConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isRegister()) {
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }
        if (config.isRegister()) {
            List<ServerConfig> serverConfigs = config.getServer();
            if (CommonUtils.isNotEmpty(serverConfigs)) {
                for (ServerConfig server : serverConfigs) {
                    String serviceName = MulticastRegistryHelper.buildListDataId(config, server.getProtocol());
                    ProviderInfo providerInfo = MulticastRegistryHelper.convertProviderToProviderInfo(config, server);
                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_START, serviceName));
                    }
                    doRegister(appName, serviceName, providerInfo);

                    if (LOGGER.isInfoEnabled(appName)) {
                        LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB_OVER, serviceName));
                    }
                }
                if (EventBus.isEnable(ProviderPubEvent.class)) {
                    ProviderPubEvent event = new ProviderPubEvent(config);
                    EventBus.post(event);
                }

            }
        }

    }


    @Override
    public void unRegister(ProviderConfig config) {
        String appName = config.getAppName();
        if (!registryConfig.isRegister()) { // 注册中心不注册
            if (LOGGER.isInfoEnabled(appName)) {
                LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_REGISTRY_IGNORE));
            }
            return;
        }
        if (config.isRegister()) { // 服务不注册
            List<ServerConfig> serverConfigs = config.getServer();
            if (CommonUtils.isNotEmpty(serverConfigs)) {
                for (ServerConfig server : serverConfigs) {
                    String serviceName = MulticastRegistryHelper.buildListDataId(config, server.getProtocol());
                    ProviderInfo providerInfo = MulticastRegistryHelper.convertProviderToProviderInfo(config, server);
                    try {
                        doUnRegister(serviceName, providerInfo);
                        if (LOGGER.isInfoEnabled(appName)) {
                            LOGGER.infoWithApp(appName,
                                    LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB, serviceName, "1"));
                        }
                    } catch (Exception e) {
                        LOGGER.errorWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_UNPUB, serviceName, "0"),
                                e);
                    }
                }
            }
        }

    }


    @Override
    public void batchUnRegister(List<ProviderConfig> configs) {
        for (ProviderConfig config : configs) {
            String appName = config.getAppName();
            try {
                unRegister(config);
            } catch (Exception e) {
                LOGGER.errorWithApp(appName, "Error when batch unregistry", e);
            }
        }
    }

    @Override
    public List<ProviderGroup> subscribe(ConsumerConfig config) {
        if (!config.isSubscribe()) {
            return null;
        }

        String key = MulticastRegistryHelper.buildListDataId(config, config.getProtocol());
        List<ConsumerConfig> listeners = notifyListeners.get(key);
        if (listeners == null) {
            listeners = new ArrayList<ConsumerConfig>();
            notifyListeners.put(key, listeners);
        }
        listeners.add(config);
        multicast(SUBSCRIBE + key);

        ProviderGroup group = allProviderCache.get(key);
        if (group == null) {
            group = new ProviderGroup();
            allProviderCache.put(key, group);
        }

        if (EventBus.isEnable(ConsumerSubEvent.class)) {
            ConsumerSubEvent event = new ConsumerSubEvent(config);
            EventBus.post(event);
        }

        return Collections.singletonList(group);
    }

    @Override
    public void unSubscribe(ConsumerConfig config) {
        if (!config.isSubscribe()) {
            return;
        }
        String key = MulticastRegistryHelper.buildListDataId(config, config.getProtocol());
        notifyListeners.remove(key);
        // it is not necessary to multicast UNSUBSCRIBE info
    }

    @Override
    public void batchUnSubscribe(List<ConsumerConfig> configs) {
        for (ConsumerConfig config : configs) {
            String appName = config.getAppName();
            try {
                unSubscribe(config);
            } catch (Exception e) {
                LOGGER.errorWithApp(appName, "Error when batch unSubscribe", e);
            }
        }
    }

    @Override
    public void destroy() {
        try {
            multicastSocket.leaveGroup(multicastAddress);
            multicastSocket.close();
            multicastSocket = null;

            if (cleanFuture != null && !cleanFuture.isCancelled()) {
                cleanFuture.cancel(true);
            }

        } catch (Throwable t) {
            LOGGER.warn(t.getMessage(), t);
        }

    }

    @Override
    public void init() {
        if (multicastSocket != null) {
            return;
        }

        String addressInput = registryConfig.getAddress();
        if (StringUtils.isEmpty(addressInput)) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_EMPTY_ADDRESS, EXT_NAME));
        }

        try {
            if (!addressInput.startsWith(EXT_NAME)) {
                addressInput = EXT_NAME + "://" + addressInput;
            }
            URI url = new URI(addressInput);
            multicastPort = url.getPort();
            if (multicastPort <= 0) {
                multicastPort = DEFAULT_MULTICAST_PORT;
            }
            multicastAddress = InetAddress.getByName(url.getHost());
            MulticastRegistryHelper.checkMulticastAddress(multicastAddress);
            multicastSocket = new MulticastSocket(multicastPort);
            NetUtils.joinMulticastGroup(multicastSocket, multicastAddress);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buf = new byte[2048];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    while (!multicastSocket.isClosed()) {
                        try {
                            multicastSocket.receive(recv);
                            String msg = new String(recv.getData()).trim();
                            int i = msg.indexOf('\n');
                            if (i > 0) {
                                msg = msg.substring(0, i).trim();
                            }
                            MulticastRegistry.this.receive(msg, (InetSocketAddress) recv.getSocketAddress());
                            Arrays.fill(buf, (byte) 0);
                        } catch (Throwable e) {
                            if (!multicastSocket.isClosed()) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            }, "SofaMulticastRegistryReceiver");
            thread.setDaemon(true);
            thread.start();
        } catch (Exception e) {
            throw new SofaRpcRuntimeException(LogCodes.getLog(LogCodes.ERROR_REGISTRY_INIT, EXT_NAME), e);
        }


        String cleanPeriodStr = registryConfig.getParameter(CLEAN_PERIOD);
        if (StringUtils.isNotBlank(cleanPeriodStr)) {
            this.cleanPeriod = Integer.parseInt(cleanPeriodStr);
        }
        if (!Boolean.FALSE.toString().equalsIgnoreCase(registryConfig.getParameter(CLEAN))) {
            this.cleanFuture = cleanExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        clean(); // Remove the expired
                    } catch (Throwable t) { // Defensive fault tolerance
                        LOGGER.error("Unexpected exception occur at clean expired provider, cause: " + t.getMessage(), t);
                    }
                }
            }, cleanPeriod, cleanPeriod, TimeUnit.MILLISECONDS);
        } else {
            this.cleanFuture = null;
        }
    }

    private void clean() {
        Map<String, ProviderGroup> allProviderCache = this.allProviderCache;
        for (Map.Entry<String, ProviderGroup> entry : allProviderCache.entrySet()) {
            List<ProviderInfo> providerInfos = entry.getValue().getProviderInfos();
            if (CommonUtils.isNotEmpty(providerInfos)) {
                for (ProviderInfo providerInfo : providerInfos) {
                    if (isExpired(providerInfo)) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Clean expired provider " + providerInfos);
                        }
                        doUnRegister(entry.getKey(), providerInfo);
                    }
                }

            }
        }
    }

    private boolean isExpired(ProviderInfo providerInfo) {
        try (Socket socket = new Socket(providerInfo.getHost(), providerInfo.getPort())) {
        } catch (Throwable e) {
            try {
                Thread.sleep(100);
            } catch (Throwable e2) {
            }
            try (Socket socket2 = new Socket(providerInfo.getHost(), providerInfo.getPort())) {
            } catch (Throwable e2) {
                return true;
            }
        }
        return false;
    }

    private void receive(String msg, InetSocketAddress remoteAddress) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Receive multicast message: " + msg + " from " + remoteAddress);
        }
        if (msg.startsWith(REGISTER)) {
            String[] split = msg.split(SPACE);
            if (split.length != 3) {
                LOGGER.error("Receive error REGISTER :" + msg);
                return;
            }
            String serviceName = split[1];
            String providerInfoUrl = split[2];
            receiveRegister(serviceName, providerInfoUrl);
        } else if (msg.startsWith(UNREGISTER)) {
            String[] split = msg.split(SPACE);
            if (split.length != 3) {
                LOGGER.error("Receive error REGISTER :" + msg);
                return;
            }
            String serviceName = split[1];
            String providerInfoUrl = split[2];
            receiveUnregistered(serviceName, providerInfoUrl);
        } else if (msg.startsWith(SUBSCRIBE)) {
            String serviceName = msg.substring(SUBSCRIBE.length()).trim();
            ProviderGroup providerGroup = registeredCache.get(serviceName);
            if (providerGroup != null && !providerGroup.isEmpty()) {
                for (ProviderInfo providerInfo : providerGroup.getProviderInfos()) {
                    String providerInfoUrl = ProviderHelper.toUrl(providerInfo);
                    String regStr = StringUtils.join(new String[]{REGISTER, serviceName, providerInfoUrl}, SPACE);
                    multicast(regStr);
                }
            }
        }

    }


    private void receiveRegister(String serviceName, String providerInfoUrl) {
        ProviderInfo providerInfo = ProviderHelper.toProviderInfo(providerInfoUrl);
        ProviderGroup providerGroup = addToCache(serviceName, providerInfo, allProviderCache);
        notifyConsumerListeners(serviceName, providerGroup);
    }

    private void receiveUnregistered(String serviceName, String providerInfoUrl) {
        ProviderInfo providerInfo = ProviderHelper.toProviderInfo(providerInfoUrl);
        removeFromCache(serviceName, providerInfo, allProviderCache);
    }

    private ProviderGroup removeFromCache(String serviceName, ProviderInfo providerInfo, Map<String, ProviderGroup> cache) {
        ProviderGroup oldGroup = cache.get(serviceName);
        if (oldGroup != null) { // 存在老的key
            oldGroup.remove(providerInfo);
        }
        return oldGroup;
    }


    private void multicast(String msg) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Send multicast message: " + msg + " to " + multicastAddress + ":" + multicastPort);
        }
        try {
            byte[] data = (msg + "\n").getBytes();
            DatagramPacket hi = new DatagramPacket(data, data.length, multicastAddress, multicastPort);
            multicastSocket.send(hi);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    private void doRegister(String appName, String serviceName, ProviderInfo providerInfo) {
        if (LOGGER.isInfoEnabled(appName)) {
            LOGGER.infoWithApp(appName, LogCodes.getLog(LogCodes.INFO_ROUTE_REGISTRY_PUB, serviceName));
        }
        addToCache(serviceName, providerInfo, allProviderCache);
        String regStr = StringUtils.join(new String[]{REGISTER, serviceName, ProviderHelper.toUrl(providerInfo)}, SPACE);
        multicast(regStr);
        addToCache(serviceName, providerInfo, registeredCache);

        notifyConsumerListeners(serviceName, allProviderCache.get(serviceName));
    }

    private ProviderGroup addToCache(String serviceName, ProviderInfo providerInfo, Map<String, ProviderGroup> cache) {
        ProviderGroup oldGroup = cache.get(serviceName);
        if (oldGroup != null) { // 存在老的key
            oldGroup.add(providerInfo);
        } else { // 没有老的key，第一次加入
            List<ProviderInfo> news = new ArrayList<ProviderInfo>();
            news.add(providerInfo);
            cache.put(serviceName, new ProviderGroup(news));
        }
        return oldGroup;
    }


    private void doUnRegister(String serviceName, ProviderInfo providerInfo) {
        removeFromCache(serviceName, providerInfo, allProviderCache);
        removeFromCache(serviceName, providerInfo, registeredCache);

        String unregStr = StringUtils.join(new String[]{UNREGISTER, serviceName, ProviderHelper.toUrl(providerInfo)}, SPACE);
        multicast(unregStr);

        notifyConsumerListeners(serviceName, allProviderCache.get(serviceName));
    }


    private void notifyConsumerListeners(String serviceName, ProviderGroup providerGroup) {
        List<ConsumerConfig> consumerConfigs = notifyListeners.get(serviceName);
        if (consumerConfigs != null) {
            for (ConsumerConfig config : consumerConfigs) {
                ProviderInfoListener listener = config.getProviderInfoListener();
                if (listener != null) {
                    listener.updateProviders(providerGroup); // 更新分组
                }
            }
        }
    }

    public Map<String, ProviderGroup> getAllProviderCache() {
        return allProviderCache;
    }
}