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
package com.alipay.sofa.rpc.transport.netty;

import com.alipay.sofa.rpc.common.utils.NetUtils;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import com.alipay.sofa.rpc.transport.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.net.InetSocketAddress;

/**
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 * @since 5.4.0
 */
public class NettyChannel extends AbstractChannel<ChannelHandlerContext, Channel> {
    /**
     * slf4j Logger for this class
     */
    private final static Logger   LOGGER = LoggerFactory.getLogger(NettyChannel.class);

    /**
     * 长连接上下文
     */
    private ChannelHandlerContext context;

    /**
     * 通道
     */
    private Channel               channel;

    public NettyChannel(Channel channel) {
        this.channel = channel;
    }

    public NettyChannel(ChannelHandlerContext context) {
        this.context = context;
        this.channel = context.channel();
    }

    @Override
    public ChannelHandlerContext channelContext() {
        return context;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public void writeAndFlush(final Object obj) {
        Future future = channel.writeAndFlush(obj);
        future.addListener(new FutureListener() {
            @Override
            public void operationComplete(Future future1) throws Exception {
                if (!future1.isSuccess()) {
                    Throwable throwable = future1.cause();
                    LOGGER.error("Failed to send to "
                        + NetUtils.channelToString(localAddress(), remoteAddress())
                        + " for msg : " + obj
                        + ", Cause by:", throwable);
                }
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return channel.isOpen() && channel.isActive();
    }
}
