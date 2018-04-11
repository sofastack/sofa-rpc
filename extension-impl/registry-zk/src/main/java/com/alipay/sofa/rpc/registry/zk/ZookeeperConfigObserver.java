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
package com.alipay.sofa.rpc.registry.zk;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.AbstractInterfaceConfig;
import com.alipay.sofa.rpc.listener.ConfigListener;
import org.apache.curator.framework.recipes.cache.ChildData;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZookeeperObserver for config node.
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ZookeeperConfigObserver extends AbstractZookeeperObserver {

    /**
     * slf4j Logger for this class
     */
    private final static Logger                                              LOGGER            = LoggerFactory
                                                                                                   .getLogger(ZookeeperConfigObserver.class);

    /**
     * The Config listener map.
     */
    private ConcurrentHashMap<AbstractInterfaceConfig, List<ConfigListener>> configListenerMap = new ConcurrentHashMap<AbstractInterfaceConfig, List<ConfigListener>>();

    /**
     * 该接口下增加了一个配置
     *
     * @param config 接口名称
     * @param data        配置
     */
    public void addConfig(AbstractInterfaceConfig config, ChildData data) {
        // TODO
        if (data == null) {
            if (LOGGER.isInfoEnabled(config.getAppName())) {
                LOGGER.info("data is null");
            }
        } else {
            if (LOGGER.isInfoEnabled(config.getAppName())) {
                LOGGER.info("Receive data: path=[" + data.getPath() + "]"
                    + ", data=[" + new String(data.getData(), RpcConstants.DEFAULT_CHARSET) + "]"
                    + ", stat=[" + data.getStat() + "]");
            }
        }
    }

    public void removeConfig(AbstractInterfaceConfig config, ChildData data) {
        // TODO
        if (data == null) {
            if (LOGGER.isInfoEnabled(config.getAppName())) {
                LOGGER.info("data is null");
            }
        } else {
            if (LOGGER.isInfoEnabled(config.getAppName())) {
                LOGGER.info("Receive data: path=[" + data.getPath() + "]"
                    + ", data=[" + new String(data.getData(), RpcConstants.DEFAULT_CHARSET) + "]"
                    + ", stat=[" + data.getStat() + "]");
            }
        }
    }

    public void updateConfig(AbstractInterfaceConfig config, ChildData data) {
        // TODO
        if (data == null) {
            if (LOGGER.isInfoEnabled(config.getAppName())) {
                LOGGER.info("data is null");
            }
        } else {
            if (LOGGER.isInfoEnabled(config.getAppName())) {
                LOGGER.info("Receive data: path=[" + data.getPath() + "]"
                    + ", data=[" + new String(data.getData(), RpcConstants.DEFAULT_CHARSET) + "]"
                    + ", stat=[" + data.getStat() + "]");
            }
        }
    }

    public void updateConfigAll(AbstractInterfaceConfig config, List<ChildData> currentData) {

    }

    /**
     * Add config listener.
     *
     * @param config   the config
     * @param listener the listener
     */
    public void addConfigListener(AbstractInterfaceConfig config, ConfigListener listener) {
        if (listener != null) {
            initOrAddList(configListenerMap, config, listener);
        }
    }

    /**
     * Remove config listener.
     *
     * @param config the config
     */
    public void removeConfigListener(AbstractInterfaceConfig config) {
        configListenerMap.remove(config);
    }
}
