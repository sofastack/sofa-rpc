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

import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extensible;
import com.alipay.sofa.rpc.message.ResponseFuture;

import java.net.InetSocketAddress;

/**
 * ClientTransport
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(singleton = false)
public abstract class ClientTransport {

    /**
     * 客户端配置
     */
    protected ClientTransportConfig transportConfig;

    /**
     * 客户端配置
     *
     * @param transportConfig 客户端配置
     */
    protected ClientTransport(ClientTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
    }

    /**
     * 返回配置
     *
     * @return config
     */
    public ClientTransportConfig getConfig() {
        return transportConfig;
    }

    /**
     * 建立长连接
     */
    public abstract void connect();

    /**
     * 断开连接
     */
    public abstract void disconnect();

    /**
     * 销毁（最好是通过工厂模式销毁，这样可以清理缓存）
     */
    public abstract void destroy();

    /**
     * 是否可用（有可用的长连接）
     *
     * @return the boolean
     */
    public abstract boolean isAvailable();

    /**
     * 设置长连接
     *
     * @param channel the channel
     */
    public abstract void setChannel(AbstractChannel channel);

    /**
     * 得到长连接
     *
     * @return channel
     */
    public abstract AbstractChannel getChannel();

    /**
     * 当前请求数
     *
     * @return 当前请求数 int
     */
    public abstract int currentRequests();

    /**
     * 异步调用
     *
     * @param message 消息
     * @param timeout 超时时间
     * @return 异步Future response future
     * @throws SofaRpcException SofaRpcException
     */
    public abstract ResponseFuture asyncSend(SofaRequest message, int timeout) throws SofaRpcException;

    /**
     * 同步调用
     *
     * @param message 消息
     * @param timeout 超时时间
     * @return SofaResponse base message
     * @throws SofaRpcException SofaRpcException
     */
    public abstract SofaResponse syncSend(SofaRequest message, int timeout) throws SofaRpcException;

    /**
     * 单向调用
     *
     * @param message 消息
     * @param timeout 超时时间
     * @throws SofaRpcException SofaRpcException
     */
    public abstract void oneWaySend(SofaRequest message, int timeout) throws SofaRpcException;

    /**
     * 客户端收到异步响应
     *
     * @param response the response
     */
    public abstract void receiveRpcResponse(SofaResponse response);

    /**
     * 客户端收到服务端的请求，可能是服务端Callback
     *
     * @param request the request
     */
    public abstract void handleRpcRequest(SofaRequest request);

    /**
     * 远程地址
     *
     * @return 远程地址，一般是服务端地址
     */
    public abstract InetSocketAddress remoteAddress();

    /**
     * 本地地址
     *
     * @return 本地地址，一般是客户端地址
     */
    public abstract InetSocketAddress localAddress();

}
