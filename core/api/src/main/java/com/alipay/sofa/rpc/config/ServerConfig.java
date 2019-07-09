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
package com.alipay.sofa.rpc.config;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.listener.ChannelListener;
import com.alipay.sofa.rpc.server.Server;
import com.alipay.sofa.rpc.server.ServerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.alipay.sofa.rpc.common.RpcConfigs.getBooleanValue;
import static com.alipay.sofa.rpc.common.RpcConfigs.getIntValue;
import static com.alipay.sofa.rpc.common.RpcConfigs.getStringValue;
import static com.alipay.sofa.rpc.common.RpcOptions.DEFAULT_PROTOCOL;
import static com.alipay.sofa.rpc.common.RpcOptions.DEFAULT_SERIALIZATION;
import static com.alipay.sofa.rpc.common.RpcOptions.DEFAULT_TRANSPORT;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_ACCEPTS;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_CONTEXT_PATH;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_DAEMON;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_EPOLL;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_HOST;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_IOTHREADS;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_POOL_ALIVETIME;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_POOL_CORE;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_POOL_MAX;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_POOL_PRE_START;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_POOL_QUEUE;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_POOL_QUEUE_TYPE;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_POOL_TYPE;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_PORT_START;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_STOP_TIMEOUT;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVER_TELNET;
import static com.alipay.sofa.rpc.common.RpcOptions.SEVER_ADAPTIVE_PORT;
import static com.alipay.sofa.rpc.common.RpcOptions.SEVER_AUTO_START;
import static com.alipay.sofa.rpc.common.RpcOptions.TRANSPORT_PAYLOAD_MAX;
import static com.alipay.sofa.rpc.common.RpcOptions.TRANSPORT_SERVER_KEEPALIVE;

