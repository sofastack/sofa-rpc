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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.alipay.sofa.rpc.common.RpcConfigs.getBooleanValue;
import static com.alipay.sofa.rpc.common.RpcConfigs.getIntValue;
import static com.alipay.sofa.rpc.common.RpcConfigs.getStringValue;
import static com.alipay.sofa.rpc.common.RpcOptions.DEFAULT_REGISTRY;
import static com.alipay.sofa.rpc.common.RpcOptions.REGISTRY_BATCH;
import static com.alipay.sofa.rpc.common.RpcOptions.REGISTRY_BATCH_SIZE;
import static com.alipay.sofa.rpc.common.RpcOptions.REGISTRY_CONNECT_TIMEOUT;
import static com.alipay.sofa.rpc.common.RpcOptions.REGISTRY_HEARTBEAT_PERIOD;
import static com.alipay.sofa.rpc.common.RpcOptions.REGISTRY_INDEX_ADDRESS;
import static com.alipay.sofa.rpc.common.RpcOptions.REGISTRY_INVOKE_TIMEOUT;
import static com.alipay.sofa.rpc.common.RpcOptions.REGISTRY_RECONNECT_PERIOD;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVICE_REGISTER;
import static com.alipay.sofa.rpc.common.RpcOptions.SERVICE_SUBSCRIBE;

