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

import com.alipay.sofa.rpc.common.annotation.Unstable;
import com.alipay.sofa.rpc.common.struct.TwoWayMap;
import com.alipay.sofa.rpc.common.utils.StringUtils;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;

/**
 * <p>每个长连接都对应一个上下文，例如客户端本地存着服务端版本，服务端存着客户端的APP信息等</p>
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Unstable
public class ChannelContext {

    /**
     * 每个长连接独立的一个缓存，最多256条
     * TODO 是否分将缓存 全局静态区和动态区
     */
    protected volatile TwoWayMap<Short, String> headerCache;
    /**
     * 对方版本
     */
    protected Integer                           dstVersion;
    /**
     * 客户端应用Id
     */
    protected String                            clientAppId;
    /**
     * 客户端应用名称
     */
    protected String                            clientAppName;
    /**
     * 客户端应用实例id
     */
    protected String                            clientInstanceId;
    /**
     * 长连接的协议
     */
    private String                              protocol;

    /**
     * Put header cache
     *
     * @param key   the key
     * @param value the value
     */
    public void putHeadCache(Short key, String value) {
        if (headerCache == null) {
            synchronized (this) {
                if (headerCache == null) {
                    headerCache = new TwoWayMap<Short, String>();
                }
            }
        }
        if (headerCache != null && !headerCache.containsKey(key)) {
            headerCache.put(key, value);
        }
    }

    /**
     * Invalidate head cache.
     *
     * @param key   the key
     * @param value the value
     */
    public void invalidateHeadCache(Byte key, String value) {
        if (headerCache != null && headerCache.containsKey(key)) {
            String old = headerCache.get(key);
            if (!old.equals(value)) {
                throw new SofaRpcRuntimeException("Value of old is not match current");
            }
            headerCache.remove(key);
        }
    }

    /**
     * Gets header.
     *
     * @param key the key
     * @return the header
     */
    public String getHeader(Short key) {
        if (key != null && headerCache != null) {
            return headerCache.get(key);
        }
        return null;
    }

    /**
     * Gets header.
     *
     * @param value the value
     * @return the header
     */
    public Short getHeaderKey(String value) {
        if (StringUtils.isNotEmpty(value) && headerCache != null) {
            return headerCache.getKey(value);
        }
        return null;
    }

    /**
     * Gets dst version.
     *
     * @return the dst version
     */
    public Integer getDstVersion() {
        return dstVersion;
    }

    /**
     * Sets dst version.
     *
     * @param dstVersion the dst version
     * @return the dst version
     */
    public ChannelContext setDstVersion(Integer dstVersion) {
        this.dstVersion = dstVersion;
        return this;
    }

    /**
     * Gets client app id.
     *
     * @return the client app id
     */
    public String getClientAppId() {
        return clientAppId;
    }

    /**
     * Sets client app id.
     *
     * @param clientAppId the client app id
     * @return the client app id
     */
    public ChannelContext setClientAppId(String clientAppId) {
        this.clientAppId = clientAppId;
        return this;
    }

    /**
     * Gets client app name.
     *
     * @return the client app name
     */
    public String getClientAppName() {
        return clientAppName;
    }

    /**
     * Sets client app name.
     *
     * @param clientAppName the client app name
     * @return the client app name
     */
    public ChannelContext setClientAppName(String clientAppName) {
        this.clientAppName = clientAppName;
        return this;
    }

    /**
     * Gets client instance id.
     *
     * @return the client instance id
     */
    public String getClientInstanceId() {
        return clientInstanceId;
    }

    /**
     * Sets client instance id.
     *
     * @param clientInstanceId the client instance id
     * @return the client instance id
     */
    public ChannelContext setClientInstanceId(String clientInstanceId) {
        this.clientInstanceId = clientInstanceId;
        return this;
    }

    /**
     * Sets protocol.
     *
     * @param protocol the protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
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
     * 得到可用的引用索引
     *
     * @param consumerToProvider 客户端发起true、还是服务端发起false
     * @return 可用的引用索引
     */
    public Short getAvailableRefIndex(boolean consumerToProvider) {
        if (headerCache == null) {
            synchronized (this) {
                if (headerCache == null) {
                    headerCache = new TwoWayMap<Short, String>();
                }
            }
        }
        if (consumerToProvider) { // from consumer to provider : 0~32766
            for (short i = 0; i < Short.MAX_VALUE; i++) {
                if (!headerCache.containsKey(i)) {
                    return i;
                }
            }
        } else { // from provider to consumer : -1~-32767
            for (short i = -1; i > Short.MIN_VALUE; i--) {
                if (!headerCache.containsKey(i)) {
                    return i;
                }
            }
        }
        return null;
    }
}
