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
package com.alipay.sofa.rpc.transport.triple.quic;

import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler;

/**
 * QUIC client channel initializer for HTTP/3.
 * Sets up the QUIC connection and HTTP/3 protocol handlers for client.
 *
 * @author <a href="mailto:evenljj@antfin.com">Even Li</a>
 */
public class QuicClientChannelInitializer extends io.netty.channel.ChannelInitializer<QuicChannel> {

    private final Http3ClientHandler responseHandler;

    /**
     * Create QUIC client channel initializer.
     *
     * @param responseHandler HTTP/3 response handler
     */
    public QuicClientChannelInitializer(Http3ClientHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    protected void initChannel(QuicChannel ch) {
        ChannelPipeline p = ch.pipeline();

        // Add HTTP/3 client connection handler
        // For 0.0.20.Final, use the default constructor
        // The response handler will be added to each stream channel when creating streams
        Http3ClientConnectionHandler connectionHandler = new Http3ClientConnectionHandler();

        p.addLast("http3-client-connection", connectionHandler);
    }

    /**
     * Get the response handler.
     *
     * @return HTTP/3 response handler
     */
    public Http3ClientHandler getResponseHandler() {
        return responseHandler;
    }
}