/**
 * 注册中心配置
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
public class RegistryConfig extends AbstractIdConfig implements Serializable {
    /**
     * The constant serialVersionUID.
     */
    private static final long     serialVersionUID = -2921019924557602234L;

    /**
     * 协议
     */
    private String                protocol         = getStringValue(DEFAULT_REGISTRY);

    /**
     * 指定注册中心的地址, 和index必须填一个，address优先
     */
    private String                address;

    /**
     * 指定注册中心寻址服务的地址, 和address必须填一个
     */
    private String                index            = getStringValue(REGISTRY_INDEX_ADDRESS);

    /**
     * 是否注册，如果是false只订阅不注册
     */
    private boolean               register         = getBooleanValue(SERVICE_REGISTER);

    /**
     * 是否订阅服务
     */
    private boolean               subscribe        = getBooleanValue(SERVICE_SUBSCRIBE);

    /**
     * 调用注册中心超时时间
     */
    private int                   timeout          = getIntValue(REGISTRY_INVOKE_TIMEOUT);

    /**
     * 连接注册中心超时时间
     */
    private int                   connectTimeout   = getIntValue(REGISTRY_CONNECT_TIMEOUT);

    /**
     * 保存到本地文件的位置，默认$HOME下
     */
    private String                file;

    /**
     * 是否批量操作
     */
    private boolean               batch            = getBooleanValue(REGISTRY_BATCH);

    /**
     * 定时批量检查时的条目数
     */
    private int                   batchSize        = getIntValue(REGISTRY_BATCH_SIZE);

    /**
     * Consumer给Provider发心跳的间隔
     */
    protected int                 heartbeatPeriod  = getIntValue(REGISTRY_HEARTBEAT_PERIOD);

    /**
     * Consumer给Provider重连的间隔
     */
    protected int                 reconnectPeriod  = getIntValue(REGISTRY_RECONNECT_PERIOD);

    /**
     * The Parameters. 自定义参数
     */
    protected Map<String, String> parameters;

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
    public RegistryConfig setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * Gets address.
     *
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets address.
     *
     * @param address the address
     * @return the address
     */
    public RegistryConfig setAddress(String address) {
        this.address = address;
        return this;
    }

    /**
     * Gets index.
     *
     * @return the index
     */
    public String getIndex() {
        return index;
    }

    /**
     * Sets index.
     *
     * @param index the index
     * @return the index
     */
    public RegistryConfig setIndex(String index) {
        this.index = index;
        return this;
    }

    /**
     * Is register boolean.
     *
     * @return the boolean
     */
    public boolean isRegister() {
        return register;
    }

    /**
     * Sets register.
     *
     * @param register the register
     * @return the register
     */
    public RegistryConfig setRegister(boolean register) {
        this.register = register;
        return this;
    }

    /**
     * Is subscribe boolean.
     *
     * @return the boolean
     */
    public boolean isSubscribe() {
        return subscribe;
    }

    /**
     * Sets subscribe.
     *
     * @param subscribe the subscribe
     * @return the subscribe
     */
    public RegistryConfig setSubscribe(boolean subscribe) {
        this.subscribe = subscribe;
        return this;
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     * @return the timeout
     */
    public RegistryConfig setTimeout(int timeout) {
        this.timeout = timeout;
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
    public RegistryConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Gets file.
     *
     * @return the file
     */
    public String getFile() {
        return file;
    }

    /**
     * Sets file.
     *
     * @param file the file
     * @return the file
     */
    public RegistryConfig setFile(String file) {
        this.file = file;
        return this;
    }

    /**
     * Is batch boolean.
     *
     * @return the boolean
     */
    public boolean isBatch() {
        return batch;
    }

    /**
     * Sets batch.
     *
     * @param batch the batch
     * @return the batch
     */
    public RegistryConfig setBatch(boolean batch) {
        this.batch = batch;
        return this;
    }

    /**
     * Gets batch check.
     *
     * @return the batch check
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Sets batch check.
     *
     * @param batchSize the batch check
     * @return the batch check
     */
    public RegistryConfig setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Gets heartbeatPeriod.
     *
     * @return the heartbeatPeriod
     */
    public int getHeartbeatPeriod() {
        return heartbeatPeriod;
    }

    /**
     * Sets heartbeatPeriod.
     *
     * @param heartbeatPeriod the heartbeatPeriod
     * @return the heartbeatPeriod
     */
    public RegistryConfig setHeartbeatPeriod(int heartbeatPeriod) {
        this.heartbeatPeriod = heartbeatPeriod;
        return this;
    }

    /**
     * Gets reconnectPeriod.
     *
     * @return the reconnectPeriod
     */
    public int getReconnectPeriod() {
        return reconnectPeriod;
    }

    /**
     * Sets reconnectPeriod.
     *
     * @param reconnectPeriod the reconnectPeriod
     * @return the reconnectPeriod
     */
    public RegistryConfig setReconnectPeriod(int reconnectPeriod) {
        this.reconnectPeriod = reconnectPeriod;
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
     * @return the RegistryConfig
     */
    public RegistryConfig setParameters(Map<String, String> parameters) {
        if (this.parameters == null) {
            this.parameters = new ConcurrentHashMap<String, String>();
            this.parameters.putAll(parameters);
        }
        return this;
    }

    /**
     * Sets parameter.
     *
     * @param key   the key
     * @param value the value
     * @return the RegistryConfig
     */
    public RegistryConfig setParameter(String key, String value) {
        if (parameters == null) {
            parameters = new ConcurrentHashMap<String, String>();
        }
        parameters.put(key, value);
        return this;
    }

    /**
     * Gets parameter.
     *
     * @param key the key
     * @return the value
     */
    public String getParameter(String key) {
        return parameters == null ? null : parameters.get(key);
    }

    @Override
    public String toString() {
        return "RegistryConfig{" +
            "protocol='" + protocol + '\'' +
            ", address='" + address + '\'' +
            ", index='" + index + '\'' +
            ", register=" + register +
            ", subscribe=" + subscribe +
            ", timeout=" + timeout +
            ", connectTimeout=" + connectTimeout +
            ", file='" + file + '\'' +
            ", batch=" + batch +
            ", batchSize=" + batchSize +
            ", heartbeatPeriod=" + heartbeatPeriod +
            ", reconnectPeriod=" + reconnectPeriod +
            ", parameters=" + parameters +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RegistryConfig)) {
            return false;
        }

        RegistryConfig that = (RegistryConfig) o;

        if (register != that.register) {
            return false;
        }
        if (subscribe != that.subscribe) {
            return false;
        }
        if (timeout != that.timeout) {
            return false;
        }
        if (connectTimeout != that.connectTimeout) {
            return false;
        }
        if (batch != that.batch) {
            return false;
        }
        if (batchSize != that.batchSize) {
            return false;
        }
        if (heartbeatPeriod != that.heartbeatPeriod) {
            return false;
        }
        if (reconnectPeriod != that.reconnectPeriod) {
            return false;
        }
        if (!protocol.equals(that.protocol)) {
            return false;
        }
        if (address != null ? !address.equals(that.address) : that.address != null) {
            return false;
        }
        if (index != null ? !index.equals(that.index) : that.index != null) {
            return false;
        }
        if (file != null ? !file.equals(that.file) : that.file != null) {
            return false;
        }
        return parameters != null ? parameters.equals(that.parameters) : that.parameters == null;

    }

    @Override
    public int hashCode() {
        int result = protocol.hashCode();
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (index != null ? index.hashCode() : 0);
        result = 31 * result + (register ? 1 : 0);
        result = 31 * result + (subscribe ? 1 : 0);
        result = 31 * result + timeout;
        result = 31 * result + connectTimeout;
        result = 31 * result + (file != null ? file.hashCode() : 0);
        result = 31 * result + (batch ? 1 : 0);
        result = 31 * result + batchSize;
        result = 31 * result + heartbeatPeriod;
        result = 31 * result + reconnectPeriod;
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }
}