/**
 * 服务端配置
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class ServerConfig extends AbstractIdConfig implements Serializable {
    /**
     * The constant serialVersionUID.
     */
    private static final long                 serialVersionUID = -574374673831680403L;

    /*------------- 参数配置项开始-----------------*/
    /**
     * 配置名称
     */
    protected String                          protocol         = getStringValue(DEFAULT_PROTOCOL);

    /**
     * 实际监听IP，与网卡对应
     */
    protected String                          host             = getStringValue(SERVER_HOST);

    /**
     * 监听端口
     */
    protected int                             port             = getIntValue(SERVER_PORT_START);

    /**
     * 基本路径
     */
    protected String                          contextPath      = getStringValue(SERVER_CONTEXT_PATH);

    /**
     * io线程池大小
     */
    protected int                             ioThreads        = getIntValue(SERVER_IOTHREADS);

    /**
     * 线程池类型
     */
    protected String                          threadPoolType   = getStringValue(SERVER_POOL_TYPE);

    /**
     * 业务线程池大小
     */
    protected int                             coreThreads      = getIntValue(SERVER_POOL_CORE);

    /**
     * 业务线程池大小
     */
    protected int                             maxThreads       = getIntValue(SERVER_POOL_MAX);

    /**
     * 是否允许telnet，针对自定义协议
     */
    protected boolean                         telnet           = getBooleanValue(SERVER_TELNET);

    /**
     * 线程池类型，默认普通线程池
     */
    protected String                          queueType        = getStringValue(SERVER_POOL_QUEUE_TYPE);

    /**
     * 业务线程池队列大小
     */
    protected int                             queues           = getIntValue(SERVER_POOL_QUEUE);

    /**
     * 线程池回收时间
     */
    protected int                             aliveTime        = getIntValue(SERVER_POOL_ALIVETIME);

    /**
     * 线程池是否初始化核心线程
     */
    protected boolean                         preStartCore     = getBooleanValue(SERVER_POOL_PRE_START);

    /**
     * 服务端允许客户端建立的连接数
     */
    protected int                             accepts          = getIntValue(SERVER_ACCEPTS);

    /**
     * 最大数据包大小
     */
    @Deprecated
    protected int                             payload          = getIntValue(TRANSPORT_PAYLOAD_MAX);

    /**
     * 序列化方式
     */
    protected String                          serialization    = getStringValue(DEFAULT_SERIALIZATION);

    /**
     * 事件分发规则。
     */
    @Deprecated
    protected String                          dispatcher       = RpcConstants.DISPATCHER_MESSAGE;

    /**
     * The Parameters. 自定义参数
     */
    protected Map<String, String>             parameters;

    /**
     * 镜像ip，例如监听地址是1.2.3.4，告诉注册中心的确是3.4.5.6
     */
    protected String                          virtualHost;

    /**
     * 镜像端口
     */
    protected Integer                         virtualPort;

    /**
     * 连接事件监听器实例，连接或者断开时触发
     */
    protected transient List<ChannelListener> onConnect;

    /**
     * 是否启动epoll
     */
    protected boolean                         epoll            = getBooleanValue(SERVER_EPOLL);

    /**
     * 是否hold住端口，true的话随主线程退出而退出，false的话则要主动退出
     */
    protected boolean                         daemon           = getBooleanValue(SERVER_DAEMON);

    /**
     * The Adaptive port.
     */
    protected boolean                         adaptivePort     = getBooleanValue(SEVER_ADAPTIVE_PORT);

    /**
     * 传输层
     */
    protected String                          transport        = getStringValue(DEFAULT_TRANSPORT);

    /**
     * 是否自动启动
     */
    protected boolean                         autoStart        = getBooleanValue(SEVER_AUTO_START);

    /**
     * 服务端关闭超时时间
     */
    protected int                             stopTimeout      = getIntValue(SERVER_STOP_TIMEOUT);

    /**
     * 是否维持长连接
     */
    protected boolean                         keepAlive        = getBooleanValue(TRANSPORT_SERVER_KEEPALIVE);

    /*------------- 参数配置项结束-----------------*/
    /**
     * 服务端对象
     */
    private transient volatile Server         server;

    /**
     * 绑定的地址。是某个网卡，还是全部地址
     */
    private transient String                  boundHost;

    /**
     * 启动服务
     *
     * @return the server
     */
    public synchronized Server buildIfAbsent() {
        if (server != null) {
            return server;
        }
        // 提前检查协议+序列化方式
        // ConfigValueHelper.check(ProtocolType.valueOf(getProtocol()),
        //                SerializationType.valueOf(getSerialization()));

        server = ServerFactory.getServer(this);
        return server;
    }

    /**
     * 关闭服务
     */
    public synchronized void destroy() {
        ServerFactory.destroyServer(this);
    }

    /**
     * Gets protocol.
     *
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets protocol.
     *
     * @param protocol the protocol
     * @return the protocol
     */
    public ServerConfig setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * Gets host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets host.
     *
     * @param host the host
     * @return the host
     */
    public ServerConfig setHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets port.
     *
     * @param port the port
     * @return the port
     */
    public ServerConfig setPort(int port) {
        if (!NetUtils.isRandomPort(port) && NetUtils.isInvalidPort(port)) {
            throw ExceptionUtils.buildRuntime("server.port", port + "",
                "port must between -1 and 65535 (-1 means random port)");
        }
        this.port = port;
        return this;
    }

    /**
     * Gets context path.
     *
     * @return the context path
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Sets context path.
     *
     * @param contextPath the context path
     * @return the context path
     */
    public ServerConfig setContextPath(String contextPath) {
        if (!contextPath.endsWith(StringUtils.CONTEXT_SEP)) {
            contextPath += StringUtils.CONTEXT_SEP;
        }
        this.contextPath = contextPath;
        return this;
    }

    /**
     * Gets ioThreads.
     *
     * @return the ioThreads
     */
    public int getIoThreads() {
        return ioThreads;
    }

    /**
     * Sets ioThreads.
     *
     * @param ioThreads the ioThreads
     * @return the ioThreads
     */
    public ServerConfig setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
        return this;
    }

    /**
     * Gets threadPoolType.
     *
     * @return the threadPoolType
     */
    public String getThreadPoolType() {
        return threadPoolType;
    }

    /**
     * Sets threadPoolType.
     *
     * @param threadPoolType the threadPoolType
     * @return the threadPoolType
     */
    public ServerConfig setThreadPoolType(String threadPoolType) {
        this.threadPoolType = threadPoolType;
        return this;
    }

    /**
     * Gets core threads.
     *
     * @return the core threads
     */
    public int getCoreThreads() {
        return coreThreads;
    }

    /**
     * Sets core threads.
     *
     * @param coreThreads the core threads
     * @return the core threads
     */
    public ServerConfig setCoreThreads(int coreThreads) {
        this.coreThreads = coreThreads;
        return this;
    }

    /**
     * Gets max threads.
     *
     * @return the max threads
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * Sets max threads.
     *
     * @param maxThreads the max threads
     * @return the max threads
     */
    public ServerConfig setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    /**
     * Is telnet boolean.
     *
     * @return the boolean
     */
    public boolean isTelnet() {
        return telnet;
    }

    /**
     * Sets telnet.
     *
     * @param telnet the telnet
     * @return the telnet
     */
    public ServerConfig setTelnet(boolean telnet) {
        this.telnet = telnet;
        return this;
    }

    /**
     * Gets queue type.
     *
     * @return the queue type
     */
    public String getQueueType() {
        return queueType;
    }

    /**
     * Sets queue type.
     *
     * @param queueType the queue type
     * @return the queue type
     */
    public ServerConfig setQueueType(String queueType) {
        this.queueType = queueType;
        return this;
    }

    /**
     * Gets queues.
     *
     * @return the queues
     */
    public int getQueues() {
        return queues;
    }

    /**
     * Sets queues.
     *
     * @param queues the queues
     * @return the queues
     */
    public ServerConfig setQueues(int queues) {
        this.queues = queues;
        return this;
    }

    /**
     * Gets alive time.
     *
     * @return the alive time
     */
    public int getAliveTime() {
        return aliveTime;
    }

    /**
     * Sets alive time.
     *
     * @param aliveTime the alive time
     * @return the alive time
     */
    public ServerConfig setAliveTime(int aliveTime) {
        this.aliveTime = aliveTime;
        return this;
    }

    /**
     * Is pre start core boolean.
     *
     * @return the boolean
     */
    public boolean isPreStartCore() {
        return preStartCore;
    }

    /**
     * Sets pre start core.
     *
     * @param preStartCore the pre start core
     * @return the pre start core
     */
    public ServerConfig setPreStartCore(boolean preStartCore) {
        this.preStartCore = preStartCore;
        return this;
    }

    /**
     * Gets accepts.
     *
     * @return the accepts
     */
    public int getAccepts() {
        return accepts;
    }

    /**
     * Sets accepts.
     *
     * @param accepts the accepts
     * @return the accepts
     */
    public ServerConfig setAccepts(int accepts) {
        ConfigValueHelper.checkPositiveInteger("server.accept", accepts);
        this.accepts = accepts;
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
    public ServerConfig setPayload(int payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Gets serialization.
     *
     * @return the serialization
     */
    public String getSerialization() {
        return serialization;
    }

    /**
     * Sets serialization.
     *
     * @param serialization the serialization
     * @return the serialization
     */
    public ServerConfig setSerialization(String serialization) {
        this.serialization = serialization;
        return this;
    }

    /**
     * Gets dispatcher.
     *
     * @return the dispatcher
     */
    public String getDispatcher() {
        return dispatcher;
    }

    /**
     * Sets dispatcher.
     *
     * @param dispatcher the dispatcher
     * @return the dispatcher
     */
    public ServerConfig setDispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    /**
     * Gets parameters.
     *
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Sets parameters.
     *
     * @param parameters the parameters
     * @return the parameters
     */
    public ServerConfig setParameters(Map<String, String> parameters) {
        if (this.parameters == null) {
            this.parameters = new ConcurrentHashMap<String, String>();
            this.parameters.putAll(parameters);
        }
        return this;
    }

    /**
     * Gets virtualHost.
     *
     * @return the virtualHost
     */
    public String getVirtualHost() {
        return virtualHost;
    }

    /**
     * Sets virtualHost.
     *
     * @param virtualHost the virtualHost
     * @return the virtualHost
     */
    public ServerConfig setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
        return this;
    }

    /**
     * Gets virtual port.
     *
     * @return the virtual port
     */
    public Integer getVirtualPort() {
        return virtualPort;
    }

    /**
     * Sets virtual port.
     *
     * @param virtualPort the virtual port
     * @return the virtual port
     */
    public ServerConfig setVirtualPort(Integer virtualPort) {
        this.virtualPort = virtualPort;
        return this;
    }

    /**
     * Gets onConnect.
     *
     * @return the onConnect
     */
    public List<ChannelListener> getOnConnect() {
        return onConnect;
    }

    /**
     * Sets onConnect.
     *
     * @param onConnect the onConnect
     * @return the onConnect
     */
    public ServerConfig setOnConnect(List<ChannelListener> onConnect) {
        this.onConnect = onConnect;
        return this;
    }

    /**
     * Is epoll boolean.
     *
     * @return the boolean
     */
    public boolean isEpoll() {
        return epoll;
    }

    /**
     * Sets epoll.
     *
     * @param epoll the epoll
     * @return the epoll
     */
    public ServerConfig setEpoll(boolean epoll) {
        this.epoll = epoll;
        return this;
    }

    /**
     * Is daemon boolean.
     *
     * @return the boolean
     */
    public boolean isDaemon() {
        return daemon;
    }

    /**
     * Sets daemon.
     *
     * @param daemon the daemon
     * @return the daemon
     */
    public ServerConfig setDaemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    /**
     * Gets transport.
     *
     * @return the transport
     */
    public String getTransport() {
        return transport;
    }

    /**
     * Sets transport.
     *
     * @param transport the transport
     * @return the transport
     */
    public ServerConfig setTransport(String transport) {
        this.transport = transport;
        return this;
    }

    /**
     * Is adaptive port boolean.
     *
     * @return the boolean
     */
    public boolean isAdaptivePort() {
        return adaptivePort;
    }

    /**
     * Sets adaptive port.
     *
     * @param adaptivePort the adaptive port
     * @return the adaptive port
     */
    public ServerConfig setAdaptivePort(boolean adaptivePort) {
        this.adaptivePort = adaptivePort;
        return this;
    }

    /**
     * Is auto start boolean.
     *
     * @return the boolean
     */
    public boolean isAutoStart() {
        return autoStart;
    }

    /**
     * Sets auto start.
     *
     * @param autoStart the auto start
     * @return the auto start
     */
    public ServerConfig setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
        return this;
    }

    /**
     * Gets stop timeout.
     *
     * @return the stop timeout
     */
    public int getStopTimeout() {
        return stopTimeout;
    }

    /**
     * Sets stop timeout.
     *
     * @param stopTimeout the stop timeout
     * @return the stop timeout
     */
    public ServerConfig setStopTimeout(int stopTimeout) {
        this.stopTimeout = stopTimeout;
        return this;
    }

    /**
     * Gets server.
     *
     * @return the server
     */
    public Server getServer() {
        return server;
    }

    /**
     * Set server
     * @param server
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Sets bound host.
     *
     * @param boundHost the bound host
     * @return the bound host
     */
    public ServerConfig setBoundHost(String boundHost) {
        this.boundHost = boundHost;
        return this;
    }

    /**
     * Gets bound host
     *
     * @return bound host
     */
    public String getBoundHost() {
        return boundHost;
    }

    /**
     * Get KeepAlive
     *
     * @return 是否长连接
     */
    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * set KeepAlive
     *
     * @param keepAlive 是否长连接
     * @return this
     */
    public ServerConfig setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        result = prime * result
            + ((protocol == null) ? 0 : protocol.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ServerConfig other = (ServerConfig) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (protocol == null) {
            if (other.protocol != null) {
                return false;
            }
        } else if (!protocol.equals(other.protocol)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ServerConfig [protocol=" + protocol + ", port=" + port + ", host=" + host + "]";
    }

}
