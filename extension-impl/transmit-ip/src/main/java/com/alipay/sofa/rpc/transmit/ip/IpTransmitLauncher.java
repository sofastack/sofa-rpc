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

import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.transmit.TransmitConfig;
import com.alipay.sofa.rpc.transmit.TransmitLauncher;
import com.alipay.sofa.rpc.transmit.registry.TransmitRegistry;
import com.alipay.sofa.rpc.transmit.registry.TransmitRegistryCallback;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 级别转发加载器
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension(value = "ip")
public class IpTransmitLauncher implements TransmitLauncher {

    /**
     * 预热转发前缀
     */
    private static final String                                       TRANSMIT_ID_PREFIX = "SOFA_RPC_TRANSMIT_";

    /**
     * 是否已经启动转发
     */
    private static volatile boolean                                   transmitLaunched   = false;

    /**
     * 应用-->转发处理器
     */
    private static final ConcurrentHashMap<String, IpTransmitHandler> HANDLERS           = new ConcurrentHashMap<String, IpTransmitHandler>();

    /**
     * 私有构造函数
     */
    private IpTransmitLauncher() {

    }

    /**
     * 转发功能是否已加载，如果是的话，之后构建的服务发布者将自动加载预热转发Filter
     *
     * @return 是否已加载
     */
    public static boolean isLaunched() {
        return transmitLaunched;
    }

    /**
     * 订阅地址注册中心
     */
    private TransmitRegistry registry;

    @Override
    public void load(String appName, TransmitConfig transmitConfig) {
        if (appName == null) {
            return;
        }

        buildHandler(appName, transmitConfig);
        transmitLaunched = !HANDLERS.isEmpty();

    }

    private static IpTransmitHandler buildHandler(String appName, TransmitConfig transmitConfig) {
        IpTransmitHandler handler = HANDLERS.get(appName);
        if (handler == null) {
            if (transmitConfig == null) {
                transmitConfig = new TransmitConfig();
            }
            handler = new IpTransmitHandler(appName, transmitConfig);
            IpTransmitHandler old = HANDLERS.putIfAbsent(appName, handler);
            if (old != null) {
                handler = old;
            }
        }
        return handler;
    }

    /**
     * 应用对应的转发处理器
     *
     * @param appName 应用名
     * @return IpTransmitHandler
     */
    public static IpTransmitHandler getHandler(String appName) {
        return appName == null ? null : HANDLERS.get(appName);
    }

    @Override
    public void startTransmit(String appName) {
        if (registry == null) {
            throw new SofaRpcRuntimeException("IpTransmitRegistry is null, please set it before start transmit.");
        }
        if (appName == null) {
            return;
        }
        final IpTransmitHandler handler = getHandler(appName);
        if (handler != null) {
            handler.startTransmit();
            String uniqueId = handler.getTransmitConfig().getUniqueIdValue();
            // 生成配置中心需要的dataId
            String dataId = generateDataId(appName, uniqueId);
            // 注册当前节点到配置中心
            registry.register(appName, dataId);
            // 订阅其它转发节点
            registry.subscribe(appName, dataId, new TransmitRegistryCallback() {
                @Override
                public void handleData(String dataId, List<String> strings) {
                    handler.getAddressHolder().processSubscribe(dataId, strings);
                }

                @Override
                public void addData(String dataId, String add) {
                    handler.getAddressHolder().addAvailableAddress(add);
                }

                @Override
                public void deleteData(String dataId, String delete) {
                    handler.getAddressHolder().deleteAvailableAddress(dataId);
                }

                @Override
                public void setData(String dataId, List<String> strings) {
                    handler.setAvailableTransmitAddresses(strings);
                }
            });
        } else {
            throw new SofaRpcException(RpcErrorType.SERVER_UNDECLARED_ERROR, "Need load transmit config before start.");
        }
    }

    @Override
    public void stopTransmit(String appName) {
        if (appName == null) {
            return;
        }
        IpTransmitHandler handler = getHandler(appName);
        if (handler != null) {
            handler.stopTransmit();
        }
    }

    @Override
    public void unload(String appName) {
        if (appName == null) {
            return;
        }
        IpTransmitHandler handler = HANDLERS.remove(appName);
        if (handler != null) {
            handler.stopTransmit();
        }
        transmitLaunched = !HANDLERS.isEmpty();
    }

    /**
     * 生成DataId
     *
     * @param appName       应用名
     * @param uniqueIdValue 关键字d
     * @return dataId
     */
    String generateDataId(String appName, String uniqueIdValue) {
        String serviceName = TRANSMIT_ID_PREFIX + appName.toUpperCase();
        if (StringUtils.isNotBlank(uniqueIdValue)) {
            serviceName = serviceName + "_" + uniqueIdValue.toUpperCase();
        }

        return serviceName;
    }

    /**
     * Get IpTransmitRegistry
     *
     * @return IpTransmitRegistry
     */
    public TransmitRegistry getRegistry() {
        return registry;
    }

    /**
     * Set IpTransmitRegistry
     *
     * @param registry IpTransmitRegistry
     * @return this
     */
    @Override
    public TransmitLauncher setRegistry(TransmitRegistry registry) {
        this.registry = registry;
        return this;
    }
}
