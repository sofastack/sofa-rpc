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
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.listener.ChannelListener;

import java.util.List;
import java.util.Objects;

import static com.alipay.sofa.rpc.common.RpcConfigs.getBooleanValue;
import static com.alipay.sofa.rpc.common.RpcConfigs.getIntValue;
import static com.alipay.sofa.rpc.common.RpcConfigs.getStringValue;
import static com.alipay.sofa.rpc.common.RpcOptions.CONSUMER_CONNECTION_NUM;
import static com.alipay.sofa.rpc.common.RpcOptions.CONSUMER_CONNECT_TIMEOUT;
import static com.alipay.sofa.rpc.common.RpcOptions.CONSUMER_DISCONNECT_TIMEOUT;
import static com.alipay.sofa.rpc.common.RpcOptions.CONSUMER_INVOKE_TIMEOUT;
import static com.alipay.sofa.rpc.common.RpcOptions.DEFAULT_TRANSPORT;
import static com.alipay.sofa.rpc.common.RpcOptions.TRANSPORT_PAYLOAD_MAX;
import static com.alipay.sofa.rpc.common.RpcOptions.TRANSPORT_USE_EPOLL;

/**
 * 客户端传输层配置
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ClientTransportConfig {

    /**
     * 客户端的一些信息（请只读）
     */
    private ConsumerConfig        consumerConfig;
    /**
     * 对应的Provider信息（请只读）
     */
    private ProviderInfo          providerInfo;
    /**
     * 默认传输实现（一般和协议一致）
     */
    private String                container         = getStringValue(DEFAULT_TRANSPORT);
    /**
     * 默认连接超时时间
     */
    private int                   connectTimeout    = getIntValue(CONSUMER_CONNECT_TIMEOUT);
    /**
     * 默认断开连接超时时间
     */
    private int                   disconnectTimeout = getIntValue(CONSUMER_DISCONNECT_TIMEOUT);
    /**
     * 默认的调用超时时间（长连接调用时会被覆盖）
     */
    private int                   invokeTimeout     = getIntValue(CONSUMER_INVOKE_TIMEOUT);
    /**
     * 默认一个地址建立长连接的数量
     */
    private int                   connectionNum     = getIntValue(CONSUMER_CONNECTION_NUM);
    /**
     * 最大数据量
     */
    private int                   payload           = getIntValue(TRANSPORT_PAYLOAD_MAX);
    /**
     * 是否使用Epoll
     */
    private boolean               useEpoll          = getBooleanValue(TRANSPORT_USE_EPOLL);
    /**
     * 连接事件监听器
     */
    private List<ChannelListener> channelListeners;

    /**
     * Gets consumer config.
     *
     * @return the consumer config
     */
    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    /**
     * Sets consumer config.
     *
     * @param consumerConfig the consumer config
     * @return the consumer config
     */
    public ClientTransportConfig setConsumerConfig(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
        return this;
    }

    /**
     * Gets provider info.
     *
     * @return the provider info
     */
    public ProviderInfo getProviderInfo() {
        return providerInfo;
    }

    /**
     * Sets provider info.
     *
     * @param providerInfo the provider info
     * @return the provider info
     */
    public ClientTransportConfig setProviderInfo(ProviderInfo providerInfo) {
        this.providerInfo = providerInfo;
        return this;
    }

    /**
     * Gets container.
     *
     * @return the container
     */
    public String getContainer() {
        return container;
    }

    /**
     * Sets container.
     *
     * @param container the container
     * @return the container
     */
    public ClientTransportConfig setContainer(String container) {
        this.container = container;
        return this;
    }

    /**
     * Gets connect timeout.
     *
     * @return the connect timeout
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets connect timeout.
     *
     * @param connectTimeout the connect timeout
     * @return the connect timeout
     */
    public ClientTransportConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Gets disconnect timeout.
     *
     * @return the disconnect timeout
     */
    public int getDisconnectTimeout() {
        return disconnectTimeout;
    }

    /**
     * Sets disconnect timeout.
     *
     * @param disconnectTimeout the disconnect timeout
     * @return the disconnect timeout
     */
    public ClientTransportConfig setDisconnectTimeout(int disconnectTimeout) {
        this.disconnectTimeout = disconnectTimeout;
        return this;
    }

    /**
     * Gets invoke timeout.
     *
     * @return the invoke timeout
     */
    public int getInvokeTimeout() {
        return invokeTimeout;
    }

    /**
     * Sets invoke timeout.
     *
     * @param invokeTimeout the invoke timeout
     * @return the invoke timeout
     */
    public ClientTransportConfig setInvokeTimeout(int invokeTimeout) {
        this.invokeTimeout = invokeTimeout;
        return this;
    }

    /**
     * Gets connection num.
     *
     * @return the connection num
     */
    public int getConnectionNum() {
        return connectionNum;
    }

    /**
     * Sets connection num.
     *
     * @param connectionNum the connection num
     * @return the connection num
     */
    public ClientTransportConfig setConnectionNum(int connectionNum) {
        this.connectionNum = connectionNum;
        return this;
    }

    /**
     * Gets payload.
     *
     * @return the payload
     */
    public int getPayload() {
        return payload;
    }

    /**
     * Sets payload.
     *
     * @param payload the payload
     * @return the payload
     */
    public ClientTransportConfig setPayload(int payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Is use epoll boolean.
     *
     * @return the boolean
     */
    public boolean isUseEpoll() {
        return useEpoll;
    }

    /**
     * Sets use epoll.
     *
     * @param useEpoll the use epoll
     * @return the use epoll
     */
    public ClientTransportConfig setUseEpoll(boolean useEpoll) {
        this.useEpoll = useEpoll;
        return this;
    }

    /**
     * Gets channel listeners.
     *
     * @return the channel listeners
     */
    public List<ChannelListener> getChannelListeners() {
        return channelListeners;
    }

    /**
     * Sets channel listeners.
     *
     * @param channelListeners the channel listeners
     * @return the channel listeners
     */
    public ClientTransportConfig setChannelListeners(List<ChannelListener> channelListeners) {
        this.channelListeners = channelListeners;
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + "{" +
            "consumerConfig=" + consumerConfig +
            ", providerInfo=" + providerInfo +
            ", container='" + container + '\'' +
            ", connectTimeout=" + connectTimeout +
            ", disconnectTimeout=" + disconnectTimeout +
            ", invokeTimeout=" + invokeTimeout +
            ", connectionNum=" + connectionNum +
            ", payload=" + payload +
            ", useEpoll=" + useEpoll +
            ", channelListeners=" + channelListeners +
            '}';
    }
}
