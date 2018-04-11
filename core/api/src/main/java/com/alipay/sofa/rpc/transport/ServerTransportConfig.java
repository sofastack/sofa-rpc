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

import com.alipay.sofa.rpc.common.RpcOptions;
import com.alipay.sofa.rpc.listener.ChannelListener;
import com.alipay.sofa.rpc.server.ServerHandler;

import java.util.List;
import java.util.Map;

import static com.alipay.sofa.rpc.common.RpcConfigs.getBooleanValue;
import static com.alipay.sofa.rpc.common.RpcConfigs.getIntValue;
import static com.alipay.sofa.rpc.common.RpcConfigs.getStringValue;

/**
 * Config of server transport
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ServerTransportConfig {

    private String                host             = getStringValue(RpcOptions.SERVER_HOST);
    private int                   port             = getIntValue(RpcOptions.SERVER_PORT_START);

    private String                contextPath      = getStringValue(RpcOptions.SERVER_CONTEXT_PATH);
    private String                container        = getStringValue(RpcOptions.DEFAULT_TRANSPORT);
    private int                   backlog          = getIntValue(RpcOptions.TRANSPORT_SERVER_BACKLOG);
    private String                protocolType     = getStringValue(RpcOptions.DEFAULT_PROTOCOL);
    private boolean               reuseAddr        = getBooleanValue(RpcOptions.TRANSPORT_SERVER_REUSE_ADDR);
    private boolean               keepAlive        = getBooleanValue(RpcOptions.TRANSPORT_SERVER_KEEPALIVE);
    private boolean               tcpNoDelay       = getBooleanValue(RpcOptions.TRANSPORT_SERVER_TCPNODELAY);
    private int                   bizMaxThreads    = getIntValue(RpcOptions.SERVER_POOL_MAX);                //default business pool set to 200
    private String                bizPoolType      = getStringValue(RpcOptions.SERVER_POOL_TYPE);

    private boolean               useEpoll         = getBooleanValue(RpcOptions.TRANSPORT_USE_EPOLL);
    private String                bizPoolQueueType = getStringValue(RpcOptions.SERVER_POOL_QUEUE_TYPE);      // 队列类型
    private int                   bizPoolQueues    = getIntValue(RpcOptions.SERVER_POOL_QUEUE);              // 队列大小

    private int                   bossThreads      = getIntValue(RpcOptions.TRANSPORT_SERVER_BOSS_THREADS);  // boss线程,一个端口绑定到一个线程

    private int                   ioThreads        = getIntValue(RpcOptions.TRANSPORT_SERVER_IO_THREADS);    // worker线程==IO线程，一个长连接绑定到一个线程

    private int                   maxConnection    = getIntValue(RpcOptions.SERVER_ACCEPTS);                 // 最大连接数 default set to 100
    private int                   payload          = getIntValue(RpcOptions.TRANSPORT_PAYLOAD_MAX);          // 最大数据包 default set to 8M
    private int                   buffer           = getIntValue(RpcOptions.TRANSPORT_BUFFER_SIZE);          // 缓冲器大小
    private boolean               telnet           = getBooleanValue(RpcOptions.SERVER_TELNET);              // 是否允许telnet
    private boolean               daemon           = getBooleanValue(RpcOptions.SERVER_DAEMON);              // 是否守护线程，true随主线程退出而退出，false需要主动退出

    private int                   bufferMin        = getIntValue(RpcOptions.TRANSPORT_BUFFER_MIN);
    private int                   bufferMax        = getIntValue(RpcOptions.TRANSPORT_BUFFER_MAX);

    private Map<String, String>   parameters;                                                                //其他一些参数配置

    private List<ChannelListener> channelListeners;
    private ServerHandler         serverHandler;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public boolean isReuseAddr() {
        return reuseAddr;
    }

    public void setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getBizMaxThreads() {
        return bizMaxThreads;
    }

    public void setBizMaxThreads(int bizMaxThreads) {
        this.bizMaxThreads = bizMaxThreads;
    }

    public String getBizPoolType() {
        return bizPoolType;
    }

    public void setBizPoolType(String bizPoolType) {
        this.bizPoolType = bizPoolType;
    }

    public boolean isUseEpoll() {
        return useEpoll;
    }

    public void setUseEpoll(boolean useEpoll) {
        this.useEpoll = useEpoll;
    }

    public String getBizPoolQueueType() {
        return bizPoolQueueType;
    }

    public void setBizPoolQueueType(String bizPoolQueueType) {
        this.bizPoolQueueType = bizPoolQueueType;
    }

    public int getBizPoolQueues() {
        return bizPoolQueues;
    }

    public void setBizPoolQueues(int bizPoolQueues) {
        this.bizPoolQueues = bizPoolQueues;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
    }

    public int getMaxConnection() {
        return maxConnection;
    }

    public void setMaxConnection(int maxConnection) {
        this.maxConnection = maxConnection;
    }

    public int getPayload() {
        return payload;
    }

    public void setPayload(int payload) {
        this.payload = payload;
    }

    public int getBuffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public boolean isTelnet() {
        return telnet;
    }

    public void setTelnet(boolean telnet) {
        this.telnet = telnet;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public int getBufferMin() {
        return bufferMin;
    }

    public ServerTransportConfig setBufferMin(int bufferMin) {
        this.bufferMin = bufferMin;
        return this;
    }

    public int getBufferMax() {
        return bufferMax;
    }

    public ServerTransportConfig setBufferMax(int bufferMax) {
        this.bufferMax = bufferMax;
        return this;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public List<ChannelListener> getChannelListeners() {
        return channelListeners;
    }

    public void setChannelListeners(List<ChannelListener> channelListeners) {
        this.channelListeners = channelListeners;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }
}
