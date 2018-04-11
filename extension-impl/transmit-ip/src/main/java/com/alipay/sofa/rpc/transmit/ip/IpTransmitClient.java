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

import com.alipay.sofa.rpc.client.Cluster;
import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.ProviderInfoAttrs;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.invoke.Invoker;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.proxy.ProxyFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 转发客户端
 *
 * @author <a href="mailto:lw111072@antfin.com">liangen</a>
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class IpTransmitClient {

    /**
     * slf4j Logger for this class
     */
    private final static Logger  LOGGER = LoggerFactory.getLogger(IpTransmitClient.class);

    /**
     * 转发客户端的服务引用配置
     */
    private final ConsumerConfig consumerConfig;
    /**
     * 端口
     */
    private final Integer        port;
    /**
     * 管理长连接
     */
    private volatile Cluster     cluster;
    /**
     * 调用器
     */
    private volatile Invoker     invoker;

    /**
     * 构造函数
     *
     * @param consumerConfig 转发客户端的服务引用
     * @param port           对方端口
     */
    public IpTransmitClient(ConsumerConfig consumerConfig, int port) {
        this.consumerConfig = consumerConfig;
        this.port = port;
    }

    /**
     * 初始化客户端
     */
    public void init() {
        if (this.invoker == null) {
            synchronized (this) {
                if (this.invoker == null) {
                    Object proxy = consumerConfig.refer();
                    this.invoker = ProxyFactory.getInvoker(proxy, consumerConfig.getProxy());
                    this.cluster = consumerConfig.getConsumerBootstrap().getCluster();

                    // 初始化已经订阅到的地址列表
                    IpTransmitHandler handler = IpTransmitLauncher.getHandler(consumerConfig.getAppName());
                    List<String> ips = handler != null ?
                        handler.getAvailableTransmitAddresses() : new ArrayList<String>();
                    ProviderGroup group = new ProviderGroup();
                    if (ips != null) {
                        for (String ip : ips) {
                            group.add(buildProviderInfo(ip));
                        }
                    }
                    cluster.updateProviders(group);

                    if (LOGGER.isInfoEnabled(consumerConfig.getAppName())) {
                        LOGGER.infoWithApp(consumerConfig.getAppName(), "Init transmit client of {}:{}:{}",
                            consumerConfig.getInterfaceId(), consumerConfig.getProtocol(), port);
                    }
                }
            }
        }
    }

    private ProviderInfo buildProviderInfo(String ip) {
        ProviderInfo providerInfo = new ProviderInfo(ip, port)
            .setStaticAttr(ProviderInfoAttrs.ATTR_APP_NAME, consumerConfig.getAppName())
            .setStaticAttr(ProviderInfoAttrs.ATTR_SOURCE, "transmit");
        return providerInfo;
    }

    /**
     * 调用
     *
     * @param request 请求
     * @return 响应
     */
    public SofaResponse invoke(SofaRequest request) {
        return invoker.invoke(request);
    }

    /**
     * 往转发客户端里删除地址
     *
     * @param ip 远程地址
     */
    public void removeConnectionByIp(String ip) {
        ProviderGroup group = new ProviderGroup();
        group.add(buildProviderInfo(ip));
        cluster.removeProvider(group);
    }

    /**
     * 往转发客户端里添加地址
     *
     * @param ip 远程地址
     */
    public void addConnectionByIp(String ip) {
        ProviderGroup group = new ProviderGroup();
        group.add(buildProviderInfo(ip));
        cluster.addProvider(group);
    }

    /**
     * 销毁转发客户端
     */
    public void destroy() {
        consumerConfig.unRefer();
    }
}