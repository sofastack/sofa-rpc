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
package com.alipay.sofa.rpc.transmit.registry.impl;

import com.alipay.sofa.rpc.common.SystemInfo;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.RegistryConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transmit.registry.TransmitRegistry;
import com.alipay.sofa.rpc.transmit.registry.TransmitRegistryCallback;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;

import static com.alipay.sofa.rpc.common.utils.StringUtils.CONTEXT_SEP;

/**
 * Data structures on zookeeper：
 *
 * *  -$rootPath
 *         └--sofa-rpc-transmit-ip
 *             |--SOFA_RPC_TRANSMIT_APP_UNIQUE（transmit id）
 *             |       |
 *             |       |
 *             |       |-192.168.1.100 （transmit ip）
 *             |       |
 *             |       |-192.168.2.100 （transmit ip2）
 *             |       |
 *             |       └-......
 *             |
 *             |
 *             |---SOFA_RPC_TRANSMIT_APP2_UNIQUE2 （transmit id2）
 *             | ......
 *  </pre>
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 */
@Extension(value = "zookeeper")
public class ZookeeperIpTransmitRegistry implements TransmitRegistry {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER                 = LoggerFactory.getLogger(ZookeeperIpTransmitRegistry.class);

    public static final String  SOFA_RPC_TRANSMIT_IP   = "sofa-rpc-transmit-ip";

    private RegistryConfig      registryConfig;

    private CuratorFramework    zkClient;

    private String              rootPath;

    public final static String  PARAM_CREATE_EPHEMERAL = "createEphemeral";

    private boolean             ephemeralNode          = true;

    @Override
    public synchronized void init(RegistryConfig registryConfig) {
        if (zkClient != null) {
            return;
        }
        this.registryConfig = registryConfig;

        String addressInput = registryConfig.getAddress(); // xxx:2181,yyy:2181/path1/paht2
        int idx = addressInput.indexOf(CONTEXT_SEP);
        String address; // IP地址
        if (idx > 0) {
            address = addressInput.substring(0, idx);
            rootPath = addressInput.substring(idx);
            if (!rootPath.endsWith(CONTEXT_SEP)) {
                rootPath += CONTEXT_SEP; // 保证以"/"结尾
            }
        } else {
            address = addressInput;
            rootPath = CONTEXT_SEP;
        }
        ephemeralNode = !CommonUtils.isFalse(registryConfig.getParameter(PARAM_CREATE_EPHEMERAL));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "Init ZookeeperIpTransmitRegistry with address {}, root path is {}. ephemeralNode:{}",
                address, rootPath, ephemeralNode);
        }
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        zkClient = CuratorFrameworkFactory.builder()
            .connectString(address)
            .sessionTimeoutMs(registryConfig.getConnectTimeout() * 3)
            .connectionTimeoutMs(registryConfig.getConnectTimeout())
            .canBeReadOnly(false)
            .retryPolicy(retryPolicy)
            .defaultData(null)
            .build();

        start();
    }

    public boolean start() {
        if (zkClient == null) {
            LOGGER.warn("Start ZookeeperIpTransmitRegistry registry must be do init first!");
            return false;
        }
        if (zkClient.getState() == CuratorFrameworkState.STARTED) {
            return true;
        }
        try {
            zkClient.start();
        } catch (Exception e) {
            throw new SofaRpcRuntimeException("Failed to start ZookeeperIpTransmitRegistry zkClient", e);
        }
        return zkClient.getState() == CuratorFrameworkState.STARTED;
    }

    @Override
    public void register(String appName, String dataId) {
        if (registryConfig.isRegister()) {
            try {
                String transmitIpPath = createTransmitIpPath(dataId);

                getAndCheckZkClient()
                    .create()
                    .creatingParentContainersIfNeeded()
                    .withMode(ephemeralNode ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT)
                    .forPath(transmitIpPath);
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to register transmit ip to ZookeeperIpTransmitRegistry!", e);
            }
        }

    }

    @Override
    public void subscribe(String appName, final String dataId, final TransmitRegistryCallback callback) {
        if (registryConfig.isSubscribe()) {
            try {

                final String transmitDataIdPath = createTransmitDataIdPath(dataId);

                List<String> alreayIps = zkClient.getChildren().forPath(transmitDataIdPath);
                callback.setData(dataId, alreayIps);

                PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, transmitDataIdPath, true);
                pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
                    @Override
                    public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event)
                        throws Exception {

                        String ip = getIp(transmitDataIdPath, event.getData().getPath());

                        switch (event.getType()) {
                            case CHILD_ADDED:
                                callback.addData(dataId, ip);
                                break;
                            case CHILD_REMOVED:
                                callback.deleteData(dataId, ip);
                                break;
                            default:
                                break;
                        }

                    }
                });

                pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            } catch (Exception e) {
                throw new SofaRpcRuntimeException("Failed to subscribe transmit ip from ZookeeperIpTransmitRegistry!",
                    e);
            }
        }
    }

    private String createTransmitIpPath(String dataId) {

        return rootPath + SOFA_RPC_TRANSMIT_IP + CONTEXT_SEP + dataId + CONTEXT_SEP + SystemInfo.getLocalHost();
    }

    private String createTransmitDataIdPath(String dataId) {
        return rootPath + SOFA_RPC_TRANSMIT_IP + CONTEXT_SEP + dataId;

    }

    private CuratorFramework getAndCheckZkClient() {
        if (zkClient == null || zkClient.getState() != CuratorFrameworkState.STARTED) {
            throw new SofaRpcRuntimeException("Zookeeper client is not available");
        }
        return zkClient;
    }

    private String getIp(String transmitDataIdPath, String ipPath) {

        String ip = ipPath.substring(transmitDataIdPath.length() + 1);

        return ip;
    }

    @Override
    public void destroy() {
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            zkClient.close();
        }
    }
}