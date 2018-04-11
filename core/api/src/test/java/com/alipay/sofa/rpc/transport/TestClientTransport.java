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
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.message.ResponseFuture;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
@Extension("test")
public class TestClientTransport extends ClientTransport {

    AbstractChannel       channel;

    private AtomicInteger currentRequest = new AtomicInteger(0);

    public void setRequest(int request) {
        currentRequest.set(request);
    }

    /**
     * 客户端配置
     *
     * @param transportConfig 客户端配置
     */
    protected TestClientTransport(ClientTransportConfig transportConfig) {
        super(transportConfig);
        channel = new TestChannel(InetSocketAddress.createUnresolved(transportConfig.getProviderInfo().getHost(),
            transportConfig.getProviderInfo().getPort()),
            InetSocketAddress.createUnresolved(transportConfig.getProviderInfo().getHost(),
                new Random().nextInt(65535)));
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void setChannel(AbstractChannel channel) {
        this.channel = channel;
    }

    @Override
    public AbstractChannel getChannel() {
        return channel;
    }

    @Override
    public int currentRequests() {
        return currentRequest.get();
    }

    @Override
    public ResponseFuture asyncSend(SofaRequest message, int timeout) throws SofaRpcException {
        return null;
    }

    @Override
    public SofaResponse syncSend(SofaRequest message, int timeout) throws SofaRpcException {
        return null;
    }

    @Override
    public void oneWaySend(SofaRequest message, int timeout) throws SofaRpcException {

    }

    @Override
    public void receiveRpcResponse(SofaResponse response) {

    }

    @Override
    public void handleRpcRequest(SofaRequest request) {

    }

    @Override
    public InetSocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public InetSocketAddress localAddress() {
        return channel.localAddress();
    }
}
